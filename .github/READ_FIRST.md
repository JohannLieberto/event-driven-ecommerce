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
| `kafka-broker-api-versions.sh localhost:9092` + ZK timeouts | Kafka container exiting (1) on boot — crash before healthcheck |

## Current Fix (Pushed — Awaiting Jenkins Result)
- **docker-compose.yml:** `restart: on-failure` added to Kafka, `kafka-broker-api-versions.sh --bootstrap-server localhost:9092` healthcheck, ZK timeout env vars
- **Jenkinsfile:** `docker compose down -v --remove-orphans` before `up --build` to clear stale state; Kafka readiness loop updated to use `localhost:9092`
- **Commit:** latest on `develop` — April 15 2026

## Next Steps
- [ ] Wait for Jenkins build #37 result
- [ ] If passing: pipeline is unblocked, move to Karate test fixes
- [ ] If failing: pull exact `docker compose logs kafka` output and audit further

## Branch State
- Last fix pushed: April 15 2026, ~18:01 BST
- Jenkins: build #37 triggered — awaiting result
- All previous builds (#26, #30, #36) failed at **Start Infrastructure** stage
