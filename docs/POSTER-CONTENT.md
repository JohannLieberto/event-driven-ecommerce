# Sprint 1 Poster — Event-Driven E-Commerce

---

## 🏷️ Project Title
**Event-Driven E-Commerce Order & Inventory Microservices System**

---

## 👥 Team
| Name | Role |
|---|---|
| Hitesh | DevOps, Infrastructure, Jenkins, Docker, SonarQube |
| Mahi | Backend Microservices, Order Service, Testing |
| Mabeen | Backend Microservices, Inventory Service, Testing |

---

## 🎯 Sprint 1 Goal
> "Establish the foundational microservices architecture with core order and inventory management capabilities, enabling customers to place orders with automatic stock validation through a secure, cloud-native infrastructure."

---

## 📋 User Story Status

| ID | User Story | Points | Status |
|---|---|---|---|
| US1 | Allow Customers to Place Orders | 8 | ✅ Done |
| US2 | Check Stock Availability When Creating Orders | 8 | ✅ Done |
| US3 | Validate Order Requests with Clear Error Messages | 5 | ✅ Done |
| US4 | Set Up Order Service Database | 3 | ✅ Done |
| US5 | Manage Product Catalog & Stock Levels | 8 | ✅ Done |
| US6 | Reserve & Release Stock Automatically | 8 | ✅ Done |
| US7 | Set Up Inventory Service Database | 3 | ✅ Done |
| US8 | Support Bulk Inventory Updates | 5 | ✅ Done |
| US9 | Enable Automatic Service Discovery (Eureka) | 5 | ✅ Done |
| US10 | Centralize Configuration Management | 5 | ✅ Done |
| US11 | Single Entry Point for All APIs (Gateway) | 5 | ✅ Done |
| US12 | Protect APIs with Authentication (JWT) | 8 | ⚠️ Partial |
| US13 | Support Multiple Service Instances Safely | 3 | ✅ Done |

**Planned: 74 pts | Completed: ~66 pts**

---

## 📉 Burndown Chart Data

| Sprint Day | Remaining Points |
|---|---|
| Day 1 | 74 |
| Day 3 | 58 |
| Day 5 | 42 |
| Day 7 | 27 |
| Day 10 | 8 |
| End | ~8 (US12 partial) |

---

## ✅ Definition of Done
- 80%+ code coverage (achieved **90%**)
- All unit tests passing
- All acceptance tests passing
- Two approvals required for merge requests
- Documentation completed per User Story
- Passed the pipeline and merged from feature branch into dev branch

---

## 🔬 SonarQube Analysis (Latest)

| Metric | Result |
|---|---|
| Bugs | 0 |
| Vulnerabilities | 0 |
| Code Smells | 0 |
| Coverage | 90% |
| Duplications | 0% |
| Quality Gate | ✅ PASSED |

> **45 issues resolved** (5 Blocker, 15 Critical, 18 Major, 7 Minor)

---

## 🧪 Test Metrics

| Test Level | Count |
|---|---|
| Unit Tests | ~70 |
| Integration Tests | In Progress |
| Line Coverage | 90% |
| Method Coverage | 100% |

**Test classes include:**
- OrderServiceTest, GlobalExceptionHandlerTest, OrderTest, OrderItemTest
- OrderRequestTest, OrderResponseTest, OrderNotFoundExceptionTest
- InsufficientStockExceptionTest, InventoryServiceExceptionTest
- InventoryServiceTest (13 tests), InventoryExceptionHandlerTest

---

## 🔁 Retrospective

### What Went Well
- Successfully delivered all core microservices (Order, Inventory, API Gateway, Eureka, Config Server)
- Resolved all 45 SonarQube issues (Blocker to Minor) within the sprint
- Achieved 90% test coverage, exceeding the 80% Definition of Done target
- CI/CD pipeline fully automated: Jenkins → Build → Test → SonarQube → Docker Deploy

### What To Improve
- JWT authentication (US12) was planned but only partially implemented — carry over to Sprint 2
- Integration tests between services need more coverage (currently unit-test focused)
- Port conflicts between manual dev stack and CI stack caused deployment delays

---

## 🖥️ Screenshots Needed for Poster

### 1. Jenkins Pipeline View
- Go to: http://localhost:8083
- Open your pipeline job → click the latest successful build
- Take a screenshot of the **Stage View** (the coloured pipeline stages grid)
- Should show: Checkout → Build → Test → SonarQube → Docker Deploy all in green ✅

### 2. SonarQube — Analysis (Current State)
- Go to: http://localhost:9000
- Click on the `order-service` or top-level project
- Take a screenshot of the **main dashboard** showing:
  - 0 Bugs, 0 Vulnerabilities, 0 Code Smells
  - 90% Coverage
  - Quality Gate: PASSED

### 3. SonarQube — History Graph
- On the same SonarQube project page, click **Activity** tab
- Take a screenshot showing the **coverage trend going up** over multiple builds
- This shows improvement over time — looks great on the poster!

### 4. Test Results / Coverage Report (optional)
- Go to: http://localhost:8083 → your build → **Coverage Report** (JaCoCo)
- Screenshot showing the 90% coverage breakdown per class

---

## 🏗️ Architecture (for poster diagram)

```
[Client]
   |
[API Gateway :8080]
   |          |
[Order    [Inventory
Service]   Service]
 :8081      :8082
   |          |
[Order DB] [Inventory DB]
 PostgreSQL  PostgreSQL

[Eureka Server :8761]  — Service Discovery
[Config Server :8888]  — Centralized Config
[Jenkins       :8083]  — CI/CD Pipeline
[SonarQube     :9000]  — Code Quality
```

---

*Last updated: March 12, 2026*
