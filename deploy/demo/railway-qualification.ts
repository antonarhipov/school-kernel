// Prepared qualification configuration. Never applied by implementation or local tests.
// The operator must qualify Hobby capacity and set workspace-wide spend controls separately.
import { defineRailway, project, service } from "railway/iac";

export default defineRailway(() => project("school-kernel-demo", {
  variables: { managed: false },
  resources: [service("demo", {
    build: { buildEnvironment: "V3", builder: "DOCKERFILE", dockerfilePath: "deploy/demo/Dockerfile" },
    healthcheck: "/healthz",
    healthcheckTimeout: 180,
    replicas: { "europe-west4-drams3a": 1 },
    deploy: {
      drainingSeconds: 30,
      overlapSeconds: 0,
      limitOverride: { containers: { cpu: 2, memoryBytes: 3000000000 } },
      restartPolicyMaxRetries: 1,
      sleepApplication: false,
    },
  })],
}));
