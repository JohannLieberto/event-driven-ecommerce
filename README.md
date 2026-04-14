# Event-Driven Ecommerce — Microservices Platform

An event-driven microservices order processing platform built with **Spring Boot 3.2**, **Java 17**, and **Spring Cloud**. The platform demonstrates a production-grade microservices architecture featuring service discovery, centralised configuration, API gateway routing with JWT authentication, asynchronous Kafka messaging, distributed tracing, circuit breakers, Kubernetes/Helm deployment, and a full CI/CD pipeline.

---

## Architecture Overview

```
                         +------------------+
                         |   API Gateway    |  :8080
                         |  JWT Auth +      |
                         |  Routing (Eureka)|
                         +--------+---------+
                                  |
       +---------+--------+-------+--------+---------+
       |         |        |                |         |
  +----+----+ +--+------+ +------+  +------+--+ +----+-------+
  |  Order  | |Inventory| |Payment|  |Shipping | |Notification|
  | Service | | Service | |Service|  | Service | |  Service   |
  |  :8081  | |  :8082  | | :8083 |  |  :8084  | |   :8085    |
  |(MySQL)  | |(MySQL)  | |(MySQL)|  | (MySQL) | |            |
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

All services register with **Eureka** for service discovery. The **Config Server** provides centralised configuration. The **API Gateway** handles routing and JWT-based authentication. Services communicate asynchronously via **Apache Kafka** (order-placed, payment-processed, shipment-dispatched events).

---

## Services

| Service | Port | Description | Database |
|---|---|---|---|
| `api-gateway` | 8080 | Entry point — JWT auth + dynamic routing via Eureka | — |
| `order-service` | 8081 | Manages customer orders (CRUD, status updates, Kafka producer) | MySQL (`order_db`) |
| `inventory-service` | 8082 | Manages product stock levels and reservations | MySQL (`inventory_db`) |
| `payment-service` | 8083 | Processes payments, publishes payment events to Kafka | MySQL (`payment_db`) |
| `shipping-service` | 8084 | Handles shipment creation and dispatch events | MySQL (`shipping_db`) |
| `notification-service` | 8085 | Consumes Kafka events and sends notifications | — |
| `eureka-server` | 8761 | Netflix Eureka service registry | — |
| `config-server` | 8888 | Spring Cloud Config Server (native profile) | — |

---

## Tech Stack

| Category | Technology |
|---|---|
| Language & Runtime | Java 17, Spring Boot 3.2.0 |
| Microservices Framework | Spring Cloud 2023.0.0 (Eureka, Config, Gateway) |
| Messaging | Apache Kafka (event-driven inter-service communication) |
| Database | MySQL 8 (one database per service) |
| ORM | Spring Data JPA + Hibernate 6 |
| Security | JWT via `jjwt` (API Gateway), Spring Security |
| Resilience | Resilience4J (circuit breakers, retry, rate limiting) |
| Observability | Micrometer + Zipkin (distributed tracing), Spring Actuator |
| API Documentation | SpringDoc OpenAPI 3 (Swagger UI) |
| Containerisation | Docker + Docker Compose |
| Orchestration | Kubernetes + Helm 3 |
| CI/CD | Jenkins (Jenkinsfile pipeline) |
| Code Quality | SonarQube, JaCoCo 0.8.11 |
| Integration Testing | Karate DSL |
| Load Testing | k6 |
| Build | Maven 3 (multi-module) |
| Test DB | H2 (in-memory, unit tests only) |

---

## Project Structure

```
event-driven-ecommerce/
├── README.md                          # Project overview and setup guide
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
├── karate-tests/                      # Karate DSL integration test suite
├── performance-tests/
│   └── order-flow-load-test.js        # k6 load test (100 VUs, staged ramp-up)
├── coverage-report/                   # Aggregated JaCoCo coverage report module
├── jenkins/                           # Jenkins configuration
└── docs/                              # Project documentation and diagrams
```

---

## Event-Driven Flow

```
[Client] → POST /api/orders → [API Gateway] → [Order Service]
                                                     |
                                          Publishes: order-placed
                                                     |
                               +---------------------+--------------------+
                               |                                          |
                    [Payment Service]                          [Inventory Service]
                    Processes payment                          Reserves stock
                    Publishes: payment-processed
                               |
                    [Shipping Service]
                    Creates shipment
                    Publishes: shipment-dispatched
                               |
                    [Notification Service]
                    Sends order confirmation
```

---

## Running Locally with Docker Compose

### Prerequisites
- Docker Desktop installed and running
- Java 17 (to build JARs before Docker build)

### Steps

```bash
# 1. Clone the repository
git clone https://github.com/JohannLieberto/event-driven-ecommerce.git
cd event-driven-ecommerce

# 2. Build all modules (skip tests for speed)
mvn clean package -DskipTests

# 3. Start the full stack
docker-compose up --build
```

Services start in dependency order:
1. MySQL databases — start first
2. Apache Kafka + Zookeeper
3. `eureka-server` — waits for healthy state
4. `config-server` — waits for Eureka
5. All microservices — wait for their DB + Eureka + Config
6. `api-gateway` — waits for Eureka

### Verify

| Service | URL |
|---|---|
| Eureka Dashboard | http://localhost:8761 |
| API Gateway | http://localhost:8080/actuator/health |
| Order Service | http://localhost:8081/actuator/health |
| Inventory Service | http://localhost:8082/actuator/health |
| Payment Service | http://localhost:8083/actuator/health |
| Shipping Service | http://localhost:8084/actuator/health |
| Notification Service | http://localhost:8085/actuator/health |
| Zipkin Tracing UI | http://localhost:9411 |
| Swagger UI (Order) | http://localhost:8081/swagger-ui.html |

```bash
# Stop all containers
docker-compose down

# Stop and remove volumes (wipes DB data)
docker-compose down -v
```

---

## Kubernetes Deployment (Helm)

```bash
# Deploy all services to a local Kubernetes cluster
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

The [`Jenkinsfile`](./Jenkinsfile) defines a fully automated pipeline:

| Stage | Description |
|---|---|
| Checkout | Pull source from GitHub |
| Build | `mvn clean compile -DskipTests` — compile all modules |
| Unit Tests | `mvn test` — run unit tests, publish Surefire reports |
| Code Quality | SonarQube static analysis + quality gate |
| Integration Tests | Karate DSL end-to-end API tests |
| Package | `mvn package -DskipTests` — build executable JARs |
| Docker Build | Build and tag Docker images per service |
| Deploy | Push images and deploy to target environment |

Test reports published from `**/target/surefire-reports/TEST-*.xml`. Coverage reports generated by JaCoCo at `coverage-report/target/site/jacoco-aggregate/`.

---

## Performance Testing (k6)

```bash
# Run load test — 100 virtual users, staged ramp-up
k6 run performance-tests/order-flow-load-test.js
```

The load test covers the full order flow (place order → payment → inventory check) with defined thresholds for p95 latency and error rate.

---

## Running Tests

```bash
# Run all tests with coverage
mvn clean verify

# Individual module tests
mvn clean verify -pl order-service
mvn clean verify -pl inventory-service

# Integration tests (Karate)
mvn verify -pl karate-tests

# Coverage reports
# Aggregated:  coverage-report/target/site/jacoco-aggregate/index.html
# Per-service: <service>/target/site/jacoco/index.html
```

---

## API Endpoints

All requests are routed via the API Gateway at `http://localhost:8080`. JWT token required in `Authorization: Bearer <token>` header.

### Order Service (`/api/orders`)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/orders` | Place a new order |
| `GET` | `/api/orders/{id}` | Get order by ID |
| `GET` | `/api/orders/customer/{customerId}` | Get all orders for a customer |
| `PUT` | `/api/orders/{id}/status` | Update order status |

### Inventory Service (`/api/inventory`)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/inventory` | List all inventory items (paginated) |
| `GET` | `/api/inventory/{productId}` | Get product by ID |
| `POST` | `/api/inventory` | Add new inventory item |
| `PUT` | `/api/inventory/{productId}` | Update product / stock level |
| `DELETE` | `/api/inventory/{productId}` | Remove inventory item |

### Payment Service (`/api/payments`)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/payments` | Process a payment for an order |
| `GET` | `/api/payments/{orderId}` | Get payment status for an order |

### Shipping Service (`/api/shipping`)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/shipping` | Create a shipment |
| `GET` | `/api/shipping/{orderId}` | Get shipment status for an order |

Full interactive API documentation available via Swagger UI at `http://localhost:<service-port>/swagger-ui.html`.

---

## Resilience & Observability

- **Circuit Breakers** — Resilience4J circuit breakers on all inter-service REST calls; fallback responses configured per service
- **Distributed Tracing** — Micrometer + Zipkin; every request receives a trace ID propagated across all service hops; view at http://localhost:9411
- **Health Checks** — Spring Actuator `/actuator/health` on all services
- **Metrics** — Actuator `/actuator/metrics` and `/actuator/prometheus` endpoints

---

## Branch Strategy

| Branch | Purpose |
|---|---|
| `main` | Stable, production-ready code |
| `develop` | Integration branch — all features merge here first |
| `feature/US*` | Individual user story feature branches |
| `fix/*` | Bug fix branches |
| `integration/sprint*` | Sprint demo / integration branches |

---

## Team

MSc Software Design with Cloud Native Computing — TUS Project 2026
