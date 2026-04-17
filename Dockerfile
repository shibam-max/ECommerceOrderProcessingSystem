# ============================================================
# Stage 1: Build — compile and package with Maven
# ============================================================
FROM maven:3.8.8-eclipse-temurin-8 AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

COPY src ./src
RUN mvn package -B -DskipTests -q

# ============================================================
# Stage 2: Runtime — minimal JRE image (no build tools)
# ============================================================
FROM amazoncorretto:8-alpine

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /app/target/*.jar app.jar

RUN mkdir -p /app/logs && chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", \
  "java -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:MaxRAMFraction=2 -Djava.security.egd=file:/dev/./urandom $JAVA_OPTS -jar app.jar"]
