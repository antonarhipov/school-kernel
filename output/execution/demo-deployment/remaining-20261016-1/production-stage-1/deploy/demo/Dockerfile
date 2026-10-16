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

FROM eclipse-temurin:25-jre@sha256:fd4b1c02c64eef378212e23656fc0b0caa41a357876fa0b60e51ceda94a3db64
WORKDIR /opt/demo
RUN groupadd --gid 10001 demo && useradd --uid 10001 --gid demo --no-create-home demo
COPY --from=build /build/kernel-cli/target/school-kernel.jar /opt/demo/kernel.jar
COPY --from=build /build/timetable-workspace/target/timetable-workspace-1.0.0-SNAPSHOT.jar /opt/demo/workspace.jar
COPY --chmod=0555 deploy/demo/start deploy/demo/kernel /opt/demo/
ENV SPRING_PROFILES_ACTIVE=demo \
    SCHOOL_KERNEL_EXECUTABLE=/opt/demo/kernel \
    DEMO_KERNEL_ARTIFACT=/opt/demo/kernel.jar
USER 10001:10001
EXPOSE 8080
STOPSIGNAL SIGTERM
ENTRYPOINT ["/opt/demo/start"]
