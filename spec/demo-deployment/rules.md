# Technical Rules: demo deployment

## Design overview

The demo retains the existing Spring Security host, route, CSRF and owner filters. Railway HTTP ingress uses an
explicit isolated-environment trust option because the provider does not publish stable proxy peer addresses.
Only scheme and matching forwarded host are consumed; forwarding never selects authority or visitor ownership.
The normal profile and the default stable-address trust option retain their existing behavior.

## Codebase alignment

Spring Boot configuration binds the opt-in only in the demo profile. Deployment identity and generated-domain
checks reject accidental activation outside Railway. No provider API credential enters the service or image.
Provider topology is checked by the operator using their existing authenticated CLI before deployment and after
restart. This is a deployment invariant, not a claim that headers authenticate a private caller.

## Security surface

| Surface | Access |
|---|---|
| GET `/healthz` using platform health authority | Health only, no session or school data |
| Workspace shell/static assets and public sample reads | Exact configured public HTTPS authority |
| CSRF/config/session restart | Same public gate; restart requires CSRF and same-origin POST |
| Every workspace/import/export/draft/run/review/accept/cancel route | Existing route allowlist, owner binding, lifecycle/version guards; mutations also require CSRF and Origin |
| Structured authoring and return-to-authoring | 404 in demo |
| Other routes | Existing deny-by-default rules |

## Verification strategy

Unit checks establish default refusal, explicit trust activation, deployment configuration validation, strict
single-value forwarding, native authority, same origin and session-free denial. Public probes demonstrate edge
replacement of forged forwarding values, HTTP redirection and safe refusals. The isolated two-browser UC-2 journey
is repeated across operator-controlled restarts, with exact Current/non-mutation checks and retained browser text.
Local profile regression evidence remains applicable to unchanged paths; failed prior attempts are preserved.

## Rules

### RULE-1 - Trust the audited isolated Railway ingress boundary

- Applies to: UC-2, UC-12
- Constraint: MUST leave `demo.railway-isolated-ingress` off by default. Enabling it MUST require Railway deployment
  identities and the exact generated domain; an operator preflight MUST show exactly this one service in the
  project/environment, one replica, and no TCP proxy. Adding another service, raw TCP ingress, or another domain
  MUST trigger requalification before serving visitors. The option MUST NOT be combined with address allowlists.
- Reason: [Railway staff confirmation](https://station.railway.com/questions/fastapi-uvicorn-trusted-proxy-configura-9c06fa12)
  states there is no stable socket-peer CIDR or signed edge identity. Private callers and TCP proxies bypass header
  sanitization. [Private networking](https://docs.railway.com/networking/private-networking/how-it-works) isolates
  projects/environments; [public edge specifications](https://docs.railway.com/networking/public-networking/specs-and-limits)
  and staff confirmation establish HTTPS and forwarding replacement. Trust therefore belongs to the audited network
  topology, never a guessed subnet, an X-Railway header, or a claimed client address.
- Verification: Retain read-only topology/TCP evidence, deployment configuration checks, direct/default-mode
  negative tests, deployed spoof probes and repeated restart/browser results. No forwarded header supplies Host,
  an owner identifier, or a redirect target. Malformed/duplicate scheme/host, foreign Origin and Forwarded remain denied.

## Use-case cross-reference

| Use case | Rules |
|---|---|
| UC-1 | Inherited specification |
| UC-2 | RULE-1 |
| UC-3 | Inherited UC-2 access boundary |
| UC-4 | Inherited UC-2 access boundary |
| UC-5 | Inherited UC-2 access boundary |
| UC-6 | Inherited UC-2 access boundary |
| UC-7 | Inherited UC-2 access boundary |
| UC-8 | Inherited UC-2 access boundary |
| UC-9 | Inherited UC-2 access boundary |
| UC-10 | Inherited UC-2 access boundary |
| UC-11 | Inherited UC-2 access boundary |
| UC-12 | RULE-1 |
| UC-13 | Inherited specification |

## Design exclusions

No stable Railway CIDR is invented. Neither a successful Free-plan session test nor this ingress decision qualifies
Hobby capacity, billing controls, solver accounting or the remaining use cases.
