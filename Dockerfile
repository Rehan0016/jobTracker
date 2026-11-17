# ===================================================================
# Multi-stage Dockerfile for JobTracker Spring Boot Backend
# ===================================================================

# -------------------------------------------------------------------
# Stage 1: Builder Stage
# Uses Maven with Eclipse Temurin JDK 17 to compile and test
# -------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Build argument to control whether tests run during image build (default: false = tests run)
ARG SKIP_TESTS=false

# Copy Maven project descriptor
COPY pom.xml .

# Copy source code
COPY src ./src

# Package the application JAR and execute test suite
RUN mvn clean package -DskipTests=${SKIP_TESTS} -B

# -------------------------------------------------------------------
# Stage 2: Runtime Stage
# Minimal Eclipse Temurin JRE 17 Alpine image running as non-root
# -------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

# Create a dedicated non-root user and group
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy only the compiled executable JAR from the builder stage
COPY --from=builder /app/target/jobtracker-0.0.1-SNAPSHOT.jar app.jar

# Enforce file permissions for non-root execution
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8080

# Enforce production profile by default
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

# Health-check endpoint verification
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -q --spider http://localhost:8080/api/health || exit 1

# Launch Spring Boot with container memory optimization flags
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
