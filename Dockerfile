# syntax=docker/dockerfile:1.7

FROM maven:3.9.15-eclipse-temurin-26@sha256:029a8e2838ae68238ffb8be407cddbb3f07d4d839c60c6f26c619a69fd184531 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -B -Dstyle.color=never

FROM eclipse-temurin:17-jre-ubi10-minimal@sha256:f5d48f4d9d15ef5e60ebd12232698ad1f152bb24e2daa7453e4ff0de1341da8d

WORKDIR /app

COPY --from=build --chown=10001:10001 /app/target/*.jar app.jar

EXPOSE 8081

HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=5 \
  CMD wget --quiet --spider --header='X-Forwarded-Proto: https' http://localhost:8081/actuator/health/readiness || exit 1

USER 10001:10001

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
