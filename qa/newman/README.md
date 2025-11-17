# **PolyShop Newman Automated Test Runner**

This directory contains the automation setup for running the Postman API tests using **Newman**.
It is used for CI/CD pipelines (GitHub Actions, GitLab CI, Jenkins, etc.) to perform backend regression testing.

---

## **1. Prerequisites**

Install Node.js (v18+ recommended).
Then install Newman globally:

```
npm install -g newman
```

Optional HTML report:

```
npm install -g newman-reporter-htmlextra
```

---

## **2. Files Included**

| File           | Description                                  |
| -------------- | -------------------------------------------- |
| `run-tests.sh` | Shell script to execute all Newman test runs |
| `README.md`    | Documentation file (this file)               |

---

## **3. Environment & Collection Files**

Newman expects Postman assets from `postman/`:

```
postman/
  PolyShop.postman_collection.json
  PolyShop.local.postman_environment.json
```

Make sure these files exist before running tests.

---

## **4. Running Tests (Local)**

Run all Newman tests:

```
./run-tests.sh
```

Or manually:

```
newman run ../postman/PolyShop.postman_collection.json \
  -e ../postman/PolyShop.local.postman_environment.json
```

With extended HTML reporting:

```
newman run ../postman/PolyShop.postman_collection.json \
  -e ../postman/PolyShop.local.postman_environment.json \
  -r htmlextra --reporter-htmlextra-export reports/report.html
```

---

## **5. Using Newman in CI/CD**

### **5.1 GitHub Actions Example**

```yaml
name: API Tests

on:
  push:
  pull_request:

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Install Node
        uses: actions/setup-node@v4
        with:
          node-version: 18

      - name: Install Newman
        run: npm install -g newman newman-reporter-htmlextra

      - name: Run Tests
        run: |
          cd newman
          ./run-tests.sh
```

### **5.2 GitLab CI Example**

```yaml
stages:
  - test

api_tests:
  stage: test
  script:
    - npm install -g newman
    - newman run postman/PolyShop.postman_collection.json -e postman/PolyShop.local.postman_environment.json
```

---

## **6. Folder Structure**

```
newman/
│
├── run-tests.sh
└── README.md
```

---

## **7. Purpose of Newman Suite**

Newman is used to:

* Validate API correctness on every commit
* Detect breaking changes across microservices
* Run automated smoke tests before deployment
* Produce HTML/JSON test reports
* Integrate with CI pipelines
* Ensure API contracts remain stable

---

## **8. Next Steps**

* Add multi-environment matrices (local, staging, prod)
* Add global Postman test scripts
* Export HTML & JSON reports to CI artifacts
