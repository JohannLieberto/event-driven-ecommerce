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

### [2026-04-15] Gateway Route Poll → Eureka REST API Check

**Branch:** `develop`
**Stage affected:** `Start Infrastructure`

#### Problem

After the 6 application services became healthy (via `docker inspect`), the pipeline polled the **API Gateway** routes endpoint to confirm service discovery:

```bash
curl -sf http://localhost:8088/actuator/gateway/routes | grep -q "$SVC"
```

This was unreliable — the gateway route table may not reflect Eureka registration state accurately, and the gateway itself could be slow to refresh its route cache even after services are fully registered in Eureka.

#### Fix

Replaced the gateway route poll with a **direct Eureka REST API check** per service. Each service is queried individually at `http://localhost:8761/eureka/apps/${SVC}` and the response JSON is validated with a `python3` inline script to confirm `status == UP`:

```bash
# Before — gateway route poll
SERVICES="order-service inventory-service payment-service shipping-service notification-service"
RETRIES=30
for SVC in $SERVICES; do
    until curl -sf http://localhost:8088/actuator/gateway/routes | grep -q "$SVC"; do
        ...
        sleep 5
    done
done

# After — direct Eureka REST API check
SERVICES="ORDER-SERVICE INVENTORY-SERVICE PAYMENT-SERVICE SHIPPING-SERVICE NOTIFICATION-SERVICE"
MAX_ATTEMPTS=20
for SVC in $SERVICES; do
    until \
        curl -sf \
            -H "Accept: application/json" \
            "http://localhost:8761/eureka/apps/${SVC}" \
        | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    status = d['application']['instance'][0]['status']
    sys.exit(0 if status == 'UP' else 1)
except Exception:
    sys.exit(1)
" 2>/dev/null; do
        ...
        sleep 15
    done
done
```

Key differences:
- Service names are **uppercase** to match Eureka's registry format (`ORDER-SERVICE` not `order-service`)
- Retry interval increased from **5s → 15s** (Eureka heartbeat interval is 30s; polling faster than that is wasteful)
- Max attempts reduced from **30 → 20** (total timeout: 20 × 15s = 5 minutes)
- Checks `status == UP` explicitly — a registered-but-DOWN instance would previously pass the gateway grep

#### Status
✅ Fix applied. Already present in `develop` Jenkinsfile.

---

### [2026-04-15] Maven Local Repository Cache — `-Dmaven.repo.local`

**Branch:** `develop`
**Stage affected:** `Build All Services`, `Unit Tests` (all 5), `Karate API Tests`

#### Problem

Maven was downloading dependencies from the internet on every pipeline run because Jenkins workspace isolation does not reuse the default `~/.m2/repository` across builds reliably, and different pipeline stages may run with different working directories.

#### Fix

Added a `MAVEN_CACHE` environment variable and passed `-Dmaven.repo.local` to every `mvn` invocation:

```groovy
// In environment block
MAVEN_CACHE = "${env.HOME}/.m2/repository"
```

Applied to:
- `mvn clean package -DskipTests ...` (Build All Services)
- `mvn test -pl order-service ...` (Unit Test — order-service)
- `mvn test -pl inventory-service ...` (Unit Test — inventory-service)
- `mvn test -pl payment-service ...` (Unit Test — payment-service)
- `mvn test -pl shipping-service ...` (Unit Test — shipping-service)
- `mvn test -pl notification-service ...` (Unit Test — notification-service)
- `mvn verify -pl karate-tests ...` (Karate API Tests)

All 7 `mvn` calls now include `-Dmaven.repo.local=${MAVEN_CACHE}`.

#### Status
✅ Fix applied. Already present in `develop` Jenkinsfile.

---

## Known Pending Items

- ~~The 6 downstream application services still use `curl -sf` from the Jenkins host for their readiness checks.~~ **Resolved** — all 6 now use `docker inspect` (same pattern as Eureka).
- The Eureka REST API check requires `python3` to be available on the Jenkins agent. Confirm this is installed before the next pipeline run.
- Karate test suite coverage and passing status on `develop` is unverified against the current infrastructure stack — next pipeline run will confirm.

---

## Infrastructure Notes

- All services run on a shared `ecommerce-network` Docker bridge network.
- Eureka healthcheck has a `start_period: 60s` — Docker will not report `healthy` before 60 seconds have elapsed regardless of actual boot time.
- The Jenkins agent must have Docker CLI access (`docker`, `docker compose`, `docker exec`, `docker inspect`) available on `PATH`.
- `python3` must be installed on the Jenkins agent (required for the Eureka REST API status check).
- `curl` must be installed on the Jenkins agent for the Eureka REST API checks to work.
