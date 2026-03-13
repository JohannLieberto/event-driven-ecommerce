# Sprint 1 Demo Script

> **Verified against `develop` branch — all field types, endpoint paths and ports match the actual code.**  
> Last updated: 13 March 2026

---

## Table of Contents

1. [Service Map](#service-map)
2. [Before They Arrive — Setup](#before-they-arrive--setup)
3. [Section 1 — Context (2 min)](#section-1--context-2-min)
4. [Section 2 — Infrastructure (2 min)](#section-2--infrastructure-2-min)
5. [Section 3 — Live API Demo in Postman (6 min)](#section-3--live-api-demo-in-postman-6-min)
6. [Section 4 — CI/CD Pipeline (3 min)](#section-4--cicd-pipeline-3-min)
7. [Section 5 — Tests (1 min)](#section-5--tests-1-min)
8. [Sprint 2 Plan](#sprint-2-plan)
9. [Key Corrections from Previous Docs](#key-corrections-from-previous-docs)

---

## Service Map

| Service | Port | Status |
|---|---|---|
| API Gateway | `8080` | ✅ Sprint 1 Complete |
| Order Service | `8081` | ✅ Sprint 1 Complete |
| Inventory Service | `8082` | ✅ Sprint 1 Complete |
| Eureka Server | `8761` | ✅ Sprint 1 Complete |
| Config Server | `8888` | ✅ Sprint 1 Complete |
| Jenkins | `8083` | ✅ Sprint 1 Complete |
| SonarQube | `9000` | ✅ Sprint 1 Complete |
| Order DB (PostgreSQL) | `5432` | ✅ Sprint 1 Complete |
| Inventory DB (PostgreSQL) | `5433` | ✅ Sprint 1 Complete |

---

## Before They Arrive — Setup

Run these **5 minutes before** the demo starts:

```bash
# Start the full stack
docker-compose up -d

# Open browser tabs in advance
start http://localhost:8761    # Eureka Dashboard
start http://localhost:8083    # Jenkins
start http://localhost:9000    # SonarQube
```

Wait ~60 seconds until the Eureka dashboard shows all services green before beginning.

---

## Section 1 — Context (2 min)

**Who speaks:** Anyone

> *"We built an event-driven e-commerce microservices platform using Spring Boot 3.2 and Java 17. The domain is an online store — customers place orders, and the system automatically validates and reserves stock in real time. The two core business services are Order Service and Inventory Service, fronted by an API Gateway, with Eureka for service discovery and Config Server for centralised configuration."*

Point at the architecture diagram on the poster.

---

## Section 2 — Infrastructure (2 min)

**Open:** `http://localhost:8761`

> *"This is the Eureka dashboard. Every service registers itself automatically on startup — no hardcoded URLs anywhere. The API Gateway uses this registry for client-side load balancing. This is what makes the system cloud-native."*

✅ **Demonstrates:** US9 — Automatic Service Discovery

---

## Section 3 — Live API Demo in Postman (6 min)

> ⚠️ All IDs are `Long` (numeric). `customerId` is **not** a String — do not use `"CUST001"` etc.

---

### Step 1 — Create a Product

**Demonstrates:** US5 — Manage Product Catalog

```
POST http://localhost:8082/api/inventory
Content-Type: application/json

{
  "name": "MacBook Pro",
  "description": "Apple M3 laptop",
  "price": 1999.99,
  "stockQuantity": 10
}
```

**Expected Response:** `201 Created`

```json
{
  "id": 1,
  "name": "MacBook Pro",
  "description": "Apple M3 laptop",
  "price": 1999.99,
  "stockQuantity": 10
}
```

> *"Note the returned `id` — it's a Long. We'll use `1` for all subsequent calls. `price` is a BigDecimal, `stockQuantity` is an Integer."*

---

### Step 2 — Check Stock Availability

**Demonstrates:** US2 — Stock Check endpoint used internally by Order Service

```
GET http://localhost:8082/api/inventory/1/check?quantity=3
```

**Expected Response:** `200 OK` — sufficient stock confirmed

---

### Step 3 — List All Products (Paginated)

**Demonstrates:** US5 — Pagination support

```
GET http://localhost:8082/api/inventory?page=0&size=10
```

**Expected Response:** `200 OK` — paginated product list with metadata

> *"The list is paginated with Spring Data Pageable, built to handle large catalogues at scale."*

---

### Step 4 — Place an Order (via API Gateway)

**Demonstrates:** US1 — Place Orders + US2 — Stock Validation (Feign Client call)

```
POST http://localhost:8080/api/orders
Content-Type: application/json

{
  "customerId": 1,
  "items": [
    {
      "productId": 1,
      "quantity": 3
    }
  ]
}
```

**Expected Response:** `201 Created`

```json
{
  "id": 1,
  "customerId": 1,
  "status": "CONFIRMED",
  "items": [
    { "productId": 1, "quantity": 3 }
  ]
}
```

> *"We're hitting port 8080 — the API Gateway — which routes to Order Service on 8081. `customerId` is a Long. Before saving the order, Order Service calls Inventory Service via Feign Client to check and reserve stock. Both services are resolved by name through Eureka — no hardcoded URLs."*

---

### Step 5 — Verify Stock Was Reduced

**Demonstrates:** US6 — Automatic Stock Reservation

```
GET http://localhost:8082/api/inventory/1
```

**Expected Response:** `stockQuantity` dropped from `10` → `7`

> *"Stock reduced atomically. The Product entity uses `@Version` for optimistic locking — if two orders try to reserve simultaneously, only one wins; the other gets a clean 409 Conflict."*

---

### Step 6 — Retrieve the Order

```
GET http://localhost:8080/api/orders/1
```

**Expected Response:** `200 OK` with full order details

---

### Step 7 — Get All Orders for a Customer

```
GET http://localhost:8080/api/orders?customerId=1
```

**Expected Response:** `200 OK` — list of orders for customer ID `1` (Long query param)

---

### Step 8 — Insufficient Stock (Negative Test)

**Demonstrates:** US2 — Failure scenario

```
POST http://localhost:8080/api/orders
Content-Type: application/json

{
  "customerId": 1,
  "items": [
    {
      "productId": 1,
      "quantity": 999
    }
  ]
}
```

**Expected Response:** `400 Bad Request`

```json
{
  "status": 400,
  "error": "Insufficient stock for product 1"
}
```

> *"No order is created, no stock is modified."*

---

### Step 9 — Bean Validation Error (Negative Test)

**Demonstrates:** US3 — Validation with Clear Error Messages

```
POST http://localhost:8080/api/orders
Content-Type: application/json

{
  "items": [
    {
      "productId": 1,
      "quantity": -1
    }
  ]
}
```

**Expected Response:** `400 Bad Request` — two field-level errors:

```json
{
  "status": 400,
  "error": "Validation Failed",
  "errors": [
    { "field": "customerId", "message": "Customer ID is required" },
    { "field": "items[0].quantity", "message": "Quantity must be greater than 0" }
  ]
}
```

> *"`GlobalExceptionHandler` catches both validation failures and returns structured field-level messages — before anything touches the service layer."*

---

### Step 10 — Manual Stock Reserve / Release

**Demonstrates:** US6 — Reserve and Release Stock endpoints

**Reserve:**

```
PUT http://localhost:8082/api/inventory/1/reserve
Content-Type: application/json

{
  "quantity": 2,
  "orderId": 1
}
```

**Expected Response:** `200 OK` — updated product with reduced stock

**Release:**

```
PUT http://localhost:8082/api/inventory/1/release
Content-Type: application/json

{
  "quantity": 2,
  "orderId": 1
}
```

**Expected Response:** `200 OK` — stock returned to previous level

> *"`orderId` (Long) is stored in `StockChangeLog` for a full audit trail of every stock movement."*

---

## Section 4 — CI/CD Pipeline (3 min)

**Open:** `http://localhost:8083`

> *"Every push to the `develop` branch triggers the pipeline automatically via a GitHub webhook. Let me trigger one live."*

```bash
echo "# demo trigger" >> demo.txt
git add demo.txt && git commit -m "demo: trigger pipeline"
git push origin develop
```

Watch Jenkins pick up the build and walk through the **5 pipeline stages**:

| Stage | What Happens |
|---|---|
| **1. Checkout** | Pulls latest code from GitHub |
| **2. Build** | `mvn clean compile -DskipTests` |
| **3. Test & Coverage** | `mvn clean verify` — runs all tests, generates JaCoCo reports |
| **4. Package** | `mvn package -DskipTests` |
| **5. Code Quality** | SonarQube scan + Quality Gate enforcement |

> *"If the Quality Gate fails, the entire pipeline fails — enforced automatically on every commit."*

**Then open SonarQube** `http://localhost:9000`:

> *"Zero bugs, zero vulnerabilities. We resolved all 45 SonarQube issues before this demo — including two real NullPointerException risks in the exception handlers and 5 BLOCKER-level issues with empty test methods."*

---

## Section 5 — Tests (1 min)

> *"We have 31 tests total — 20 unit tests and 11 integration tests. The test pyramid sits unit-heavy as expected for Sprint 1. Unit tests use Mockito for mocking; integration tests use `@SpringBootTest` with an H2 in-memory database. End-to-end tests are planned for Sprint 2."*

| Layer | Count | Framework |
|---|---|---|
| Unit Tests | 20 | JUnit 5 + Mockito |
| Integration Tests | 11 | `@SpringBootTest` + H2 |
| E2E Tests | 0 | Planned — Sprint 2 |
| **Total** | **31** | |

---

## Sprint 2 Plan

Based on rubric gaps vs. Sprint 1 delivery:

| Rubric Requirement | Sprint 1 | Sprint 2 Plan |
|---|---|---|
| **Resilience4J** circuit breaker | ❌ Missing | `@CircuitBreaker` on `InventoryClient` — fallback if Inventory is down |
| **JWT Authentication** | ⚠️ Partial (Gateway config only) | Full `/auth/login` → JWT → validated on every request |
| **Observability / Tracing** | ❌ Missing | Micrometer + Zipkin distributed tracing |
| **E2E Tests** | ❌ Missing | Login → create order → verify stock reduction |
| **Deployment stage** | ✅ `docker-compose up` in pipeline | Keep as-is |

### Sprint 2 User Stories

- **US14** — As a customer, I want order placement to gracefully handle inventory service outages so the system degrades without crashing *(Resilience4J circuit breaker)*
- **US15** — As a user, I want to log in and receive a JWT token so all API calls are authenticated *(Full JWT flow)*
- **US16** — As an ops team, I want distributed traces across services so I can diagnose latency issues *(Zipkin / Micrometer)*

---

## Key Corrections from Previous Docs

> These were wrong in earlier handover documents — verified directly from source code on `develop`.

| What Was Written | What the Code Actually Has |
|---|---|
| `"customerId": "CUST001"` (String) | `"customerId": 1` — **Long** |
| `POST /api/inventory/products` | `POST /api/inventory` |
| `GET /api/inventory/products/{id}` | `GET /api/inventory/{id}` |
| `GET /api/inventory/check-stock?productId=...` | `GET /api/inventory/{productId}/check?quantity=...` |
| `POST /api/inventory/reserve` | `PUT /api/inventory/{productId}/reserve` |
| `POST /api/inventory/release` | `PUT /api/inventory/{productId}/release` |
