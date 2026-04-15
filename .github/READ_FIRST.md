# READ FIRST — AI Session Context
> This file is for AI assistant context. Read this before doing anything on this repo.

## Repo
- **Project:** Event-Driven E-Commerce Microservices
- **Stack:** Java, Spring Boot, Kafka, PostgreSQL, Eureka, Docker, Jenkins
- **Active Branch:** `develop`
- **Team:** JohannLieberto + team (this file is for AI-assisted sessions with Hitesh only)

## ✅ Infrastructure is Fully Working (April 15 2026)

All Docker Compose healthchecks and Jenkins wait loops are fixed and verified. The entire infrastructure stack (Kafka, Postgres, Eureka, all 6 services) comes up cleanly and Jenkins correctly waits for each layer before proceeding.

## What's Been Fixed
| Issue | Fix | Commit / Time |
|---|---|---|
| Kafka healthcheck using `localhost:9092` | TCP port check via `cat /dev/null > /dev/tcp/localhost/9092` | `4cc474b` |
| Eureka + 6 service Jenkins wait loops grepping body for `UP` | HTTP 200 via `curl -sf ... >/dev/null 2>&1` | ~18:57 BST |
| Eureka `curl -sf` unreliable from Jenkins agent | `docker inspect --format='{{.State.Health.Status}}' eureka-server \| grep -q 'healthy'` | `897b742` |
| 6 app service Jenkins wait loops using `curl -sf localhost:PORT` | Same `docker inspect` pattern for all 6 | `46381db` |
| All 7 `docker-compose.yml` healthchecks using `wget \| grep -q UP` | `wget -qO- ... >/dev/null 2>&1` (HTTP 200, no body parse) | `0520950` |
| Karate stage using `mvn test` — tests silently skipped | `mvn verify -Dskip.karate=false`; JUnit path → `failsafe-reports/` | latest |

## Root Cause Summary
- **Kafka:** Healthcheck used `localhost:9092` but broker advertises as `kafka:9092` → replaced with TCP check.
- **Eureka + all services — `grep -q UP`:** Spring Boot 3.3 actuator JSON body does not guarantee the literal string `UP` → replaced with HTTP 200 check only.
- **Jenkins `curl -sf localhost`:** Unreliable from the Jenkins agent shell regardless of port mapping → replaced with `docker inspect` which reads Docker's own internal healthcheck verdict.
- **`docker-compose.yml` healthchecks:** Were still using `wget | grep -q UP` internally, so Docker was marking containers `unhealthy` even though services were up → fixed to HTTP 200 (`wget -qO- ... >/dev/null 2>&1`).
- **Karate tests silently skipped:** `pom.xml` configures Karate under `maven-failsafe-plugin` (integration-test phase), not surefire. `mvn test` never reaches that phase. Fix: `mvn verify -Dskip.karate=false`. JUnit reports land in `failsafe-reports/`, not `surefire-reports/`.

## Previous Failed Attempts (Kafka — historical)
| Attempt | Why It Failed |
|---|---|
| `localhost:9092` in healthcheck (early) | Mixed up inside/outside container context |
| `kafka:9092` in healthcheck | `kafka` hostname doesn't resolve to itself inside the container |
| `cub kafka-ready` | `cub` not available in `cp-kafka:7.5.0` |
| ZK healthcheck with `nc` | `nc` not available in Confluent image |
| ZK healthcheck with `zkServer.sh` + `ruok` | `ruok` four-letter command disabled by default |
| Removed ZK healthcheck, used `service_started` | Kafka starts before ZK port is ready |
| `kafka-topics.sh --list` with `start_period: 60s` | Unreliable exit code, still unhealthy |
| `kafka-broker-api-versions.sh localhost:9092` + ZK timeouts | Broker registered as `kafka:9092` not `localhost` |

## Current State
- **Infrastructure:** ✅ All green — Kafka, Postgres, Eureka, all 6 services healthy
- **Karate stage:** Fix pushed — next build should execute tests and produce `failsafe-reports/`
- **Next:** Verify Karate tests actually run and pass on next Jenkins build
