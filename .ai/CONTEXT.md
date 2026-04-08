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
- **Docker + Docker Compose** for local development AND cloud deployment
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

> On the EC2 server, replace `localhost` with the EC2 public IP, e.g. `http://<EC2-IP>:8081/actuator/health`

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

## 9. Cloud Deployment Plan — Single EC2 Instance with Docker Compose

> **Why this approach?** We run `docker-compose up` locally and it works perfectly. On AWS we do exactly the same thing on a single EC2 instance. No ECS, no MSK, no RDS — just one VM running Docker. Cost: ~$15–30/month.

### Target Architecture

```
Internet
   │
   ▼
[EC2 Instance - e.g. t3.medium]
   │  (all ports opened via Security Group)
   │
   ├── docker-compose up --build
   │     ├── api-gateway        :8080  ← public-facing
   │     ├── order-service      :8081
   │     ├── inventory-service  :8083
   │     ├── payment-service    :8084
   │     ├── shipping-service   :8085
   │     ├── notification-service :8086
   │     ├── eureka-server      :8761
   │     ├── kafka + zookeeper  :9092
   │     ├── kafka-ui           :9000
   │     └── postgres           :5432
```

Everything runs in Docker containers on one machine, exactly as it does locally. No changes to code or docker-compose required.

---

### Step-by-Step EC2 Deployment

#### Step 1 — Launch an EC2 Instance
- **AMI:** Amazon Linux 2023 (or Ubuntu 22.04)
- **Instance type:** `t3.medium` (2 vCPU, 4GB RAM) — minimum for running all services + Kafka
  - If budget allows: `t3.large` (8GB RAM) for more headroom
- **Storage:** 20GB gp3 SSD (enough for Docker images + data)
- **Key pair:** Create or use existing `.pem` key for SSH
- **Security Group — open these inbound ports:**

| Port | Purpose |
|---|---|
| 22 | SSH (your IP only, not 0.0.0.0) |
| 8080 | API Gateway (public) |
| 8761 | Eureka dashboard (optional, restrict if needed) |
| 9000 | Kafka UI (optional, restrict if needed) |
| 8081–8086 | Microservices (optional, restrict to team IPs) |

> ⚠️ Never open port 5432 (Postgres) or 9092 (Kafka) to the internet. They communicate internally via Docker network.

---

#### Step 2 — SSH Into the Instance
```bash
ssh -i your-key.pem ec2-user@<EC2-PUBLIC-IP>
# Ubuntu: ssh -i your-key.pem ubuntu@<EC2-PUBLIC-IP>
```

---

#### Step 3 — Install Docker and Docker Compose
```bash
# Amazon Linux 2023
sudo dnf update -y
sudo dnf install -y docker git
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ec2-user

# Install Docker Compose plugin
MKDIR -P ~/.docker/cli-plugins
curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
  -o ~/.docker/cli-plugins/docker-compose
chmod +x ~/.docker/cli-plugins/docker-compose

# Log out and back in so group change takes effect
exit
ssh -i your-key.pem ec2-user@<EC2-PUBLIC-IP>

# Verify
docker --version
docker compose version
```

---

#### Step 4 — Install Java 17 and Maven (to build JARs)
```bash
# Amazon Linux 2023
sudo dnf install -y java-17-amazon-corretto-devel

# Install Maven
wget https://downloads.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz
tar -xf apache-maven-3.9.6-bin.tar.gz
sudo mv apache-maven-3.9.6 /opt/maven
echo 'export PATH=/opt/maven/bin:$PATH' >> ~/.bashrc
source ~/.bashrc

# Verify
java -version
mvn -version
```

---

#### Step 5 — Clone the Repo and Build
```bash
git clone https://github.com/JohannLieberto/event-driven-ecommerce.git
cd event-driven-ecommerce
git checkout develop

# Build all JARs (skipping tests for speed)
mvn clean package -DskipTests
```

---

#### Step 6 — Start the Full Stack
```bash
docker compose up --build -d
# -d runs it in the background (detached mode)
```

Watch startup progress:
```bash
docker compose logs -f
# Ctrl+C to stop watching (containers keep running)
```

---

#### Step 7 — Verify Everything Is Running
```bash
# From the EC2 instance itself:
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health

# From your laptop (replace with EC2 public IP):
curl http://<EC2-IP>:8080/actuator/health
```

Browser links (from your laptop):
- Eureka: `http://<EC2-IP>:8761`
- Kafka UI: `http://<EC2-IP>:9000`
- API Gateway: `http://<EC2-IP>:8080`

---

#### Step 8 — Keep It Running After SSH Disconnect
Since we used `-d` (detached), containers keep running when you close SSH. To manage:
```bash
# Stop everything
docker compose down

# Restart everything
docker compose up -d

# See what's running
docker ps

# View logs for a specific service
docker compose logs -f order-service
```

Optionally, set Docker to start on reboot:
```bash
sudo systemctl enable docker
# Containers with restart: always in docker-compose will auto-start
```

Add `restart: unless-stopped` to each service in `docker-compose.yml` so containers come back automatically after an EC2 reboot:
```yaml
order-service:
  restart: unless-stopped
  ...
```

---

#### Step 9 — Deploying Updates (Pull and Restart)
When new code is merged to `develop`, SSH in and run:
```bash
cd event-driven-ecommerce
git pull
mvn clean package -DskipTests
docker compose up --build -d
```

Only the services with changed images will rebuild. Others restart instantly from cache.

---

### Estimated AWS Cost

| Resource | Monthly Cost |
|---|---|
| EC2 `t3.medium` (on-demand) | ~$30 |
| EC2 `t3.medium` (1-year reserved) | ~$18 |
| 20GB gp3 EBS storage | ~$1.60 |
| Data transfer (light usage) | ~$1–3 |
| **Total (on-demand)** | **~$32–35/month** |
| **Total (reserved)** | **~$20–22/month** |

> 💡 If the team only needs it running during demos/sprints, **stop the EC2 instance** when not in use — you only pay for storage (~$1.60/month) when stopped.

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
- ❌ Open ports 5432 or 9092 in the EC2 security group — these must stay internal only

---

## 11. Quick Reference — Useful Commands

```bash
# Start everything
docker compose up --build -d

# Restart one service only (after code change)
docker compose up --build -d order-service

# View logs for a specific service
docker compose logs -f order-service

# Stop everything (keeps DB data)
docker compose down

# Stop and wipe all DB data (clean slate)
docker compose down -v

# Check what ports are actually mapped
docker ps

# Check if a specific service is healthy
curl http://localhost:<port>/actuator/health

# Run all tests
mvn clean verify

# Run tests for one module only
mvn clean verify -pl order-service

# Deploy update from develop
git pull && mvn clean package -DskipTests && docker compose up --build -d
```
