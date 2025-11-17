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

ENV INVENTORY_DB_URL=""
ENV INVENTORY_DB_USERNAME=""
ENV INVENTORY_DB_PASSWORD=""
ENV INVENTORY_RESERVATION_TTL=300
ENV INVENTORY_KAFKA_TOPIC_RESERVATIONS="inventory.reservations"
ENV KAFKA_BOOTSTRAP_SERVERS=""
ENV JWT_PUBLIC_KEY=""
ENV JWT_ISSUER="polyshop-auth"

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8083

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -fsS http://localhost:8083/actuator/health || exit 1

ENTRYPOINT ["java","-jar","/app/app.jar"]
