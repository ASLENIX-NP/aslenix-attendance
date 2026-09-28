# ==============================================================================
# STAGE 1: BUILD STAGE
# ==============================================================================
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /build

# 1. Copy Maven Wrapper and POM first to leverage Docker layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Normalize CRLF line endings in mvnw (prevents Windows-to-Linux script errors)
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Download dependencies in a separate cached layer
RUN ./mvnw dependency:go-offline -B || true

# 2. Copy source code and build production jar
COPY src/ ./src/
RUN ./mvnw clean package -DskipTests

# ==============================================================================
# STAGE 2: PRODUCTION RUNTIME STAGE (Render-optimized)
# ==============================================================================
FROM eclipse-temurin:25-jre

WORKDIR /app

# Create a non-root system user and group for security
RUN groupadd -r appgroup && useradd -r -g appgroup -u 10001 -d /app -s /sbin/nologin appuser

# Create upload directory structure for employee profile photos and files
RUN mkdir -p /app/uploads/photos && chown -R appuser:appgroup /app

# Copy executable jar from builder stage
COPY --from=builder --chown=appuser:appgroup /build/target/aslenix-attendance-*.jar /app/app.jar

# Switch to non-root user
USER appuser

# Default port (Render automatically sets $PORT dynamically at runtime)
ENV PORT=8080

# JVM Container memory optimization for Render (avoids OOM kills on 512MB RAM free/starter tiers)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=40.0 -XX:+ExitOnOutOfMemoryError"

# Expose default port
EXPOSE 8080

# Run with exec so PID 1 signal forwarding (SIGTERM for graceful shutdown) works properly
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
