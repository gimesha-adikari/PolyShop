# **qa/README.md**

```
# PolyShop QA Suite

The QA suite contains all automated testing assets for the PolyShop microservices
platform. It includes:

- Postman API tests  
- Newman CLI runner for CI/CD  
- Pact contract tests (TypeScript)  
- k6 performance tests (smoke, load, stress, spike, soak)  
- Environment configurations  
- Reporting tools  

This folder is structured as follows:

qa/
 ├── postman/
 │    ├── PolyShop.postman_collection.json
 │    ├── PolyShop.local.postman_environment.json
 │    └── README.md
 │
 ├── newman/
 │    ├── run-local.sh
 │    ├── run-ci.sh
 │    ├── reporters/
 │    │     ├── README.md
 │    │     └── custom-html-template.hbs
 │    └── README.md
 │
 ├── pact-tests/
 │    ├── package.json
 │    ├── tsconfig.json
 │    ├── jest.config.js
 │    ├── README.md
 │    ├── consumer/
 │    │     ├── auth-service.pact.test.ts
 │    │     ├── product-service.pact.test.ts
 │    │     └── order-service.pact.test.ts
 │    └── provider/
 │          ├── provider-setup.ts
 │          ├── provider-states.ts
 │          └── provider-verification.test.ts
 │
 ├── performance-tests/
 │    ├── smoke-test.js
 │    ├── load-test.js
 │    ├── stress-test.js
 │    ├── spike-test.js
 │    ├── soak-test.js
 │    └── README.md
 │
 └── README.md  (this file)

## Requirements

### Postman / Newman
- Postman Desktop  
- Node.js ≥ 18  
- Newman:  
```

npm install -g newman newman-reporter-htmlextra

```

### Pact (contract tests)
- Node.js ≥ 18  
- TypeScript  
- Jest  
- @pact-foundation/pact

### k6 (performance tests)
Install from:  
https://k6.io/docs/get-started/installation/

---

## 1. Running Postman Tests

### Local environment
```

newman run postman/PolyShop.postman_collection.json
-e postman/PolyShop.local.postman_environment.json
--reporters cli,htmlextra
--reporter-htmlextra-export reports/postman-report.html

```

### CI mode
Use:
```

bash newman/run-ci.sh

```

---

## 2. Running Pact Contract Tests

Install dependencies:
```

cd pact-tests
npm install
npm test

```

---

## 3. Running k6 Performance Tests

### Smoke Test
```

k6 run performance-tests/smoke-test.js

```

### Load Test
```

k6 run performance-tests/load-test.js

```

### Stress Test
```

k6 run performance-tests/stress-test.js

```

### Spike Test
```

k6 run performance-tests/spike-test.js

```

### Soak Test
```

k6 run performance-tests/soak-test.js

```

---

## Purpose

This QA suite ensures:

- Correctness (functional tests)  
- Reliability (performance tests)  
- Interoperability (contract tests)  
- Regression detection (CI newman runs)  

It is designed to grow with PolyShop as new microservices and APIs are added.
