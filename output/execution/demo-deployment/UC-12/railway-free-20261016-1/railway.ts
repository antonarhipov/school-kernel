import { defineRailway, project, service } from "railway/iac";

export default defineRailway(() => {
  const demoSmoke = service("demo-smoke", {
    build: { buildEnvironment: "V3", builder: "DOCKERFILE", dockerfilePath: "deploy/demo/Dockerfile" },
    healthcheck: "/healthz",
    healthcheckTimeout: 180,
    replicas: { "europe-west4-drams3a": 1 },
    deploy: { drainingSeconds: 30, limitOverride: { containers: { cpu: 1, memoryBytes: 500000000 } }, overlapSeconds: 0, restartPolicyMaxRetries: 1, sleepApplication: true },
  });

  return project("school-kernel-free-smoke", {
    variables: { managed: false },
    resources: [demoSmoke],
  });
});
