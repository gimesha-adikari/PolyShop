# syntax=docker/dockerfile:1.6

FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

COPY src ./src

RUN chmod +x ./gradlew

RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

ENV SPRING_PROFILES_ACTIVE=prod

ENV AUTH_DB_URL=""
ENV AUTH_DB_USERNAME=""
ENV AUTH_DB_PASSWORD=""
ENV AUTH_JWT_PRIVATE_KEY=""
ENV AUTH_JWT_EXPIRES_IN=900
ENV AUTH_REFRESH_EXPIRES_IN=2592000
ENV JWT_PUBLIC_KEY=""
ENV JWT_ISSUER="polyshop-auth"

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -fsS http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java","-jar","/app/app.jar"]
