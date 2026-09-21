#!/usr/bin/env node

import { choice, noul, TypeSafeClient, type Questions } from "@typesafe-ai/sdk";
import { mkdirSync, readFileSync, renameSync, rmSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { pathToFileURL } from "node:url";

const DEFAULT_MODEL = "jev-1.13.0";
const MAX_STATE_BYTES = 96_000;
const NEGATIVE_SUPPORT_THRESHOLD = 0.5;
const NOUL_REVIEW_MARGIN = 0.15;
const STRENGTHS = ["strong", "weak", "misplaced", "impossible", "absent"] as const;
const CONFLICTS = ["none", "present", "insufficient_context"] as const;

interface Evidence {
  [key: string]: string;
  kind: string;
  source: string;
  observation: string;
}

export interface BundleItem {
  [key: string]: string | string[] | Evidence[];
  id: string;
  contract: string;
  rules: string[];
  actorBoundary: string;
  claim: string;
  evidence: Evidence[];
  negativeObligations: string[];
  counterEvidence: string[];
}

interface Bundle {
  schemaVersion: 1;
  useCase: string;
  submission: string;
  items: BundleItem[];
}

interface Arguments {
  bundle: string;
  output?: string;
  validateOnly: boolean;
  model: string;
  confidenceFloor: number;
  timeoutMs: number;
}

interface Judgment {
  choice: string | null;
  confidence: number | null;
  probabilities: Record<string, number>;
}

interface NegativeObligationJudgment {
  index: number;
  obligation: string;
  supportProbability: number | null;
  supported: boolean | null;
  nearThreshold: boolean;
}

interface ItemResult {
  id: string;
  modelObserved: string | null;
  judgments: Record<string, Judgment>;
  negativeObligations: NegativeObligationJudgment[];
  reasons: string[];
  severity: "clear" | "review" | "finding";
  flagged: boolean;
  flags: string[];
}

class BundleError extends Error {}

function nonemptyString(value: unknown, field: string): string {
  if (typeof value !== "string" || value.trim() === "") {
    throw new BundleError(`${field} must be a non-empty string`);
  }
  return value.trim();
}

function stringArray(value: unknown, field: string): string[] {
  if (!Array.isArray(value)) throw new BundleError(`${field} must be an array`);
  return value.map((item, index) => nonemptyString(item, `${field}[${index}]`));
}

function objectValue(value: unknown, field: string): Record<string, unknown> {
  if (typeof value !== "object" || value === null || Array.isArray(value)) {
    throw new BundleError(`${field} must be an object`);
  }
  return value as Record<string, unknown>;
}

function validateBundle(value: unknown): Bundle {
  const raw = objectValue(value, "bundle");
  if (raw.schemaVersion !== 1) throw new BundleError("schemaVersion must be 1");

  const useCase = nonemptyString(raw.useCase, "useCase");
  if (!/^UC-\d+$/.test(useCase)) throw new BundleError("useCase must have the form UC-n");
  const submission = nonemptyString(raw.submission, "submission");
  if (!Array.isArray(raw.items) || raw.items.length === 0) {
    throw new BundleError("items must be a non-empty array");
  }

  const ids = new Set<string>();
  const items = raw.items.map((value, index): BundleItem => {
    const prefix = `items[${index}]`;
    const rawItem = objectValue(value, prefix);
    const id = nonemptyString(rawItem.id, `${prefix}.id`);
    if (ids.has(id)) throw new BundleError(`duplicate item id: ${id}`);
    ids.add(id);

    if (!Array.isArray(rawItem.evidence)) {
      throw new BundleError(`${prefix}.evidence must be an array`);
    }
    const evidence = rawItem.evidence.map((entry, evidenceIndex): Evidence => {
      const evidencePrefix = `${prefix}.evidence[${evidenceIndex}]`;
      const rawEvidence = objectValue(entry, evidencePrefix);
      return {
        kind: nonemptyString(rawEvidence.kind, `${evidencePrefix}.kind`),
        source: nonemptyString(rawEvidence.source, `${evidencePrefix}.source`),
        observation: nonemptyString(rawEvidence.observation, `${evidencePrefix}.observation`),
      };
    });

    const item: BundleItem = {
      id,
      contract: nonemptyString(rawItem.contract, `${prefix}.contract`),
      rules: stringArray(rawItem.rules, `${prefix}.rules`),
      actorBoundary: nonemptyString(rawItem.actorBoundary, `${prefix}.actorBoundary`),
      claim: nonemptyString(rawItem.claim, `${prefix}.claim`),
      evidence,
      negativeObligations: stringArray(
        rawItem.negativeObligations,
        `${prefix}.negativeObligations`,
      ),
      counterEvidence: stringArray(rawItem.counterEvidence, `${prefix}.counterEvidence`),
    };
    const stateBytes = Buffer.byteLength(JSON.stringify(item), "utf8");
    if (stateBytes > MAX_STATE_BYTES) {
      throw new BundleError(
        `${id} serializes to ${stateBytes} bytes; narrow it below ${MAX_STATE_BYTES}`,
      );
    }
    return item;
  });

  return { schemaVersion: 1, useCase, submission, items };
}

function valueAfter(args: string[], index: number, option: string): string {
  const value = args[index + 1];
  if (value === undefined || value.startsWith("--")) {
    throw new BundleError(`${option} requires a value`);
  }
  return value;
}

function parseArguments(args: string[]): Arguments {
  let bundle: string | undefined;
  let output: string | undefined;
  let validateOnly = false;
  let model = DEFAULT_MODEL;
  let confidenceFloor = 0.8;
  let timeoutMs = 30_000;

  for (let index = 0; index < args.length; index += 1) {
    const argument = args[index];
    switch (argument) {
      case "--bundle":
        bundle = valueAfter(args, index, argument);
        index += 1;
        break;
      case "--output":
        output = valueAfter(args, index, argument);
        index += 1;
        break;
      case "--model":
        model = valueAfter(args, index, argument);
        index += 1;
        break;
      case "--confidence-floor":
        confidenceFloor = Number(valueAfter(args, index, argument));
        index += 1;
        break;
      case "--timeout-ms":
        timeoutMs = Number(valueAfter(args, index, argument));
        index += 1;
        break;
      case "--validate-only":
        validateOnly = true;
        break;
      default:
        throw new BundleError(`unknown argument: ${argument ?? ""}`);
    }
  }

  if (bundle === undefined) throw new BundleError("--bundle is required");
  if (!Number.isFinite(confidenceFloor) || confidenceFloor < 0 || confidenceFloor > 1) {
    throw new BundleError("--confidence-floor must be between 0 and 1");
  }
  if (!Number.isFinite(timeoutMs) || timeoutMs <= 0) {
    throw new BundleError("--timeout-ms must be positive");
  }
  if (validateOnly && output !== undefined) {
    throw new BundleError("--output is not used with --validate-only");
  }
  if (!validateOnly && output === undefined) {
    throw new BundleError("--output is required unless --validate-only is used");
  }
  return { bundle, output, validateOnly, model, confidenceFloor, timeoutMs };
}

export function buildQuestionSet(item: BundleItem): Questions {
  const questions: Questions = {
    strength: choice(
      "Assess only whether `evidence` establishes the positive `contract` and `rules` at `actorBoundary`. Negative obligations are evaluated separately. Treat missing or sampled coverage as weak, not as a conflict. Do not infer unsupplied facts.",
      {
        strong:
          "Every positive condition is directly established at the required actor or technical boundary. Do not require negative-obligation proof for this choice.",
        weak:
          "Relevant evidence exists but is partial, sampled, indirect, or omits a positive condition.",
        misplaced:
          "Relevant evidence exists only below or at a different boundary from the one the contract requires.",
        impossible:
          "Supplied evidence or counterevidence explicitly shows that a required production condition cannot occur.",
        absent: "No concrete supplied evidence establishes the positive contract element.",
      },
    ),
  };

  if (item.counterEvidence.length > 0) {
    questions.conflict = choice(
      "Does a supplied `counterEvidence` statement directly contradict `contract` or `rules`? Missing tests, sampled coverage, indirect evidence, and other limitations are not contradictions.",
      {
        none: "No counterevidence statement directly contradicts the contract or rules.",
        present:
          "A counterevidence statement explicitly describes production behavior or data incompatible with the contract or rules.",
        insufficient_context:
          "A counterevidence statement may contradict the contract, but its wording is ambiguous. Mere missing proof does not qualify.",
      },
    );
  }

  item.negativeObligations.forEach((obligation, index) => {
    questions[`negative_${index}`] = noul(
      {
        question:
          "Does at least one supplied evidence observation directly prove this one negative obligation at the actorBoundary? Answer no when absence is only inferred from success, source structure, or unrelated evidence.",
        actorBoundary: item.actorBoundary,
        obligation,
      },
      {
        true: "Direct evidence observes that this prohibited side effect or disclosure does not occur.",
        false: "The obligation is unproved, indirect, or only inferred.",
      },
    );
  });
  return questions;
}

function parseJudgment(
  questionId: string,
  answer: unknown,
  allowed: readonly string[],
  confidenceFloor: number,
): { judgment: Judgment; flags: string[] } {
  const flags: string[] = [];
  if (typeof answer !== "object" || answer === null) {
    return {
      judgment: { choice: null, confidence: null, probabilities: {} },
      flags: [`${questionId}:malformed_answer`],
    };
  }
  const raw = answer as Record<string, unknown>;
  let answerChoice = typeof raw.choice === "string" ? raw.choice : null;
  let confidence = typeof raw.confidence === "number" ? raw.confidence : null;
  if (answerChoice === null || !allowed.includes(answerChoice)) {
    flags.push(`${questionId}:unknown_choice`);
    answerChoice = null;
  }
  if (confidence === null || confidence < 0 || confidence > 1) {
    flags.push(`${questionId}:invalid_confidence`);
    confidence = null;
  } else if (confidence < confidenceFloor) {
    flags.push(`${questionId}:low_confidence`);
  }

  const probabilities: Record<string, number> = {};
  const rawProbabilities = raw.probabilities;
  if (typeof rawProbabilities !== "object" || rawProbabilities === null) {
    flags.push(`${questionId}:missing_probabilities`);
  } else {
    for (const label of allowed) {
      const probability = (rawProbabilities as Record<string, unknown>)[label];
      if (typeof probability !== "number" || probability < 0 || probability > 1) {
        flags.push(`${questionId}:invalid_probability:${label}`);
      } else {
        probabilities[label] = probability;
      }
    }
  }
  return { judgment: { choice: answerChoice, confidence, probabilities }, flags };
}

function parseNegativeObligation(
  item: BundleItem,
  index: number,
  answer: unknown,
): { judgment: NegativeObligationJudgment; flags: string[] } {
  const questionId = `negative_${index}`;
  const obligation = item.negativeObligations[index] ?? "";
  if (typeof answer !== "object" || answer === null) {
    return {
      judgment: {
        index,
        obligation,
        supportProbability: null,
        supported: null,
        nearThreshold: false,
      },
      flags: [`${questionId}:malformed_answer`],
    };
  }
  const probability = (answer as Record<string, unknown>).noul;
  if (typeof probability !== "number" || probability < 0 || probability > 1) {
    return {
      judgment: {
        index,
        obligation,
        supportProbability: null,
        supported: null,
        nearThreshold: false,
      },
      flags: [`${questionId}:invalid_probability`],
    };
  }
  const nearThreshold = Math.abs(probability - NEGATIVE_SUPPORT_THRESHOLD) < NOUL_REVIEW_MARGIN;
  return {
    judgment: {
      index,
      obligation,
      supportProbability: probability,
      supported: probability >= NEGATIVE_SUPPORT_THRESHOLD,
      nearThreshold,
    },
    flags: nearThreshold ? [`${questionId}:near_threshold`] : [],
  };
}

export function evaluateAnswers(
  item: BundleItem,
  modelObserved: unknown,
  answers: Record<string, unknown>,
  confidenceFloor: number,
  modelExpected: string,
): ItemResult {
  const strength = parseJudgment("strength", answers.strength, STRENGTHS, confidenceFloor);
  const conflict =
    item.counterEvidence.length === 0
      ? {
          judgment: { choice: "none", confidence: null, probabilities: {} } as Judgment,
          flags: [] as string[],
        }
      : parseJudgment("conflict", answers.conflict, CONFLICTS, confidenceFloor);
  const negativeResults = item.negativeObligations.map((_, index) =>
    parseNegativeObligation(item, index, answers[`negative_${index}`]),
  );
  const negativeObligations = negativeResults.map((result) => result.judgment);
  const negativeChoice =
    negativeObligations.length === 0
      ? "not_applicable"
      : negativeObligations.every((obligation) => obligation.supported === true)
        ? "complete"
        : "missing";
  const negativeProof: Judgment = {
    choice: negativeChoice,
    confidence: null,
    probabilities: {},
  };
  const observed = typeof modelObserved === "string" ? modelObserved : null;
  const reviewFlags = [
    ...strength.flags,
    ...conflict.flags,
    ...negativeResults.flatMap((result) => result.flags),
  ];
  const findingFlags: string[] = [];
  const reasons: string[] = [];

  if (strength.judgment.choice !== "strong") {
    findingFlags.push(`strength:${strength.judgment.choice ?? "unknown"}`);
    reasons.push(`Positive evidence strength is ${strength.judgment.choice ?? "unknown"}.`);
  }
  if (negativeChoice === "missing") {
    findingFlags.push("negative_proof:missing");
    negativeObligations
      .filter((obligation) => obligation.supported !== true)
      .forEach((obligation) => {
        const probability =
          obligation.supportProbability === null
            ? "unknown"
            : obligation.supportProbability.toFixed(3);
        reasons.push(
          `Negative obligation ${obligation.index + 1} is unsupported (p=${probability}): ${obligation.obligation}`,
        );
      });
  }
  if (conflict.judgment.choice === "present") {
    findingFlags.push("conflict:present");
    reasons.push("Supplied material is classified as directly contradictory.");
  } else if (conflict.judgment.choice === "insufficient_context") {
    reviewFlags.push("conflict:insufficient_context");
    reasons.push("A possible contradiction needs human review.");
  }
  if (observed !== modelExpected) reviewFlags.push("model:unexpected_version");

  const uniqueFlags = [...new Set([...findingFlags, ...reviewFlags])].sort();
  const severity = findingFlags.length > 0 ? "finding" : reviewFlags.length > 0 ? "review" : "clear";
  return {
    id: item.id,
    modelObserved: observed,
    judgments: {
      strength: strength.judgment,
      negative_proof: negativeProof,
      conflict: conflict.judgment,
    },
    negativeObligations,
    reasons,
    severity,
    flagged: uniqueFlags.length > 0,
    flags: uniqueFlags,
  };
}

function writeAtomic(path: string, value: unknown): void {
  const destination = resolve(path);
  mkdirSync(dirname(destination), { recursive: true });
  const temporary = `${destination}.${process.pid}.tmp`;
  try {
    writeFileSync(temporary, `${JSON.stringify(value, null, 2)}\n`, { encoding: "utf8", mode: 0o600 });
    renameSync(temporary, destination);
  } finally {
    rmSync(temporary, { force: true });
  }
}

async function main(): Promise<number> {
  let args: Arguments;
  let bundle: Bundle;
  try {
    args = parseArguments(process.argv.slice(2));
    bundle = validateBundle(JSON.parse(readFileSync(resolve(args.bundle), "utf8")));
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    console.error(`checkpoint input failed: ${message}`);
    return 2;
  }

  if (args.validateOnly) {
    console.log(`valid bundle: ${bundle.useCase} with ${bundle.items.length} item(s)`);
    return 0;
  }
  if (!process.env.TYPESAFE_API_KEY?.trim()) {
    console.error("TYPESAFE_API_KEY is not available");
    return 3;
  }

  const client = new TypeSafeClient({
    defaultModel: args.model,
    timeout: args.timeoutMs,
    logLevel: "error",
    retry: {
      maxRetries: 2,
      httpStatuses: new Set([429, 529]),
      apiConnectionError: false,
      apiTimeoutError: false,
    },
  });

  const results: ItemResult[] = [];
  let inputTokens = 0;
  let outputTokens = 0;
  try {
    for (const item of bundle.items) {
      const response = await client.systemOne({
        state: {
          contract: item.contract,
          rules: item.rules,
          actorBoundary: item.actorBoundary,
          evidence: item.evidence,
          counterEvidence: item.counterEvidence,
        },
        model: args.model,
        questions: buildQuestionSet(item),
      });
      inputTokens += response.usage.input_tokens;
      outputTokens += response.usage.output_tokens;
      results.push(
        evaluateAnswers(
          item,
          response.model,
          response.answers as unknown as Record<string, unknown>,
          args.confidenceFloor,
          args.model,
        ),
      );
    }
  } catch (error) {
    const name = error instanceof Error ? error.name : "TypeSafeError";
    console.error(`TypeSafe preflight failed: ${name}`);
    return 4;
  }

  const flagged = results.filter((result) => result.flagged).length;
  const findings = results.filter((result) => result.severity === "finding").length;
  const reviews = results.filter((result) => result.severity === "review").length;
  const clear = results.filter((result) => result.severity === "clear").length;
  writeAtomic(args.output!, {
    schemaVersion: 2,
    useCase: bundle.useCase,
    submission: bundle.submission,
    modelRequested: args.model,
    confidenceFloor: args.confidenceFloor,
    generatedAt: new Date().toISOString(),
    advisoryOnly: true,
    summary: { items: results.length, flagged, findings, reviews, clear },
    usage: { requests: results.length, inputTokens, outputTokens },
    items: results,
  });
  console.log(
    `Jev preflight complete: ${results.length} item(s), ${findings} finding(s), ${reviews} review(s), ${clear} clear`,
  );
  return 0;
}

const invokedPath = process.argv[1];
if (invokedPath !== undefined && import.meta.url === pathToFileURL(resolve(invokedPath)).href) {
  process.exitCode = await main();
}
