# ============================================================
# Stage 1: Build JAR with Maven & Eclipse Temurin Java 21
# ============================================================
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /build

# Copy Maven Wrapper and POM
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Convert line endings for Linux containers if created on Windows
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Download dependencies offline for caching
RUN ./mvnw dependency:go-offline -B || true

# Copy source code and build production fat JAR
COPY src/ src/
RUN ./mvnw clean package -DskipTests

# ============================================================
# Stage 2: Minimal Production JRE Runtime
# ============================================================
FROM eclipse-temurin:21-jre

LABEL maintainer="Cognivault AI Team"
LABEL description="Production container for Cognivault AI Spring Boot Backend"

# Create unprivileged system user for security
RUN groupadd -r cognivault && useradd -r -g cognivault -m -d /app cognivault

WORKDIR /app

# Create upload directory and grant permissions
RUN mkdir -p /app/uploads && chown -R cognivault:cognivault /app

# Copy executable jar from builder stage
COPY --from=builder /build/target/ai-knowledge-assistant-0.0.1-SNAPSHOT.jar /app/app.jar
RUN chown cognivault:cognivault /app/app.jar

USER cognivault:cognivault

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/auth/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=prod", "-jar", "/app/app.jar"]
