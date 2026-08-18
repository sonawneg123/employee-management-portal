# ──────────────────────────────────────────────────────────────────────────────
#  Backend Dockerfile — Employee Management Portal
#  Multi-stage build: Maven build → minimal JRE runtime
#  Runtime: eclipse-temurin:21-jre-alpine (non-root user)
# ──────────────────────────────────────────────────────────────────────────────

# ── Stage 1: Dependency cache ─────────────────────────────────────────────────
# Download all Maven dependencies into a dedicated layer.
# This layer is only invalidated when pom.xml changes, not when source changes.
FROM maven:3.9.9-eclipse-temurin-21-alpine AS deps
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress

# ── Stage 2: Build ────────────────────────────────────────────────────────────
FROM deps AS builder
COPY src ./src
RUN mvn clean package -DskipTests -B --no-transfer-progress

# ── Stage 3: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Install tini for proper signal handling (PID 1)
RUN apk add --no-cache tini

# Create a non-root user and group
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy only the packaged JAR — no source code in the runtime image
# COPY --from=builder /build/target/*.jar app.jar
COPY --from=builder --chown=appuser:appgroup /build/target/*.jar app.jar
# Set ownership to the non-root user
# RUN chown appuser:appgroup app.jar

# Switch to non-root user
USER appuser

# Spring Boot listens on 8080 by default (also set in application.properties)
EXPOSE 8080

# Use tini as the init process for clean signal forwarding
ENTRYPOINT ["/sbin/tini", "--", "java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]
