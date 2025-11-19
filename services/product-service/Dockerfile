# syntax=docker/dockerfile:1.6

FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

COPY gradlew build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY src ./src

RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

ENV SPRING_PROFILES_ACTIVE=prod

ENV PRODUCT_DB_URL=""
ENV PRODUCT_DB_USERNAME=""
ENV PRODUCT_DB_PASSWORD=""
ENV MEDIA_BUCKET=""
ENV MEDIA_BASE_URL=""
ENV SEARCH_INDEX_URL=""
ENV JWT_PUBLIC_KEY=""
ENV JWT_ISSUER="polyshop-auth"

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -fsS http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java","-jar","/app/app.jar"]
