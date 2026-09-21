# Checkpoint Bundle Format

The helper accepts one JSON object:

```json
{
  "schemaVersion": 1,
  "useCase": "UC-2",
  "submission": "acd488c",
  "items": [
    {
      "id": "UC-2 G2",
      "contract": "The current timetable's integrity and provenance are verified before solving.",
      "rules": [
        "RULE-14 requires the UC-1 FEASIBLE postcondition and direct lineage."
      ],
      "actorBoundary": "Packaged school-kernel replan CLI process",
      "claim": "Revision-verifiable invalid current timetables are rejected before solving.",
      "evidence": [
        {
          "kind": "packaged-test",
          "source": "src/test/.../ReplanCliIT.java:120",
          "observation": "The process exits 2 and the test asserts that solver-start logging is absent."
        }
      ],
      "negativeObligations": [
        "No solver invocation",
        "No timetable or change report",
        "Current and existing destination bytes remain unchanged"
      ],
      "counterEvidence": []
    }
  ]
}
```

## Required fields

- `schemaVersion`: exactly `1`
- `useCase`: exactly one `UC-n`
- `submission`: immutable commit, or an exact-diff identifier when commits are prohibited
- `items`: non-empty array with unique `id` values
- Every item: non-empty `id`, `contract`, `actorBoundary`, and `claim`
- `rules`, `evidence`, `negativeObligations`, and `counterEvidence`: arrays; they may be empty when genuinely inapplicable
- Every evidence entry: non-empty `kind`, `source`, and `observation`

Use direct assertion/runtime observations. Do not paste a test name without the assertion it makes, or a checkpoint
claim as its own evidence. Include known counterevidence; hiding it invalidates the preflight.

## Returned judgments

The helper asks atomic questions rather than asking Jev to combine several obligations:

- `strength`: `strong`, `weak`, `misplaced`, `impossible`, or `absent`, considering positive evidence only.
- `conflict`: asked only when explicit counterevidence exists; missing or sampled evidence is not a conflict.
- one Noul per `negativeObligations` entry, returning the probability that direct evidence proves that exact obligation.

The report retains each Choice probability distribution and Noul probability. Code derives `negative_proof`, names
each unsupported obligation in `reasons`, and routes the item as `finding`, `review`, or `clear`. `review` is uncertainty
or model-version drift, not a substantive finding. Jev does not generate the reasons and the helper does not calculate
an approval verdict.

## Initial School Kernel calibration set

Replay these immutable submissions without revealing the later finding in the input bundle:

| Submission | Gold convergence commit | Blocking findings to recover |
|---|---|---|
| `422a774` | `24efd35` | UC-1 C-1 and C-2 |
| `bfb7a40` | `801deb5` | UC-2 C-1 and G-1 through G-3 |
| `acd488c` | `bbbecb1` | UC-2 C-2 |

Also sample `STRONG` rows from the final approved submissions `8c2fb7f` and `e6401f6` to measure false-positive rate.
Track blocking-finding recall, false-positive rate, confidence calibration, and repeatability. Pin the model version and
do not turn the pilot into a blocking gate until its routing thresholds are supported by these results.
