# Event-Driven-Ecommerce

An event-driven microservices order processing platform built with Spring Boot 3.2, Java 17, and Spring Cloud. The platform demonstrates a real-world microservices architecture with service discovery, centralised configuration, API gateway routing with JWT authentication, and independent PostgreSQL databases per service.

---

## Architecture Overview

```
                     +------------------+
                     |   API Gateway    |  :8080
                     |  (JWT Auth +     |
                     |   Routing)       |
                     +--------+---------+
                              |
             +----------------+----------------+
             |                                 |
    +--------+--------+             +----------+--------+
    |  Order Service  |             | Inventory Service |
    |    :8081        |             |    :8082           |
    |  (PostgreSQL)   |             |  (PostgreSQL)      |
    +-----------------+             +-------------------+

             +----------------+     +------------------+
             |  Eureka Server |     |  Config Server   |
             |  (Discovery)   |     |  (Centralised    |
             |  :8761         |     |   Config) :8888  |
             +----------------+     +------------------+
```

All services register with **Eureka** for service discovery. The **Config Server** provides centralised configuration. The **API Gateway** handles routing and JWT-based authentication for all incoming requests.

---

## Services

| Service | Port | Description | Database |
|---|---|---|---|
| `api-gateway` | 8080 | Entry point - JWT auth + routing to downstream services | - |
| `order-service` | 8081 | Manages customer orders (create, view, update status) | PostgreSQL (`order_db`) |
| `inventory-service` | 8082 | Manages product stock levels and reservations | PostgreSQL (`inventory_db`) |
| `eureka-server` | 8761 | Netflix Eureka service registry | - |
| `config-server` | 8888 | Spring Cloud Config Server (native profile) | - |

---

## Tech Stack

- **Java 17** + **Spring Boot 3.2.0**
- **Spring Cloud 2023.0.0** (Eureka, Config, Gateway)
- **Spring Data JPA** + **Hibernate 6**
- **PostgreSQL 15** (one database per service)
- **H2** (in-memory, for tests only)
- **JWT** via `jjwt 0.11.5` (API Gateway security)
- **Docker** + **Docker Compose** (local dev stack)
- **Jenkins** (CI/CD pipeline)
- **JaCoCo 0.8.11** (code coverage)
- **Maven 3** (multi-module build)

---

## Project Structure

```
event-driven-ecommerce/
+-- README.md
+-- pom.xml                        # Parent POM - dependency management
+-- Jenkinsfile                    # Jenkins CI/CD pipeline
+-- docker-compose.yml             # Local full-stack Docker environment
+-- eureka-server/                 # Service Discovery (Netflix Eureka)
|   +-- pom.xml
|   +-- src/
+-- config-server/                 # Centralised Config Server (native)
|   +-- pom.xml
|   +-- src/
+-- api-gateway/                   # API Gateway with JWT auth + routing
|   +-- pom.xml
|   +-- src/
+-- order-service/                 # Order management microservice
|   +-- pom.xml
|   +-- src/
+-- inventory-service/             # Inventory & stock microservice
|   +-- pom.xml
|   +-- src/
+-- docs/                          # Project documentation
```

---

## Running Locally with Docker Compose

### Prerequisites
- Docker Desktop installed and running
- Java 17 (to build JARs before Docker builds)

### Steps

```bash
# 1. Clone the repository
git clone https://github.com/JohannLieberto/event-driven-ecommerce.git
cd event-driven-ecommerce

# 2. Build all modules (skipping tests for speed)
mvn clean package -DskipTests

# 3. Start the full stack
docker-compose up --build
```

Services start in dependency order:
1. `order-db` and `inventory-db` (PostgreSQL) - start first
2. `eureka-server` - waits for healthy DB
3. `config-server` - waits for Eureka
4. `order-service` and `inventory-service` - wait for their DB + Eureka
5. `api-gateway` - waits for Eureka

### Verify

- Eureka Dashboard: http://localhost:8761
- Config Server: http://localhost:8888/actuator/health
- Order Service: http://localhost:8081/actuator/health
- Inventory Service: http://localhost:8082/actuator/health
- API Gateway: http://localhost:8080/actuator/health

```bash
# Stop all containers
docker-compose down

# Stop and remove volumes (wipes DB data)
docker-compose down -v
```

---

## CI/CD Pipeline (Jenkins)

The `Jenkinsfile` defines a 5-stage pipeline:

| Stage | Command | Description |
|---|---|---|
| Checkout | `checkout scm` | Pull source from Git |
| Build | `mvn clean compile -DskipTests` | Compile all modules |
| Unit Tests | `mvn test` | Run unit tests, publish Surefire reports |
| Integration Tests | `mvn verify -DskipUnitTests` | Run integration tests |
| Package | `mvn package -DskipTests` | Build executable JARs |

Test reports are published from `**/target/surefire-reports/TEST-*.xml`.

---

## Branch Strategy

| Branch | Purpose |
|---|---|
| `main` | Stable, production-ready code |
| `develop` | Integration branch - all features merge here |
| `feature/US*` | Individual user story feature branches |
| `fix/*` | Bug fix branches |
| `integration/sprint*` | Sprint demo / integration branches |

---

## Running Tests

```bash
# Run all tests
mvn test

# Run integration tests
mvn verify -DskipUnitTests

# Run with coverage report
mvn verify
# Coverage reports: target/site/jacoco/index.html
```

---

## API Endpoints

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
| `GET` | `/api/inventory` | List all inventory items |
| `GET` | `/api/inventory/{productId}` | Get stock for a product |
| `POST` | `/api/inventory` | Add new inventory item |
| `PUT` | `/api/inventory/{productId}` | Update stock level |
| `DELETE` | `/api/inventory/{productId}` | Remove inventory item |
