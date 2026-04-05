# AI Context File — Event-Driven Ecommerce
> For: Mahi & Mabeen  
> Last updated: 2026-04-05  
> Branch this was written on: `feature/batch3-event-driven-kafka`  
> Read this fully before touching any code or config.

---

## 1. What This Project Is

An event-driven microservices e-commerce platform built with:
- **Java 17 + Spring Boot 3.2**
- **Spring Cloud 2023.0.0** (Eureka, Config, Gateway)
- **Apache Kafka** (Confluent 7.5.0) for async event publishing
- **PostgreSQL 15** — one dedicated DB per service
- **Docker + Docker Compose** for local development
- **Jenkins** for CI/CD
- **Karate** for API/integration testing

All traffic enters through the **API Gateway on port 8080**, which routes to downstream microservices registered in **Eureka (port 8761)**.

---

## 2. Current Service Map

| Service | Internal Port | External Port (docker) | DB | Notes |
|---|---|---|---|---|
| `api-gateway` | 8080 | 8080 | — | Entry point for all requests |
| `order-service` | 8081 | 8081 | `orderdb` | ⚠️ Was broken — now fixed (see §4) |
| `inventory-service` | 8083 | 8083 | `inventorydb` | — |
| `payment-service` | 8084 | 8084 | `paymentdb` | — |
| `shipping-service` | 8085 | 8085 | `shippingdb` | — |
| `notification-service` | 8086 | 8086 | `notificationdb` | — |
| `eureka-server` | 8761 | 8761 | — | Service registry |
| `kafka` | 9092 | 9092 | — | Confluent CP 7.5.0 |
| `zookeeper` | 2181 | — | — | Required by Kafka |
| `kafka-ui` | 8080 (internal) | 9000 | — | Kafka topic/event viewer |
| `postgres` | 5432 | 5432 | all DBs | Single PG instance, multiple DBs |

> ⚠️ **Zipkin is NOT in docker-compose.** Do not try to access port 9411 — it will fail. If tracing is needed, add the zipkin service block manually (see §7).

---

## 3. Kafka Event Flow

```
Customer → API Gateway (8080)
    → Order Service (8081)
        → Publishes: order.created → Kafka topic
            → Inventory Service consumes → reserves stock
            → Payment Service consumes → processes payment
            → Shipping Service consumes → creates shipment
            → Notification Service consumes → sends notification
```

All events are published using `OrderEventPublisher` in order-service. Kafka topics auto-create (`KAFKA_AUTO_CREATE_TOPICS_ENABLE: true`).

---

## 4. Critical Fixes Already Made (DO NOT REVERT)

### Fix 1 — order-service port alignment
**Problem:** `application.properties` had `server.port=8082`, `application.yml` had `server.port=8081`, and docker-compose mapped `8081:8081`. This caused the service to start on 8082 internally while the host expected 8081 — resulting in `Empty reply from server`.

**Fix applied (commit `dfeb760`):**
- `order-service/src/main/resources/application.properties` → `server.port=8081` ✅
- `application.properties` is now the **single source of truth** for all Docker config

### Fix 2 — application.yml cleaned up
**Problem:** `application.yml` had duplicate `server.port`, `localhost` datasource URLs (wrong inside Docker), and localhost Eureka URLs — all of which would override or conflict with the correct docker-compose env vars.

**Fix applied (commit `dbff7c7`):**
- `order-service/src/main/resources/application.yml` stripped to only contain dev-only logging overrides
- All runtime config lives in `application.properties` and docker-compose `environment:` blocks

### Rule going forward:
> Never define `server.port`, `datasource.url`, `eureka.client.service-url`, or `kafka.bootstrap-servers` in `application.yml` for any service. These must only live in `application.properties` (with Docker-friendly hostnames like `postgres`, `kafka`, `eureka-server`) or be overridden via docker-compose `environment:` blocks.

---

## 5. How to Run Locally (Step by Step)

```bash
# 1. Clone and checkout develop (or the feature branch you're working on)
git clone https://github.com/JohannLieberto/event-driven-ecommerce.git
cd event-driven-ecommerce
git checkout develop
git pull

# 2. Build all modules (skip tests for speed)
mvn clean package -DskipTests

# 3. Start the full stack
docker-compose up --build

# 4. Watch logs for any service that fails to start
docker-compose logs -f order-service
docker-compose logs -f inventory-service
```

### Verify everything is up:

| Check | URL |
|---|---|
| Eureka dashboard (all services should show UP) | http://localhost:8761 |
| Kafka UI (browse topics and messages) | http://localhost:9000 |
| Order Service health | http://localhost:8081/actuator/health |
| Inventory Service health | http://localhost:8083/actuator/health |
| Payment Service health | http://localhost:8084/actuator/health |
| Shipping Service health | http://localhost:8085/actuator/health |
| Notification Service health | http://localhost:8086/actuator/health |
| API Gateway health | http://localhost:8080/actuator/health |

---

## 6. Correct API Request Format

### Place an Order (via API Gateway)
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "items": [
      { "productId": 1, "quantity": 2 },
      { "productId": 3, "quantity": 1 }
    ]
  }'
```

> ⚠️ `productId` and `quantity` MUST be inside the `items` array. Sending them at the top level returns `400 Bad Request: Order must contain at least one item`. This is correct validation behaviour — not a bug.

### Check an order directly on order-service:
```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8081/api/orders/{id}
```

---

## 7. Things That Do NOT Exist Yet (Don't Assume They Do)

- ❌ **Zipkin** — not in docker-compose. Port 9411 will fail. Add the service block if needed:
  ```yaml
  zipkin:
    image: openzipkin/zipkin
    container_name: zipkin
    ports:
      - "9411:9411"
    networks:
      - ecommerce-network
  ```
- ❌ **Config Server** — present as a module but NOT in docker-compose. `bootstrap.yml` in order-service points to `http://localhost:8888` but `fail-fast` must remain `false` (or config enabled=false) or the service will crash on startup.
- ❌ **JWT Auth** — referenced in README but verify it's actually wired in api-gateway before assuming it works.
- ❌ **feature/US17-karate-tests** branch — does not exist remotely. The Karate tests live on `feature/batch3-event-driven-kafka`. PR #21 merges this into `develop`.

---

## 8. Branch Strategy

| Branch | Purpose |
|---|---|
| `main` | Stable, production-ready only |
| `develop` | Integration branch — all features merge here via PR |
| `feature/US*` | One branch per user story |
| `feature/batch*` | Batch feature branches (multiple US in one) |

**Always branch off `develop`, never off `main` directly.**

```bash
git checkout develop
git pull
git checkout -b feature/US18-your-feature
```

**PR flow:** `feature/*` → `develop` → reviewed → merged → eventually `develop` → `main`.

Open PR #21 (`feature/batch3-event-driven-kafka` → `develop`) must be merged before starting new work that depends on the port fix or Karate tests.

---

## 9. Cloud Deployment Plan (AWS)

This is the agreed plan for deploying the platform to AWS. Do not deviate from this without team discussion.

### Target Architecture

```
Internet
   │
   ▼
[Route 53] → DNS
   │
   ▼
[ALB - Application Load Balancer]
   │
   ▼
[ECS Fargate Cluster]
 ├── api-gateway          (1 task, port 8080)
 ├── order-service        (1+ tasks, port 8081)
 ├── inventory-service    (1+ tasks, port 8083)
 ├── payment-service      (1+ tasks, port 8084)
 ├── shipping-service     (1+ tasks, port 8085)
 └── notification-service (1+ tasks, port 8086)
   │
   ├── [Amazon MSK] ← Managed Kafka (replaces local Confluent)
   ├── [RDS PostgreSQL] ← Managed DB (replaces local postgres container)
   └── [ECR] ← Docker image registry (replaces local builds)
```

### Step-by-Step Deployment Plan

#### Step 1 — Containerise & Push Images to ECR
```bash
# Authenticate Docker to ECR
aws ecr get-login-password --region eu-west-1 | docker login --username AWS \
  --password-stdin <account-id>.dkr.ecr.eu-west-1.amazonaws.com

# Build and tag each service
docker build -t order-service ./order-service
docker tag order-service:latest <account-id>.dkr.ecr.eu-west-1.amazonaws.com/order-service:latest
docker push <account-id>.dkr.ecr.eu-west-1.amazonaws.com/order-service:latest
# Repeat for all services
```

#### Step 2 — Provision RDS (PostgreSQL)
- Create one **RDS PostgreSQL 15** instance (or Aurora Serverless v2 for cost savings)
- Create separate databases: `orderdb`, `inventorydb`, `paymentdb`, `shippingdb`, `notificationdb`
- Place in a **private subnet** — only accessible from ECS tasks via security group
- Update each service's environment: `SPRING_DATASOURCE_URL=jdbc:postgresql://<rds-endpoint>:5432/<dbname>`

#### Step 3 — Provision Amazon MSK (Kafka)
- Create an **MSK cluster** (Kafka 3.5, 2 brokers minimum)
- Use `PLAINTEXT` within the VPC
- Update each service: `SPRING_KAFKA_BOOTSTRAP_SERVERS=<msk-broker-1>:9092,<msk-broker-2>:9092`

#### Step 4 — Create ECS Fargate Cluster
- Create one ECS cluster: `ecommerce-cluster`
- Create a **Task Definition** per service with:
  - Image: ECR image URI
  - Port mappings matching each service's port
  - Environment variables for DB URL, Kafka, Eureka
  - Memory: 512MB minimum per task, 1024MB for order/inventory
  - CPU: 256 units minimum
- Use **AWS Secrets Manager** for DB credentials (not hardcoded env vars)

#### Step 5 — Service Discovery on AWS
Two options:
- **Option A (Recommended):** Replace Eureka with **AWS Cloud Map** — native ECS service discovery, no extra container needed
- **Option B:** Keep Eureka — deploy eureka-server as an ECS service and point all services at its internal DNS name

If keeping Eureka, set:
```
EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server.ecommerce.local:8761/eureka/
```

#### Step 6 — ALB + Target Groups
- Create one **ALB** (internet-facing)
- One **Target Group** per service on its respective port
- Listener rules route by path prefix:
  - `/api/orders*` → order-service target group (via api-gateway)
  - All traffic hits api-gateway; api-gateway internally routes via Eureka/Cloud Map
- In practice: **only api-gateway needs to be ALB-exposed**. All other services are internal.

#### Step 7 — CI/CD Pipeline Update (Jenkins → AWS)
Update `Jenkinsfile` to add a deploy stage:
```groovy
stage('Deploy to AWS') {
    steps {
        sh 'aws ecr get-login-password | docker login ...'
        sh 'docker build -t order-service ./order-service'
        sh 'docker push <ecr-uri>/order-service:latest'
        sh 'aws ecs update-service --cluster ecommerce-cluster --service order-service --force-new-deployment'
    }
}
```

#### Step 8 — Environment Variables on ECS (never hardcode secrets)
Use **AWS Systems Manager Parameter Store** or **Secrets Manager**:
```
/ecommerce/order-service/db-url
/ecommerce/order-service/db-username
/ecommerce/order-service/db-password
/ecommerce/kafka/bootstrap-servers
```
Reference these in ECS task definitions as `valueFrom` secrets.

### Estimated AWS Cost (dev/staging)
| Resource | Estimated Monthly Cost |
|---|---|
| ECS Fargate (6 services, minimal sizing) | ~$40–60 |
| RDS PostgreSQL db.t3.micro | ~$15–25 |
| Amazon MSK (2 kafka.t3.small brokers) | ~$50–70 |
| ALB | ~$20 |
| ECR storage | ~$2–5 |
| **Total** | **~$130–180/month** |

> 💡 To cut costs during dev: use **RDS Aurora Serverless v2** (scales to zero) and a **single-broker MSK** or replace MSK with a small self-managed Kafka on EC2 t3.micro.

---

## 10. What Each Person Should Work On

### Before Starting Any Work:
1. `git checkout develop && git pull`
2. Check PR #21 is merged — if not, rebase your branch on top of it
3. Run `docker-compose up --build` and confirm all health endpoints return `{"status":"UP"}`
4. Place a test order via the curl in §6 and confirm Kafka UI at http://localhost:9000 shows the `order.created` event

### Do NOT:
- ❌ Change `server.port` in any `application.yml` — use `application.properties` only
- ❌ Use `localhost` as a hostname in any config that runs inside Docker — use container names (`postgres`, `kafka`, `eureka-server`)
- ❌ Set `spring.cloud.config.fail-fast=true` unless a config-server is running in docker-compose
- ❌ Add services to docker-compose without a `healthcheck` block — services that depend on it will race-condition start
- ❌ Merge directly to `main` — always go through `develop` via PR

---

## 11. Quick Reference — Useful Commands

```bash
# Start everything
docker-compose up --build

# Restart one service only (after code change)
docker-compose up --build order-service

# View logs for a specific service
docker-compose logs -f order-service

# Stop everything (keeps DB data)
docker-compose down

# Stop and wipe all DB data (clean slate)
docker-compose down -v

# Check what ports are actually mapped
docker ps

# Check if a specific service is healthy
curl http://localhost:<port>/actuator/health

# Run all tests
mvn clean verify

# Run tests for one module only
mvn clean verify -pl order-service
```
