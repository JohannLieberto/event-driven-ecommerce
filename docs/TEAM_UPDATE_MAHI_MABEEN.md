# Team Update — Mahi & Mabeen AI Assistants
**Last Updated:** 12 March 2026, 13:45 GMT  
**Project:** Event-Driven E-Commerce Microservices Platform  
**Repository:** https://github.com/JohannLieberto/event-driven-ecommerce  
**Active Branch:** `develop`

---

## 1. WHO IS WHO — TEAM ROLES

| Person | Role | Primary Responsibilities |
|---|---|---|
| **Johann (Hitesh)** | DevOps / Infrastructure Lead | Jenkins CI/CD, SonarQube, Order Service, Docker, repo owner |
| **Mahi** | Infrastructure & Security Lead | Eureka Server, Config Server, API Gateway, JWT Auth, Kubernetes, poster creation |
| **Mabeen** | Inventory Service & CI/CD Lead | Inventory Service, Stock management, Jenkins pipeline design, Docker containerization, Kubernetes |

---

## 2. PROJECT OVERVIEW

An **event-driven microservices e-commerce platform** built with Spring Boot 3.2.0 and Java 17.

### Architecture
```
Client → API Gateway (8080)
           ├── Order Service (8081) ←→ Order DB PostgreSQL (5432)
           └── Inventory Service (8082) ←→ Inventory DB PostgreSQL (5433)

Eureka Server (8761)    — all services register here
Config Server (8888)    — centralized config for all services
Jenkins (8090)          — CI/CD pipeline
SonarQube (9000)        — code quality + coverage
```

### Tech Stack
| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.0 |
| Build | Maven (multi-module) |
| Service Discovery | Spring Cloud Netflix Eureka |
| API Gateway | Spring Cloud Gateway |
| Config | Spring Cloud Config |
| Database | PostgreSQL (separate instance per service) |
| CI/CD | Jenkins |
| Code Quality | SonarQube |
| Test Coverage | JaCoCo |
| Testing | JUnit 5, Mockito, Spring Boot Test |
| Containers | Docker, Docker Compose |

---

## 3. CURRENT PROJECT STATUS (as of 12 March 2026)

### ✅ Sprint 1 — COMPLETE
- All 5 microservices built and working: Eureka, Config Server, API Gateway, Order Service, Inventory Service
- All 45 SonarQube issues resolved (0 blockers, 0 critical, 0 bugs)
- Test coverage: **~90% on Order Service and Inventory Service**
- Jenkins CI/CD pipeline working end-to-end
- Docker Compose stack for all services
- PR #13 merged to `develop` (fix: resolve all 45 SonarQube issues)

### What Was Fixed Today (12 March 2026)
1. **All 45 SonarQube issues resolved** — blocker empty test methods, critical duplicated strings, bug NPE risks
2. **JaCoCo coverage exclusions added** to root `pom.xml` — DTOs, entities, Application classes are now excluded so coverage % reflects actual business logic
3. **Jenkinsfile fixed** — runs `mvn verify sonar:sonar` in a single Maven command so JaCoCo reports are available to SonarQube
4. **Docker Compose CI isolation** — CI uses non-conflicting ports (5434/5435) so pipeline containers don't clash with local dev containers

### Recent Commits on `develop`
- `5466301` — fix: exclude DTOs, entities, config/eureka/gateway from JaCoCo coverage
- `8d33bc4` — Merge PR #13 — fix: Resolve all 45 SonarQube issues
- `ba6849d` — docs: add Sprint 1 poster content
- `3d96a4d` — fix: use non-conflicting host ports for CI DB containers (5434/5435)

---

## 4. MODULE STRUCTURE (Repo Layout)

```
event-driven-ecommerce/
├── pom.xml                        ← Parent POM, JaCoCo plugin, exclusions configured
├── docker-compose.yml             ← Full local stack (DBs, Jenkins, SonarQube)
├── Jenkinsfile                    ← CI/CD pipeline definition
├── eureka-server/                 ← Service discovery (port 8761)
├── config-server/                 ← Centralized config (port 8888)
├── api-gateway/                   ← Entry point, JWT auth, routing (port 8080)
│   └── src/main/java/.../
│       ├── security/
│       │   ├── JwtTokenProvider.java
│       │   ├── JwtAuthenticationFilter.java
│       │   └── SecurityConfig.java
│       ├── exception/GlobalErrorHandler.java
│       └── config/CorsConfig.java
├── order-service/                 ← Order management (port 8081)
│   └── src/main/java/.../
│       ├── controller/OrderController.java
│       ├── service/OrderService.java
│       ├── client/InventoryClient.java  ← Feign client calls inventory-service
│       ├── entity/Order.java, OrderItem.java
│       ├── dto/OrderRequest.java, OrderResponse.java, etc.
│       └── exception/GlobalExceptionHandler.java
└── inventory-service/             ← Inventory & stock management (port 8082)
    └── src/main/java/.../
        ├── controller/InventoryController.java
        ├── service/InventoryService.java
        ├── entity/Product.java, StockChangeLog.java
        ├── dto/ProductRequest.java, ProductResponse.java,
        │   StockCheckResponse.java, StockReservationRequest.java,
        │   BulkUpdateRequest.java, BulkUpdateResponse.java
        └── exception/InventoryExceptionHandler.java
```

---

## 5. KEY ENDPOINTS (all go through API Gateway on port 8080)

### Authentication
```
POST /auth/login
Body: { "username": "customer1", "password": "pass123" }
Response: { "token": "eyJ...", "username": "customer1", "expiresIn": 86400 }

Test users: customer1/pass123, admin/admin123
```

### Order Service
```
POST   /api/orders                          — Create order (reserves stock automatically)
GET    /api/orders/{id}                     — Get order by ID
GET    /api/orders/customer/{customerId}    — Get all orders for a customer
```

### Inventory Service
```
GET    /api/inventory/products              — Get all products (paginated)
POST   /api/inventory/products              — Create product
GET    /api/inventory/products/{id}         — Get product by ID
PUT    /api/inventory/products/{id}         — Update product
DELETE /api/inventory/products/{id}         — Delete product
GET    /api/inventory/check-stock?productId=X&quantity=Y  — Check stock
POST   /api/inventory/reserve              — Reserve stock
POST   /api/inventory/release              — Release stock
POST   /api/inventory/bulk-update          — Bulk update stock quantities
```

### Service Health & Discovery
```
GET http://localhost:8761         — Eureka dashboard (all registered services)
GET http://localhost:8888/order-service/default   — Config Server (order service config)
GET http://localhost:8081/actuator/health          — Order Service health
GET http://localhost:8082/actuator/health          — Inventory Service health
```

---

## 6. HOW SERVICES COMMUNICATE

- **Order Service → Inventory Service**: via `InventoryClient.java` (Feign Client, synchronous REST)
- When a new order is placed:
  1. Order Service calls `GET /api/inventory/check-stock` — checks if stock is available
  2. If sufficient → calls `POST /api/inventory/reserve` — reduces stock
  3. Order status set to `CONFIRMED`
  4. If insufficient → throws `InsufficientStockException` → HTTP 409 Conflict returned
- **All services register with Eureka** on startup
- **Load balancing** done automatically by Feign + Eureka (round-robin)
- **Config** fetched from Config Server (port 8888) at startup via `bootstrap.yml`

---

## 7. SONARQUBE QUALITY STATUS

| Metric | Status |
|---|---|
| Bugs | 0 |
| Blockers | 0 |
| Critical Issues | 1 (OPEN — empty constructor on `StockCheckResponse.java` in order-service) |
| Test Coverage | ~90% (business logic only, DTOs/entities excluded) |
| Quality Gate | PASSING |

### JaCoCo Exclusions (set in root `pom.xml` today)
These are excluded from coverage calculation — they are data-only classes with no logic:
- `**/dto/**`
- `**/entity/**`
- `**/model/**`
- `**/*Application.class`

---

## 8. SPRINT 2 — NEXT TASKS

### Mahi's Sprint 2 Responsibilities
| User Story | Task | Priority |
|---|---|---|
| US16 | Add Request Validation Middleware (logging filter, correlation ID) | High |
| US17 | Implement Monitoring Stack (Prometheus + Grafana dashboards) | High |
| US22 | API Documentation with Swagger/OpenAPI | Medium |
| US23 | Contribute to Integration Testing Suite | Medium |
| US24 | Create Comprehensive Postman Collection | Medium |

### Mabeen's Sprint 2 Responsibilities
| User Story | Task | Priority |
|---|---|---|
| US18 | Comprehensive Jenkins Pipeline (multi-stage, SonarQube quality gates, coverage) | **Primary** |
| US19 | Docker Containerization (Dockerfiles, multi-stage builds, Docker Hub push) | **Primary** |
| US20 | Kubernetes Deployment (manifests, deployments, services, probes) | High |
| US21 | Helm Charts (dev/staging/prod values files) | High |
| US17 | Monitoring Stack support (Prometheus metrics) | Medium |
| US23 | Integration Testing support | Medium |

---

## 9. IMPORTANT CONTEXT FOR AI ASSISTANTS

### Exception Handling Pattern
All services follow this pattern — DO NOT change it:
```java
// Constants at top of exception handler class
private static final String FIELD_TIMESTAMP = "timestamp";
private static final String FIELD_STATUS = "status";
private static final String FIELD_ERROR = "error";
private static final String FIELD_MESSAGE = "message";

// Always return ErrorResponse object with: timestamp, status, message, fieldErrors
```

### String Constants Pattern
Extract ALL repeated string literals into constants. SonarQube rule S1192 flags duplicates ≥ 3 times.

### Test Naming Convention
```
methodName_condition_expectedResult
// Example: reserveStock_insufficientStock_throwsException
```

### Null Safety Pattern
Always null-check before dereferencing (SonarQube rule S2259):
```java
// WRONG:
response.getStatusCode().value() // NPE risk

// CORRECT:
HttpStatus status = response.getStatusCode() != null
    ? HttpStatus.resolve(response.getStatusCode().value())
    : HttpStatus.INTERNAL_SERVER_ERROR;
```

### Java Version Notes
- Use `.toList()` NOT `Collectors.toList()` for streams (Java 16+)
- EXCEPTION: When the list is passed to Hibernate/JPA, use `new ArrayList<>()` instead of `.toList()` (immutable list causes `UnsupportedOperationException`)
- Use `var` where appropriate

---

---

# 🖼️ SECTION FOR MAHI — POSTER GENERATION GUIDE

## Everything You Need to Build, Run, and Screenshot the Pipeline for the Poster

### What the Poster Needs to Show
1. **Eureka Server dashboard** — all 4 services registered
2. **Jenkins pipeline** — successful build with all stages green
3. **SonarQube dashboard** — 0 issues, coverage shown
4. **API Gateway working** — successful order flow via curl
5. **Architecture diagram** — (you can draw this or screenshot from README)

---

### STEP 1 — Prerequisites (Install these if not already installed)

```bash
# Check you have these:
java --version        # Must be Java 17+
mvn --version         # Must be Maven 3.8+
docker --version      # Must be Docker 20+
docker compose version  # Must be v2 (note: no hyphen)
git --version
```

If Java 17 not installed (Mac):
```bash
brew install openjdk@17
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH="$JAVA_HOME/bin:$PATH"
```

If Java 17 not installed (Ubuntu/Debian):
```bash
sudo apt update
sudo apt install openjdk-17-jdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
```

---

### STEP 2 — Clone and Sync the Repo

```bash
git clone https://github.com/JohannLieberto/event-driven-ecommerce.git
cd event-driven-ecommerce
git checkout develop
git pull origin develop
```

You should be on the `develop` branch with all Sprint 1 code.

---

### STEP 3 — Start All Infrastructure with Docker Compose

This starts the two PostgreSQL databases, Jenkins, and SonarQube:

```bash
docker compose up -d
```

Verify containers are running:
```bash
docker compose ps
```

You should see these containers running:
- `event-driven-ecommerce-order-service-db-1` (PostgreSQL, port 5432)
- `event-driven-ecommerce-inventory-service-db-1` (PostgreSQL, port 5433)
- `event-driven-ecommerce-jenkins-1` (Jenkins, port 8090)
- `event-driven-ecommerce-sonarqube-1` (SonarQube, port 9000)

Wait ~60 seconds for SonarQube to fully start, then check:
```bash
curl http://localhost:9000/api/system/status
# Should return: {"status":"UP",...}
```

---

### STEP 4 — Build All Services

```bash
# From the repo root:
mvn clean package -DskipTests
```

This compiles all 5 modules. You should see `BUILD SUCCESS` for each.

---

### STEP 5 — Start Services in the CORRECT ORDER

Open **5 separate terminal windows** and run one command in each. Wait for each service to say `Started ... in X seconds` before starting the next.

**Terminal 1 — Eureka Server (start first, everything depends on this)**
```bash
cd event-driven-ecommerce
mvn -pl eureka-server spring-boot:run
# Wait until you see: Started EurekaServerApplication in X.X seconds
# Then open http://localhost:8761 — should see Eureka dashboard
```

**Terminal 2 — Config Server (start second)**
```bash
cd event-driven-ecommerce
mvn -pl config-server spring-boot:run
# Wait until you see: Started ConfigServerApplication in X.X seconds
```

**Terminal 3 — Order Service**
```bash
cd event-driven-ecommerce
mvn -pl order-service spring-boot:run
# Wait until you see: Started OrderServiceApplication in X.X seconds
```

**Terminal 4 — Inventory Service**
```bash
cd event-driven-ecommerce
mvn -pl inventory-service spring-boot:run
# Wait until you see: Started InventoryServiceApplication in X.X seconds
```

**Terminal 5 — API Gateway (start last)**
```bash
cd event-driven-ecommerce
mvn -pl api-gateway spring-boot:run
# Wait until you see: Started ApiGatewayApplication in X.X seconds
```

---

### STEP 6 — 📸 SCREENSHOT 1: Eureka Dashboard

Open your browser and go to: **http://localhost:8761**

You should see the Eureka dashboard with these 4 services registered under "Instances currently registered with Eureka":
- `API-GATEWAY`
- `ORDER-SERVICE`
- `INVENTORY-SERVICE`
- `CONFIG-SERVER`

**Take a full-page screenshot of this.** This shows service discovery working.

> ⚠️ If services don't appear, wait 30 more seconds and refresh — Eureka heartbeat takes ~30 seconds.

---

### STEP 7 — Load Sample Data (Inventory Products)

Open a new terminal and run these curl commands to create 3 products:

```bash
# Create Product 1 — Laptop
curl -X POST http://localhost:8082/api/inventory/products \
  -H "Content-Type: application/json" \
  -d '{"name": "Laptop Pro 15", "description": "High-performance laptop", "price": 1299.99, "stockQuantity": 50}'

# Create Product 2 — Wireless Mouse
curl -X POST http://localhost:8082/api/inventory/products \
  -H "Content-Type: application/json" \
  -d '{"name": "Wireless Mouse", "description": "Ergonomic wireless mouse", "price": 29.99, "stockQuantity": 200}'

# Create Product 3 — Mechanical Keyboard
curl -X POST http://localhost:8082/api/inventory/products \
  -H "Content-Type: application/json" \
  -d '{"name": "Mechanical Keyboard", "description": "RGB mechanical keyboard", "price": 89.99, "stockQuantity": 150}'
```

Verify products were created:
```bash
curl http://localhost:8082/api/inventory/products
# Should return a JSON list with 3 products, each with an "id" field
# Note down the ID of Laptop (probably id: 1)
```

---

### STEP 8 — Test Complete Order Flow Through Gateway

```bash
# Step 8a — Login and get JWT token
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "customer1", "password": "pass123"}'

# Copy the "token" value from the response
# Example response: {"token":"eyJhbGciOiJIUzUxMiJ9...", "username":"customer1", "expiresIn":86400}
```

Set the token as a variable (replace with your actual token):
```bash
TOKEN="eyJhbGciOiJIUzUxMiJ9...paste-your-full-token-here"
```

```bash
# Step 8b — Check stock availability
curl "http://localhost:8080/api/inventory/check-stock?productId=1&quantity=2" \
  -H "Authorization: Bearer $TOKEN"
# Expected: {"productId":1, "availableStock":50, "sufficient":true}

# Step 8c — Place an order (this automatically reserves stock)
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"customerId": 1, "items": [{"productId": 1, "quantity": 2}]}'
# Expected: order with status "CONFIRMED", HTTP 201 Created
# Note down the "id" from the response (e.g. "id": 1)

# Step 8d — Verify stock decreased
curl http://localhost:8080/api/inventory/products/1 \
  -H "Authorization: Bearer $TOKEN"
# Expected: stockQuantity should now be 48 (was 50, ordered 2)

# Step 8e — Test insufficient stock (this should fail with 409)
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"customerId": 1, "items": [{"productId": 1, "quantity": 9999}]}'
# Expected: HTTP 409 Conflict — {"message": "Insufficient stock..."}
```

---

### STEP 9 — 📸 SCREENSHOT 2: Jenkins Pipeline

**Setup Jenkins (first time only):**

1. Open http://localhost:8090
2. Get the initial admin password:
   ```bash
   docker exec $(docker ps -qf "name=jenkins") cat /var/jenkins_home/secrets/initialAdminPassword
   ```
3. Paste the password, click Install suggested plugins, create admin user
4. Install extra plugins:
   - Go to **Manage Jenkins → Plugins → Available**
   - Search and install: **SonarQube Scanner**, **JaCoCo**, **Docker Pipeline**
   - Restart Jenkins after install
5. Configure SonarQube in Jenkins:
   - Go to **Manage Jenkins → System**
   - Scroll to **SonarQube servers** section
   - Add server: Name = `SonarQube`, URL = `http://sonarqube:9000`
   - Generate a token in SonarQube: http://localhost:9000 → My Account → Security → Generate Token
   - Add the token in Jenkins as a credential (Secret text, ID = `sonarqube-token`)
6. Create pipeline job:
   - Click **New Item** → name it `event-driven-ecommerce` → select **Pipeline** → OK
   - Under **Pipeline**: select **Pipeline script from SCM**
   - SCM: **Git**, URL: `https://github.com/JohannLieberto/event-driven-ecommerce.git`
   - Branch: `*/develop`
   - Script Path: `Jenkinsfile`
   - Save

**Run the pipeline:**

1. Click **Build Now**
2. Watch the stages execute: Checkout → Build → Test/Coverage → Package → SonarQube Analysis → Quality Gate → Docker Build → Deploy
3. Wait for it to finish (takes ~3-5 minutes)

**📸 Take a screenshot** of the pipeline showing all stages green (Blue Ocean view is better — click "Open Blue Ocean" in the left sidebar).

---

### STEP 10 — 📸 SCREENSHOT 3: SonarQube Dashboard

1. Open http://localhost:9000
2. Login: admin / admin (or whatever you set)
3. Go to **Projects** — you should see `event-driven-ecommerce`
4. Click on the project
5. You should see:
   - **0 Bugs**
   - **0 Vulnerabilities**
   - **0 Security Hotspots**
   - **Coverage: ~90%** (or higher after today's JaCoCo exclusion fix)
   - **Quality Gate: PASSED** (green)

**📸 Take a full screenshot of the SonarQube project overview page.**

> ⚠️ SonarQube only has data after a Jenkins pipeline run that includes the SonarQube Analysis stage. Run the Jenkins pipeline first (Step 9) before checking SonarQube.

---

### STEP 11 — Run Tests and Generate Coverage Report Locally

If you want a local coverage report without Jenkins:

```bash
# From repo root — runs all tests + generates JaCoCo HTML report
mvn clean verify

# View coverage report for Order Service:
open order-service/target/site/jacoco/index.html

# View coverage report for Inventory Service:
open inventory-service/target/site/jacoco/index.html
```

**📸 Screenshot the JaCoCo HTML report** showing line/branch coverage percentages.

---

### STEP 12 — Smoke Test Checklist Before Poster Screenshots

Run through this checklist to confirm everything is working:

- [ ] `http://localhost:8761` — Eureka shows 4 services registered
- [ ] `curl http://localhost:8081/actuator/health` → `{"status":"UP"}`
- [ ] `curl http://localhost:8082/actuator/health` → `{"status":"UP"}`
- [ ] Login via gateway returns a JWT token
- [ ] `GET /api/inventory/products` returns product list
- [ ] `POST /api/orders` with valid stock returns HTTP 201 + `"status": "CONFIRMED"`
- [ ] `POST /api/orders` with quantity 9999 returns HTTP 409
- [ ] `mvn clean verify` passes with 0 test failures
- [ ] Jenkins pipeline all stages green
- [ ] SonarQube shows 0 blockers/bugs, Quality Gate PASSED

---

### STEP 13 — Shutdown Gracefully

```bash
# Stop Spring Boot services: Ctrl+C in each terminal (Terminal 1-5)

# Stop Docker containers:
docker compose down

# To also remove volumes (wipe all data):
docker compose down -v
```

---

### Troubleshooting for Mahi

**Service doesn't appear in Eureka:**
```bash
# Check application.yml has this:
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka
    register-with-eureka: true
    fetch-registry: true
```

**Port already in use:**
```bash
# Find what's using the port (e.g. 8081):
lsof -i :8081
# Kill it:
kill -9 <PID>
```

**Database connection refused:**
```bash
# Make sure Docker containers are running:
docker compose ps
# If not running:
docker compose up -d
# Check logs:
docker compose logs order-service-db
```

**Maven tests fail but IntelliJ passes:**
```bash
mvn clean
mvn install -DskipTests
mvn test
```

**SonarQube token error (401 Unauthorized):**
```bash
# Generate new token at http://localhost:9000 → My Account → Security
# Use token in command:
mvn sonar:sonar -Dsonar.login=YOUR_NEW_TOKEN
```

**Jenkins can't find Docker:**
```bash
# Jenkins runs in Docker. Make sure docker-compose.yml mounts the Docker socket:
# volumes:
#   - /var/run/docker.sock:/var/run/docker.sock
# This is already configured in the project's docker-compose.yml
```

---

## 10. CONTEXT PROMPTS FOR AI ASSISTANTS

Use this prompt to bring your AI assistant up to speed instantly:

```
I'm working on an event-driven microservices e-commerce platform.

Tech: Spring Boot 3.2.0, Java 17, Maven multi-module, PostgreSQL

Modules:
- eureka-server (8761) — service discovery
- config-server (8888) — centralized config
- api-gateway (8080) — JWT auth, routing
- order-service (8081) — order management, calls inventory via Feign
- inventory-service (8082) — product CRUD, stock reserve/release/bulk-update

Status:
- Sprint 1 complete — all services working, 0 SonarQube issues, ~90% test coverage
- Jenkins CI/CD pipeline working
- JaCoCo exclusions set for DTOs/entities/Application classes

Current branch: develop
Repo: https://github.com/JohannLieberto/event-driven-ecommerce

Code style rules:
- Extract repeated strings into private static final String constants
- Always null-check before dereferencing (S2259)
- Use .toList() for streams EXCEPT when passing to JPA (use new ArrayList<>() there)
- Test naming: methodName_condition_expectedResult
- Target 90% coverage, exclude DTOs/entities

What would you like help with?
```

---

*Document generated: 12 March 2026 | Repo: https://github.com/JohannLieberto/event-driven-ecommerce | Branch: develop*
