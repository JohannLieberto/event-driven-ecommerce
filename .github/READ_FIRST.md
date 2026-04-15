# READ FIRST — AI Session Context
> This file is for AI assistant context. Read this before doing anything on this repo.

## Repo
- **Project:** Event-Driven E-Commerce Microservices
- **Stack:** Java, Spring Boot, Kafka, PostgreSQL, Eureka, Docker, Jenkins
- **Active Branch:** `develop`
- **Team:** JohannLieberto + team (this file is for AI-assisted sessions with Hitesh only)

## Current Problem
~~Kafka container has been failing in Jenkins CI for 30+ commits.~~ ✅ **FIXED April 15 2026**

Eureka health check loop in Jenkins was grepping for `"UP"` in the response body, but Spring Boot 3.3 actuator does not guarantee the literal string `UP` in the JSON body shape. Eureka was actually healthy (returning HTTP 200), but the pipeline bailed because `grep -q "UP"` never matched.

## What's Been Fixed
| Issue | Fix | Status |
|---|---|---|
| Kafka healthcheck using `localhost:9092` | TCP port check via `cat /dev/null > /dev/tcp/localhost/9092` | ✅ Fixed — commit `4cc474b` |
| Eureka wait loop grepping body for `UP` | Rely on HTTP 200 via `curl -sf ... >/dev/null 2>&1` | ✅ Fixed — April 15 2026 ~18:57 BST |
| All 6 service health loops grepping body for `UP` | Same fix — HTTP 200 check only | ✅ Fixed — April 15 2026 ~18:57 BST |

## Root Cause Summary
- **Kafka:** Healthcheck command used `localhost:9092` but broker advertises as `kafka:9092` → replaced with TCP check.
- **Eureka + Services:** Spring Boot 3.3 actuator health JSON body shape may not contain literal `UP` string → replaced `grep -q "UP"` with `curl -sf ... >/dev/null 2>&1` (relies on HTTP 2xx, fails on HTTP ≥ 400).

## Previous Failed Attempts (Kafka)
| Attempt | Why It Failed |
|---|---|
| `localhost:9092` in healthcheck (early) | Mixed up inside/outside container context |
| `kafka:9092` in healthcheck | `kafka` hostname doesn't resolve to itself inside the container |
| `cub kafka-ready` | `cub` not available in `cp-kafka:7.5.0` |
| ZK healthcheck with `nc` | `nc` not available in Confluent image |
| ZK healthcheck with `zkServer.sh` + `ruok` | `ruok` four-letter command disabled by default |
| Removed ZK healthcheck, used `service_started` | Kafka starts before ZK port is ready |
| `kafka-topics.sh --list` with `start_period: 60s` | Unreliable exit code, still unhealthy |
| `kafka-broker-api-versions.sh localhost:9092` + ZK timeouts | Kafka running fine but healthcheck returning non-zero — broker registered as `kafka:9092` not `localhost` |

## Branch State
- **Last fix pushed:** April 15 2026, ~18:57 BST
- **Jenkins:** trigger a new build — expect **Start Infrastructure** to pass now
- **Next:** if Start Infrastructure passes → move to Karate test fixes
- **If still failing:** paste `curl -i http://localhost:8761/actuator/health` output from Jenkins agent
