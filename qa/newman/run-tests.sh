#!/bin/bash
set -e

COLLECTION="./postman/PolyShop.postman_collection.json"
ENV="./postman/PolyShop.local.postman_environment.json"
REPORT_DIR="./newman/reports"

mkdir -p "$REPORT_DIR"

echo "Running Newman test suite..."
echo "Collection: $COLLECTION"
echo "Environment: $ENV"

newman run "$COLLECTION" \
  --environment "$ENV" \
  --reporters cli,json,html \
  --reporter-json-export "$REPORT_DIR/report.json" \
  --reporter-html-export "$REPORT_DIR/report.html"

echo "Newman tests completed."
echo "Reports saved to $REPORT_DIR/"
