# Event-Driven Ecommerce — Microservices Platform

An event-driven microservices order processing platform built with **Spring Boot 3.3.5**, **Java 21**, and **Spring Cloud 2023.0.3**. The platform demonstrates a production-grade microservices architecture featuring service discovery, centralised configuration, API gateway routing with JWT authentication, asynchronous Kafka messaging, circuit breakers, Kubernetes/Helm deployment, and a full CI/CD pipeline.

---

## Architecture Overview

```
                         +------------------+
                         |   API Gateway    |  :8088
                         |  JWT Auth +      |
                         |  Routing (Eureka)|
                         +--------+---------+
                                  |
       +---------+--------+-------+--------+---------+
       |         |        |                |         |
  +----+----+ +--+------+ +------+  +------+--+ +----+-------+
  |  Order  | |Inventory| |Payment|  |Shipping | |Notification|
  | Service | | Service | |Service|  | Service | |  Service   |
  |  :8081  | |  :8083  | | :8084 |  |  :8085  | |   :8086    |
  |(Postgres)| |(Postgres)| |(Postgres)| |(Postgres)| |(Postgres)|
  +----+----+ +---------+ +---+---+  +---------+ +------------+
       |                      |
       +----------+-----------+
                  |
         +--------+--------+
         |   Apache Kafka   |
         |  (Event Bus)     |
         |  :9092           |
         +-----------------+

  +------------------+     +------------------+
  |  Eureka Server   |     |  Config Server   |
  |  (Discovery)     |     |  (Centralised    |
  |  :8761           |     |   Config) :8888  |
  +------------------+     +------------------+
```

All services register with **Eureka** for service discovery. The **Config Server** provides centralised configuration. The **API Gateway** handles routing and JWT-based authentication. Services communicate asynchronously via **Apache Kafka**.

---

## Services

| Service | Port | Description | Database |
|---|---|---|---|
| `api-gateway` | 8088 | Entry point — JWT auth + dynamic routing via Eureka | — |
| `order-service` | 8081 | Manages customer orders, publishes `orders.order-created` events | PostgreSQL (`orderdb`) |
| `inventory-service` | 8083 | Manages product stock, reserves/releases on Kafka events | PostgreSQL (`inventorydb`) |
| `payment-service` | 8084 | Processes payments, publishes `payment-completed` events | PostgreSQL (`paymentdb`) |
| `shipping-service` | 8085 | Creates shipments on payment completion | PostgreSQL (`shippingdb`) |
| `notification-service` | 8086 | Consumes all domain events and sends notifications | PostgreSQL (`notificationdb`) |
| `eureka-server` | 8761 | Netflix Eureka service registry | — |
| `config-server` | 8888 | Spring Cloud Config Server (native profile) | — |
| `kafka-ui` | 9000 | Kafka topic browser (Kafbat UI) | — |

---

## Tech Stack

| Category | Technology |
|---|---|
| Language & Runtime | Java 21, Spring Boot 3.3.5 |
| Microservices Framework | Spring Cloud 2023.0.3 (Eureka, Config, Gateway) |
| Messaging | Apache Kafka + Zookeeper (Confluent 7.5.0) |
| Database | PostgreSQL 15 (one database per service, shared instance) |
| ORM | Spring Data JPA + Hibernate |
| Security | JWT via `jjwt` (API Gateway), Spring Security |
| Resilience | Resilience4J (circuit breakers, retry) |
| Observability | Spring Actuator, Micrometer, Zipkin distributed tracing |
| Containerisation | Docker + Docker Compose |
| Orchestration | Kubernetes + Helm 3 |
| CI/CD | Jenkins (Jenkinsfile pipeline), GitHub Webhooks |
| Code Quality | SonarCloud, JaCoCo 0.8.12 |
| Integration Testing | Karate DSL 1.5.1 |
| Load Testing | k6 |
| Build | Maven 3 (multi-module) |
| Test DB | H2 (in-memory, unit tests only) |

---

## Project Structure

```
event-driven-ecommerce/
├── pom.xml                            # Parent Maven POM with dependency management
├── Jenkinsfile                        # Jenkins CI/CD pipeline definition
├── docker-compose.yml                 # Local full-stack Docker environment
├── init-db.sql                        # Database initialisation script
│
├── api-gateway/                       # API Gateway — JWT auth + service routing
├── eureka-server/                     # Service Discovery (Netflix Eureka)
├── config-server/                     # Centralised Configuration Server
├── order-service/                     # Order Management Microservice
├── inventory-service/                 # Inventory Management Microservice
├── payment-service/                   # Payment Processing Microservice
├── shipping-service/                  # Shipping & Dispatch Microservice
├── notification-service/              # Event-driven Notification Microservice
│
├── k8s/
│   └── helm/
│       └── ecommerce/
│           ├── Chart.yaml             # Helm chart metadata
│           ├── values.yaml            # Per-service resource limits, replicas, image config
│           └── templates/
│               └── deployment.yaml   # Kubernetes Deployment + Service templates
│
├── karate-tests/                      # Karate DSL integration & E2E test suite
│   └── src/test/java/
│       ├── features/
│       │   ├── auth/                  # Auth feature tests
│       │   ├── e2e/                   # End-to-end order flow tests
│       │   ├── gateway/               # API gateway routing tests
│       │   ├── inventory/             # Inventory API + reservation tests
│       │   ├── notification/          # Notification API tests
│       │   ├── payment/               # Payment API tests
│       │   └── shipping/              # Shipping API tests
│       └── com/ecommerce/karate/
│           └── KarateTestRunner.java  # Single runner — executes all features
│
├── performance-tests/
│   └── order-flow-load-test.js        # k6 load test (100 VUs, staged ramp-up)
├── jenkins/                           # Jenkins configuration
└── docs/                              # API guide and pipeline documentation
```

---

## Event-Driven Flow

```
[Client] → POST /api/orders → [API Gateway] → [Order Service]
                                                     |
                                    Publishes: orders.order-created
                                                     |
                               +---------------------+---------------------+
                               |                                           |
                    [Payment Service]                          [Inventory Service]
                    Listens: orders.order-created              Listens: orders.order-created
                    Processes payment                          Reserves stock
                    Publishes: payment-completed               Publishes: inventory.reserved
                               |                                        or inventory.reservation.failed
                    +----------+-----------+
                    |                      |
          [Shipping Service]    [Inventory Service]
          Listens:               Listens: payment-completed
          payment-completed      Commits or releases reservation
          Creates shipment
          Publishes: shipment.scheduled
                    |
          [Notification Service]
          Listens: orders.order-created, payment-completed, shipment.scheduled
          Sends notifications to customer
```

---

## Running Locally with Docker Compose

### Prerequisites
- Docker Desktop installed and running
- Java 21
- Maven 3

### Steps

```bash
# 1. Clone the repository
git clone https://github.com/JohannLieberto/event-driven-ecommerce.git
cd event-driven-ecommerce

# 2. Build all modules (skip tests for speed)
mvn clean package -DskipTests \
  -pl eureka-server,api-gateway,order-service,inventory-service,payment-service,shipping-service,notification-service

# 3. Start the full stack
docker compose up --build
```

Services start in dependency order:
1. PostgreSQL — single shared instance, separate databases per service
2. Apache Kafka + Zookeeper
3. `eureka-server` — waits for healthy state
4. `config-server` — waits for Eureka
5. All microservices — wait for their DB + Eureka + Config
6. `api-gateway` — waits for Eureka

### Verify

| Service | URL |
|---|---|
| Eureka Dashboard | http://localhost:8761 |
| API Gateway | http://localhost:8088/actuator/health |
| Order Service | http://localhost:8081/actuator/health |
| Inventory Service | http://localhost:8083/actuator/health |
| Payment Service | http://localhost:8084/actuator/health |
| Shipping Service | http://localhost:8085/actuator/health |
| Notification Service | http://localhost:8086/actuator/health |
| Kafka UI | http://localhost:9000 |

```bash
# Stop all containers
docker compose down

# Stop and remove volumes (wipes DB data)
docker compose down -v
```

---

## Kubernetes Deployment (Helm)

```bash
# Deploy all services to a Kubernetes cluster
helm install ecommerce ./k8s/helm/ecommerce

# Upgrade after changes
helm upgrade ecommerce ./k8s/helm/ecommerce

# Uninstall
helm uninstall ecommerce

# Check deployed pods
kubectl get pods
kubectl get services
```

Resource limits are configured per service in [`k8s/helm/ecommerce/values.yaml`](./k8s/helm/ecommerce/values.yaml).

---

## CI/CD Pipeline (Jenkins)

The [`Jenkinsfile`](./Jenkinsfile) defines a fully automated pipeline triggered via GitHub webhook on push:

| Stage | Description |
|---|---|
| Checkout | Pull source from GitHub |
| Build All Services | `mvn clean package -DskipTests` — compile all modules in parallel |
| Unit Tests | Run per-service unit tests in parallel; publish Surefire + JaCoCo reports |
| SonarCloud Analysis | Static analysis — results published to SonarCloud |
| Start Infrastructure | Docker Compose up — waits for Kafka, PostgreSQL, and all services to be healthy |
| Karate API Tests | Full E2E and integration test suite via Karate DSL; publish HTML report |
| Stop Infrastructure | Docker Compose down |
| Docker Build & Push | Build and push all service images to DockerHub (`hiteshkhade/*`) |
| Pull & Run from DockerHub | Pull latest images and verify containers start cleanly |

---

## Running Tests

```bash
# Unit tests for all services
mvn test -pl order-service,inventory-service,payment-service,shipping-service,notification-service

# Unit tests for a single service
mvn test -pl order-service

# Karate integration/E2E tests (requires running stack)
mvn verify -pl karate-tests -Dskip.karate=false -Dkarate.env=ci

# Coverage reports per service
# open <service>/target/site/jacoco/index.html
```

---

## Performance Testing (k6)

```bash
# Run load test — staged ramp-up to 100 virtual users
k6 run performance-tests/order-flow-load-test.js

# Against a remote target (e.g. EC2)
k6 run -e BASE_URL=http://<ec2-ip>:8088 performance-tests/order-flow-load-test.js
```

The load test ramps up to 100 VUs over 5 minutes covering the full order flow (place order → inventory check → payment).

---

## API Endpoints

All requests are routed via the API Gateway at `http://localhost:8088`. JWT token required in `Authorization: Bearer <token>` header for protected routes.

### Order Service (`/api/orders`)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/orders` | Place a new order |
| `GET` | `/api/orders/{id}` | Get order by ID |
| `GET` | `/api/orders/customer/{customerId}` | Get all orders for a customer |
| `PUT` | `/api/orders/{id}/status` | Update order status |
| `GET` | `/api/orders/health` | Health check |

### Inventory Service (`/api/inventory`)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/inventory` | List all inventory items |
| `GET` | `/api/inventory/{productId}` | Get product by ID |
| `POST` | `/api/inventory` | Add new inventory item |
| `PUT` | `/api/inventory/{productId}` | Update product / stock level |
| `DELETE` | `/api/inventory/{productId}` | Remove inventory item |
| `GET` | `/api/inventory/health` | Health check |

### Payment Service (`/api/payments`)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/payments/process` | Process a payment for an order |
| `GET` | `/api/payments/health` | Health check |

### Shipping Service (`/api/shipments`)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/shipments/order/{orderId}` | Get shipment by order ID |
| `GET` | `/api/shipments/health` | Health check |

### Notification Service (`/api/notifications`)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/notifications/customer/{customerId}` | Get notifications by customer |
| `GET` | `/api/notifications/order/{orderId}` | Get notifications by order |
| `GET` | `/api/notifications/health` | Health check |

---

## Resilience

- **Circuit Breakers** — Resilience4J circuit breakers on inter-service REST calls (e.g. Order Service → Inventory Client); fallback responses configured
- **Retry** — Exponential backoff retry on transient failures
- **Health Checks** — Spring Actuator `/actuator/health` on all services; used by Docker Compose and Kubernetes liveness/readiness probes

---

## Branch Strategy

| Branch | Purpose |
|---|---|
| `main` | Stable, production-ready code |
| `develop` | Integration branch — all features merge here first |

---

## Team

MSc Software Design with Cloud Native Computing — TUS Project 2026
