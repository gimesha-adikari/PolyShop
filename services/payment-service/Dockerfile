# syntax=docker/dockerfile:1.6

# Build stage
FROM node:20-alpine AS builder

WORKDIR /usr/src/app

COPY package*.json ./
RUN npm ci

COPY . .

# If you have a build step (TS, bundling), enable:
# RUN npm run build

# Runtime stage
FROM node:20-alpine

WORKDIR /usr/src/app

# curl for healthcheck
RUN apk add --no-cache curl

ENV NODE_ENV=production

# Payment envs (injected by deployment)
ENV PAYMENT_DB_URL=""
ENV PAYMENT_DB_USERNAME=""
ENV PAYMENT_DB_PASSWORD=""
ENV STRIPE_SECRET_KEY=""
ENV PAYPAL_CLIENT_ID=""
ENV PAYPAL_CLIENT_SECRET=""
ENV WEBHOOK_SECRET=""
ENV JWT_PUBLIC_KEY=""
ENV JWT_ISSUER="polyshop-auth"

COPY --from=builder /usr/src/app/node_modules ./node_modules
COPY --from=builder /usr/src/app ./

EXPOSE 8085

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -fsS http://localhost:8085/health || exit 1

CMD ["npm","run","start"]
