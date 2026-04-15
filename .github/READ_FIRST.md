# READ FIRST — AI Session Context
> This file is for AI assistant context. Read this before doing anything on this repo.

## Repo
- **Project:** Event-Driven E-Commerce Microservices
- **Stack:** Java, Spring Boot, Kafka, PostgreSQL, Eureka, Docker, Jenkins
- **Active Branch:** `develop`
- **Team:** JohannLieberto + team (this file is for AI-assisted sessions with Hitesh only)

## Current Problem
Kafka container has been failing in Jenkins CI for 30+ commits.
All dependent services (order, inventory, payment, shipping, notification) wait on `kafka: service_healthy` — so if Kafka fails, the entire pipeline fails.

## What's Been Tried (All Failed)
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

## Root Cause (Confirmed April 15 2026)
Kafka was **fully started and healthy** — `docker logs kafka` showed `[KafkaServer id=1] started` successfully.
The healthcheck command `kafka-broker-api-versions.sh --bootstrap-server localhost:9092` was failing because
the broker advertises itself as `kafka:9092` internally, not `localhost`.
This was a **healthcheck command issue**, not a Kafka startup issue.

## Current Fix (Pushed ✅ — Awaiting Jenkins Result)
- **docker-compose.yml:** Kafka healthcheck replaced with pure TCP port check:
  `bash -c 'cat /dev/null > /dev/tcp/localhost/9092'`
- Verified working on EC2 via `docker exec kafka bash -c 'cat /dev/null > /dev/tcp/localhost/9092'` → exit code 0
- Timings: `interval: 10s`, `timeout: 10s`, `retries: 10`, `start_period: 30s`
- **Commit:** [`4cc474b`](https://github.com/JohannLieberto/event-driven-ecommerce/commit/4cc474bb2a9ae797935d268067f673eb658d482b) — April 15 2026 ~18:19 BST

## Next Steps
- [ ] Trigger Jenkins build and confirm **Start Infrastructure** stage passes
- [ ] If passing: pipeline is unblocked — move to Karate test fixes
- [ ] If failing: paste `docker ps -a` and `docker logs kafka` output here

## Branch State
- **Last fix pushed:** April 15 2026, ~18:19 BST
- **Jenkins:** next build to be triggered (builds #26–#38 all failed at **Start Infrastructure**)
- **Status:** TCP healthcheck fix live on `develop`, awaiting CI confirmation
