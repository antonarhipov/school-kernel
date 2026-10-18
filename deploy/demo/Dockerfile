FROM maven:3.9.11-eclipse-temurin-25@sha256:407c4423cec0cf2981055bc2c6c0dc211d9605b6669279b95997f2d1c7e91e2c AS build
WORKDIR /build
COPY pom.xml ./
COPY kernel-contract/pom.xml kernel-contract/pom.xml
COPY kernel-cli/pom.xml kernel-cli/pom.xml
COPY timetable-workspace/pom.xml timetable-workspace/pom.xml
COPY kernel-contract/src/main kernel-contract/src/main
COPY kernel-cli/src/main kernel-cli/src/main
COPY timetable-workspace/src/main timetable-workspace/src/main
RUN mvn --batch-mode --no-transfer-progress -Dmaven.test.skip=true package

FROM ghcr.io/graalvm/native-image-community:25.0.1@sha256:2fec8f17e9eed1a14987ebf7610524622b91193ffd3d12619f47d7c2f2cab844 AS native-build
WORKDIR /build
COPY --from=build /usr/share/maven /usr/share/maven
COPY --from=build /build /build
RUN /usr/share/maven/bin/mvn --batch-mode --no-transfer-progress -pl timetable-workspace -am \
    -Pnative-demo -Dmaven.test.skip=true -DbuildArgs=-march=compatibility,--parallelism=2 -DjvmArgs=-Xmx4g package

FROM eclipse-temurin:25-jre@sha256:fd4b1c02c64eef378212e23656fc0b0caa41a357876fa0b60e51ceda94a3db64 AS runtime
WORKDIR /opt/demo
RUN groupadd --gid 10001 demo && useradd --uid 10001 --gid demo --no-create-home demo
COPY --from=build /build/kernel-cli/target/school-kernel.jar /opt/demo/kernel.jar
COPY --chmod=0555 deploy/demo/kernel /opt/demo/kernel
ENV SPRING_PROFILES_ACTIVE=demo \
    SCHOOL_KERNEL_EXECUTABLE=/opt/demo/kernel \
    DEMO_KERNEL_ARTIFACT=/opt/demo/kernel.jar
USER 10001:10001
EXPOSE 8080
STOPSIGNAL SIGTERM
ENTRYPOINT ["/opt/demo/start"]

FROM runtime AS jvm
COPY --from=build /build/timetable-workspace/target/timetable-workspace-1.0.0-SNAPSHOT.jar /opt/demo/workspace.jar
COPY --chmod=0555 deploy/demo/start /opt/demo/start

FROM runtime AS native
COPY --from=native-build /build/timetable-workspace/target/school-demo /opt/demo/workspace
COPY --chmod=0555 deploy/demo/start-native /opt/demo/start
