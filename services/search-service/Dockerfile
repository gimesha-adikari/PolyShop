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

ENV SEARCH_ELASTIC_URL=""
ENV SEARCH_INDEX_NAME="products"
ENV SEARCH_USERNAME=""
ENV SEARCH_PASSWORD=""
ENV JWT_PUBLIC_KEY=""
ENV JWT_ISSUER="polyshop-auth"

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8087

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -fsS http://localhost:8087/actuator/health || exit 1

ENTRYPOINT ["java","-jar","/app/app.jar"]
