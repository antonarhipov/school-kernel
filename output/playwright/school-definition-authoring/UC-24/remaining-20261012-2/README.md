# UC24 — PASS / automatically APPROVED

2026-10-12, base b1fd27d plus UC23 patch; [exact cumulative submission](submission.patch). Build authoring-remaining-grid-r2 SHA256 d3c322fe44133c747e7cf4d46a183b42a71164668f3446d2d001c8aedf3ee2ca. Real Chrome at18112, retained disposable authoring_ux_20261012, copied school255→257. Operator8080/235 untouched.

| Contract | Action and observed result | Verdict |
|---|---|---|
| Main1–2/G1/G2 |50 explicit cells, weekday/order/time/reservation overview and one inspector; two working row edits retained across selection | PASS |
| Main3/extension3a | Complete grid preview has no write; explicit cancel restores saved rows; time change saves exact definition, then original optional/time values restored255→257 | PASS |
| Extension2a/G3 |1600/1280/700/390 no page overflow, narrow saved inspector/no inputs, wide working edits retained; Stay preserves edits | PASS |
| G1/G2/postconditions | Entire source/Current retained; complete restored definition and saved snapshot survive reload; zero page errors | PASS |
| UC3 extensions/relationship |7 original grid cases and5 review cases PASS: generation/additions/invalid rows/reservations/stale/conflict/access/storage/transport/source/timing/restart; direct review selects intended period inspector | PASS |

[Journey](journey.js), [full saved values](values.json), [technical log](technical.log).33 unit/architecture+12 browser cases, zero failures/errors/skips; Maven verify includes DefinitionGridBrowserIT,DefinitionReviewBrowserIT. Wide and390 screenshots inspected;1280/700 same structural checks recorded. Initial7-case run had2 stale all-rows helper errors; helper now waits desktop inspector and explicitly selects requested slot, with all original durable assertions retained. Failed initial log/clone-VM journey retained in attempt1; exact original times were explicitly restored from UC23 saved253 before repeat. No backend/schema change. Owned browser/server retained for next UC; no handoff.
