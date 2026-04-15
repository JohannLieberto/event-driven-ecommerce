# CI Pipeline — Read First

This document tracks known issues, applied fixes, and important context for the Jenkins CI pipeline on the `develop` branch.

---

## Pipeline Overview

| Stage | Tool | Notes |
|---|---|---|
| Build | Maven | Skips tests, builds all 7 services |
| Unit Tests | Maven + JUnit + JaCoCo | Parallel across 5 services |
| Start Infrastructure | Docker Compose | Spins up Kafka, Zookeeper, Postgres, Eureka, all services |
| Karate API Tests | Maven + Karate | E2E tests against running containers |
| Stop Infrastructure | Docker Compose | Tears down all containers and volumes |
| Docker Build & Push | Docker | `main` branch only — pushes to `HiteshKhade` DockerHub |
| Deploy to Kubernetes | kubectl | `main` branch only — deploys to `ecommerce` namespace |

---

## Fix History

### [2026-04-15] Eureka Server Readiness Check — `curl` → `docker inspect`

**Commit:** `897b742` 
**Branch:** `develop` 
**Stage affected:** `Start Infrastructure` 

#### Problem

The Jenkins pipeline was failing with:
```
ERROR: Eureka Server did not become ready. Aborting.
```
The Eureka readiness check used `curl -sf http://localhost:8761/actuator/health` running on the **Jenkins host**. This consistently failed all 24 retries (2 minutes) despite Eureka being fully started inside its container (confirmed by Spring Boot logs: `Started EurekaServerApplication in 10.918 seconds`).

Manual verification after the job confirmed the endpoint was reachable from the host (`HTTP/1.1 200`), ruling out a port mapping issue (`8761:8761` is correctly mapped in `docker-compose.yml`).

Root cause: `curl -sf` exits non-zero even on HTTP 200 when the response body is not written to stdout in a way that satisfies the `-f` flag under certain Jenkins shell execution contexts. The check was unreliable from the Jenkins agent environment.

#### Fix

Replaced the `curl` check with `docker inspect`, which queries Docker's own internal healthcheck verdict directly:

```bash
# Before
until curl -sf http://localhost:8761/actuator/health >/dev/null 2>&1; do

# After
until docker inspect --format='{{.State.Health.Status}}' eureka-server 2>/dev/null | grep -q 'healthy'; do
```

This is consistent with the pattern already used for Kafka (`docker exec kafka ...`) and Postgres (`docker exec postgres pg_isready ...`). It leverages the healthcheck already defined in `docker-compose.yml` for `eureka-server`:

```yaml
healthcheck:
  test: ["CMD-SHELL", "wget -qO- http://localhost:8761/actuator/health | grep -q UP"]
  interval: 15s
  timeout: 10s
  retries: 8
  start_period: 60s
```

Docker's verdict is authoritative and environment-agnostic — it runs inside the container, not from the Jenkins host.

#### Status
✅ Fix pushed. Pending confirmation on next pipeline run.

---

## Known Pending Items

- The 6 downstream application services (`api-gateway`, `order-service`, `inventory-service`, `payment-service`, `shipping-service`, `notification-service`) still use `curl -sf` from the Jenkins host for their readiness checks. If those fail, the same `docker inspect` fix should be applied to all of them.

---

## Infrastructure Notes

- All services run on a shared `ecommerce-network` Docker bridge network.
- Eureka healthcheck has a `start_period: 60s` — Docker will not report `healthy` before 60 seconds have elapsed regardless of actual boot time.
- The Jenkins agent must have Docker CLI access (`docker`, `docker compose`, `docker exec`, `docker inspect`) available on `PATH`.
- `curl` must be installed on the Jenkins agent for the downstream service checks to work.
