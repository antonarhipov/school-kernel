#!/usr/bin/env node

import { choice, TypeSafeClient } from "@typesafe-ai/sdk";
import { mkdirSync, readFileSync, renameSync, rmSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";

const DEFAULT_MODEL = "jev-1.13.0";
const MAX_STATE_BYTES = 96_000;
const STRENGTHS = ["strong", "weak", "misplaced", "impossible", "absent"] as const;
const NEGATIVE_PROOFS = ["complete", "missing", "not_applicable"] as const;
const CONFLICTS = ["none", "present", "insufficient_context"] as const;

interface Evidence {
  [key: string]: string;
  kind: string;
  source: string;
  observation: string;
}

interface BundleItem {
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
}

interface ItemResult {
  id: string;
  modelObserved: string | null;
  judgments: Record<string, Judgment>;
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

const questionSet = {
  strength: choice(
    "Assess only whether `evidence` proves `contract` and `rules` at `actorBoundary`. Treat `claim` as an unverified assertion, use `counterEvidence`, and do not infer facts that are not supplied. Choose the evidence strength.",
    {
      strong:
        "Concrete evidence pins the observable outcome at the contract boundary, including all stated negative obligations.",
      weak:
        "Some relevant evidence exists but is partial, sampled, indirect, or could pass for an implementation that violates the contract.",
      misplaced:
        "The evidence is below or different from the actor or technical boundary required by the contract or rule.",
      impossible:
        "The evidence depends on a value, double, route, or condition that production cannot produce according to the supplied material.",
      absent: "No concrete supplied evidence exercises the contract element.",
    },
  ),
  negative_proof: choice(
    "Assess whether `evidence` proves every item in `negativeObligations`. Do not treat a successful response or exit status alone as proof that prohibited side effects are absent.",
    {
      complete: "Every listed negative obligation has direct supplied evidence.",
      missing: "At least one listed negative obligation lacks direct supplied evidence.",
      not_applicable: "The `negativeObligations` array is empty.",
    },
  ),
  conflict: choice(
    "Determine whether the supplied `claim`, `evidence`, or `counterEvidence` visibly conflicts with `contract` or `rules`. Judge only the supplied state.",
    {
      none: "No conflict is visible in the supplied material.",
      present: "At least one supplied fact contradicts the contract, rule, or claim.",
      insufficient_context: "The supplied material is too incomplete to decide whether a conflict exists.",
    },
  ),
};

function parseJudgment(
  questionId: string,
  answer: unknown,
  allowed: readonly string[],
  confidenceFloor: number,
): { judgment: Judgment; flags: string[] } {
  const flags: string[] = [];
  if (typeof answer !== "object" || answer === null) {
    return {
      judgment: { choice: null, confidence: null },
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
  return { judgment: { choice: answerChoice, confidence }, flags };
}

function evaluateAnswers(
  item: BundleItem,
  modelObserved: unknown,
  answers: Record<string, unknown>,
  confidenceFloor: number,
  modelExpected: string,
): ItemResult {
  const strength = parseJudgment("strength", answers.strength, STRENGTHS, confidenceFloor);
  const negativeProof = parseJudgment(
    "negative_proof",
    answers.negative_proof,
    NEGATIVE_PROOFS,
    confidenceFloor,
  );
  const conflict = parseJudgment("conflict", answers.conflict, CONFLICTS, confidenceFloor);
  const observed = typeof modelObserved === "string" ? modelObserved : null;
  const flags = [...strength.flags, ...negativeProof.flags, ...conflict.flags];

  if (strength.judgment.choice !== "strong") {
    flags.push(`strength:${strength.judgment.choice ?? "unknown"}`);
  }
  if (negativeProof.judgment.choice === "missing") flags.push("negative_proof:missing");
  if (["present", "insufficient_context"].includes(conflict.judgment.choice ?? "")) {
    flags.push(`conflict:${conflict.judgment.choice}`);
  }
  if (observed !== modelExpected) flags.push("model:unexpected_version");

  const uniqueFlags = [...new Set(flags)].sort();
  return {
    id: item.id,
    modelObserved: observed,
    judgments: {
      strength: strength.judgment,
      negative_proof: negativeProof.judgment,
      conflict: conflict.judgment,
    },
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
  try {
    for (const item of bundle.items) {
      const response = await client.systemOne({
        state: item,
        model: args.model,
        questions: questionSet,
      });
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
  writeAtomic(args.output!, {
    schemaVersion: 1,
    useCase: bundle.useCase,
    submission: bundle.submission,
    modelRequested: args.model,
    confidenceFloor: args.confidenceFloor,
    generatedAt: new Date().toISOString(),
    advisoryOnly: true,
    summary: { items: results.length, flagged },
    items: results,
  });
  console.log(`Jev preflight complete: ${results.length} item(s), ${flagged} flagged`);
  return 0;
}

process.exitCode = await main();
