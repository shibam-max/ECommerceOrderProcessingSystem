# ============================================================
# Stage 1: Build — compile and package with Maven
# ============================================================
FROM maven:3.8.8-eclipse-temurin-8 AS builder

WORKDIR /app
COPY pom.xml .
# Download dependencies first (Docker layer caching optimization)
RUN mvn dependency:go-offline -B -q

COPY src ./src
RUN mvn package -B -DskipTests -q

# ============================================================
# Stage 2: Runtime — minimal JRE image (no build tools)
# ============================================================
FROM eclipse-temurin:8-jre-alpine

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /app/target/*.jar app.jar

RUN mkdir -p /app/logs && chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
