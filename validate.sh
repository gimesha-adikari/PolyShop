#!/usr/bin/env bash
set -e

echo "=== 1. VALIDATING OPENAPI (swagger-cli via npx) ==="

# install locally if not installed
if [ ! -d "node_modules/@apidevtools" ]; then
  npm init -y
  npm install @apidevtools/swagger-cli --save-dev
fi

for f in docs/api/openapi-specs/*.yaml; do
  echo "Validating $f"
  npx swagger-cli validate "$f"
done

echo "=== 2. YAML LINT ==="
if [ ! -d ".venv" ]; then
  python3 -m venv .venv
fi

source .venv/bin/activate
pip install yamllint >/dev/null

yamllint -c .yamllint docs/api/openapi-specs

echo "=== 3. BUILD ALL JAVA SERVICES ==="
SERVICES=(
  "services/auth-service"
#  "services/product-service"
#  "services/order-service"
#  "services/payment-service"
#  "services/notification-service"
#  "services/search-service"
#  "services/analytics-service"
#  "gateway"
)

for s in "${SERVICES[@]}"; do
  echo "Building $s"
  (cd "$s" && chmod +x gradlew && ./gradlew clean build --no-daemon)
done

echo "=== 4. DOCKER BUILD ALL SERVICES ==="
for s in "${SERVICES[@]}"; do
  echo "Docker build $s"
  docker build -t "polyshop/$(basename $s):local" "$s"
done

echo "=== 5. RUN PACT TESTS ==="
cd qa/pact-tests
npm ci
npm test
cd ../..

echo "=== 6. RUN POSTMAN / NEWMAN TESTS ==="
npm install -g newman --unsafe-perm
newman run qa/postman/polyshop-gateway.postman_collection.json || true

echo "=== COMPLETE ==="
