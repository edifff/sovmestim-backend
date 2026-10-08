# syntax=docker/dockerfile:1

# ---------- build ----------
# The build image pins JDK 25 and Maven 3.9.x. Inside the image the bundled Maven is used
# (the Maven wrapper is for local builds, where it guarantees the same 3.9.x line).
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace

# Resolve dependencies first so source changes do not invalidate the dependency layer.
# The BuildKit cache mount keeps the Maven repository between builds.
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests dependency:resolve

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package

# ---------- runtime ----------
FROM eclipse-temurin:25-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --uid 10001 appuser

WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar

USER appuser
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
    CMD curl -fsS http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
