import assert from "node:assert/strict";
import test from "node:test";

import { buildQuestionSet, evaluateAnswers, type BundleItem } from "./jev-checkpoint.js";

const item = (negativeObligations: string[] = ["No write", "No disclosure"]): BundleItem => ({
  id: "UC-1 test",
  contract: "The actor sees the complete durable result.",
  rules: [],
  actorBoundary: "Real HTTP process",
  claim: "The result is complete.",
  evidence: [
    {
      kind: "integration-test",
      source: "ExampleIT.java:10",
      observation: "The test observes the response and unchanged database row.",
    },
  ],
  negativeObligations,
  counterEvidence: [],
});

const choiceAnswer = (
  selected: string,
  labels: string[],
  confidence = 0.9,
): Record<string, unknown> => ({
  type: "choice",
  choice: selected,
  confidence,
  probabilities: Object.fromEntries(labels.map((label) => [label, label === selected ? 0.9 : 0.025])),
});

const strongAnswers = (): Record<string, unknown> => ({
  strength: choiceAnswer("strong", ["strong", "weak", "misplaced", "impossible", "absent"]),
  conflict: choiceAnswer("none", ["none", "present", "insufficient_context"]),
  negative_0: { type: "noul", noul: 0.9 },
  negative_1: { type: "noul", noul: 0.85 },
});

test("builds one atomic question for each negative obligation", () => {
  assert.deepEqual(Object.keys(buildQuestionSet(item())), [
    "strength",
    "negative_0",
    "negative_1",
  ]);
});

test("derives a clear result from strong direct evidence", () => {
  const result = evaluateAnswers(item(), "jev-1.13.0", strongAnswers(), 0.8, "jev-1.13.0");
  assert.equal(result.severity, "clear");
  assert.equal(result.judgments.negative_proof?.choice, "complete");
  assert.deepEqual(result.reasons, []);
});

test("names the exact unsupported negative obligation", () => {
  const answers = strongAnswers();
  answers.negative_1 = { type: "noul", noul: 0.2 };
  const result = evaluateAnswers(item(), "jev-1.13.0", answers, 0.8, "jev-1.13.0");
  assert.equal(result.severity, "finding");
  assert.equal(result.negativeObligations[1]?.supported, false);
  assert.match(result.reasons.join("\n"), /No disclosure/);
});

test("keeps incomplete coverage separate from contradiction", () => {
  const answers = strongAnswers();
  answers.strength = choiceAnswer("weak", [
    "strong",
    "weak",
    "misplaced",
    "impossible",
    "absent",
  ]);
  const result = evaluateAnswers(item(), "jev-1.13.0", answers, 0.8, "jev-1.13.0");
  assert.equal(result.severity, "finding");
  assert.equal(result.judgments.conflict?.choice, "none");
  assert.ok(!result.flags.includes("conflict:present"));
});

test("routes low confidence separately from substantive findings", () => {
  const answers = strongAnswers();
  answers.strength = choiceAnswer(
    "strong",
    ["strong", "weak", "misplaced", "impossible", "absent"],
    0.6,
  );
  const result = evaluateAnswers(item(), "jev-1.13.0", answers, 0.8, "jev-1.13.0");
  assert.equal(result.severity, "review");
  assert.ok(result.flags.includes("strength:low_confidence"));
});
