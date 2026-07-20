# syntax=docker/dockerfile:1.7

FROM maven:3.9.11-eclipse-temurin-17@sha256:e4a7ace3dc0d645ed97f8d9ad0b0d3f0b14fa8d150138f27f116d7105a639b82 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -B -Dstyle.color=never

FROM eclipse-temurin:25-jre-ubi10-minimal@sha256:449e837febf063209a19d1885e51d645fab64200b0bffda662d9738c3dc36a56

WORKDIR /app

COPY --from=build --chown=10001:10001 /app/target/*.jar app.jar

EXPOSE 8081

HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=5 \
  CMD wget --quiet --spider --header='X-Forwarded-Proto: https' http://localhost:8081/actuator/health/readiness || exit 1

USER 10001:10001

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
