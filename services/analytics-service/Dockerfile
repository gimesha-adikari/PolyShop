# syntax=docker/dockerfile:1.6

FROM python:3.12-slim AS base

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

ENV ANALYTICS_DB_URL=""
ENV ANALYTICS_DB_USERNAME=""
ENV ANALYTICS_DB_PASSWORD=""
ENV KAFKA_BOOTSTRAP_SERVERS=""
ENV JWT_PUBLIC_KEY=""
ENV JWT_ISSUER="polyshop-auth"

EXPOSE 8088

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -fsS http://localhost:8088/health || exit 1

CMD ["uvicorn","app.main:app","--host","0.0.0.0","--port","8088"]
