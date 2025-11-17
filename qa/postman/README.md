# **PolyShop Postman Suite**

This folder contains the full Postman setup used for manual testing, local API validation, and automated Newman CI pipelines.

---

## **1. Files Included**

| File                                                   | Description                                                            |
| ------------------------------------------------------ | ---------------------------------------------------------------------- |
| `PolyShop.postman_collection.json`                     | Full API collection (Auth, Products, Orders, Inventory, Payment, etc.) |
| `PolyShop.local.postman_environment.json`              | Local environment variables                                            |
| *(future)* `PolyShop.staging.postman_environment.json` | Staging environment                                                    |
| *(future)* `PolyShop.prod.postman_environment.json`    | Production environment                                                 |

---

## **2. Required Tools**

Install Postman Desktop App:

[https://www.postman.com/downloads/](https://www.postman.com/downloads/)

For CLI automation:

```
npm install -g newman
```

---

## **3. Environment Variables**

The environment file defines:

```
auth_service = http://localhost:8081
product_service = http://localhost:8082
inventory_service = http://localhost:8083
order_service = http://localhost:8084
payment_service = http://localhost:8085
notification_service = http://localhost:8086
search_service = http://localhost:8087
analytics_service = http://localhost:8088

gateway = http://localhost:8080

access_token = (dynamic)
refresh_token = (dynamic)
productId = (dynamic)
orderId = (dynamic)
```

Postman automatically updates:

* `access_token` after login
* `refresh_token` after login
* IDs after created resources (if scripts added)

---

## **4. How to Use the Collection**

### **4.1 Import the collection**

1. Open Postman
2. Click **Import**
3. Select:

    * `PolyShop.postman_collection.json`
    * `PolyShop.local.postman_environment.json`
4. Activate the environment (top right)

---

## **5. Running Tests**

### **5.1 Run a single request**

Choose any request (e.g., Auth → Login)
Press **Send**

### **5.2 Run all requests (Collection Runner)**

1. Click the **Collection Runner**
2. Choose the *PolyShop Full API* collection
3. Select the environment
4. Run all

---

## **6. Writing Tests in Postman**

Example test script:

```js
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response has id", function () {
    var body = pm.response.json();
    pm.expect(body.id).to.exist;
});
```

You can add these scripts under the **Tests** tab of each request.

---

## **7. Automated CLI Tests (Newman)**

Run all tests from CLI:

```
newman run PolyShop.postman_collection.json \
  -e PolyShop.local.postman_environment.json
```

Run with HTML report:

```
newman run PolyShop.postman_collection.json \
  -e PolyShop.local.postman_environment.json \
  -r htmlextra
```

---

## **8. Folder Structure Best Practices**

```
postman/
│
├── PolyShop.postman_collection.json
├── PolyShop.local.postman_environment.json
└── README.md    ← THIS FILE
```

---

## **9. Next Steps**

To prepare QA for CI/CD:

* Add more environment files (staging, prod)
* Attach global test scripts
* Integrate Newman reports in GitHub Actions
