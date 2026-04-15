# READ FIRST — AI Session Context
> This file is for AI assistant context. Read this before doing anything on this repo.

## Repo
- **Project:** Event-Driven E-Commerce Microservices
- **Stack:** Java, Spring Boot, Kafka, PostgreSQL, Eureka, Docker, Jenkins
- **Active Branch:** `develop`
- **Team:** JohannLieberto + team (this file is for AI-assisted sessions with Hitesh only)

## ✅ Infrastructure is Fully Working (April 15 2026)

All Docker Compose healthchecks and Jenkins wait loops are fixed and verified. The entire infrastructure stack (Kafka, Postgres, Eureka, all 6 services) comes up cleanly and Jenkins correctly waits for each layer before proceeding. **Infrastructure is confirmed stable.**

## What's Been Fixed
| Issue | Fix | Commit / Time |
|---|---|---|
| Kafka healthcheck using `localhost:9092` | TCP port check via `cat /dev/null > /dev/tcp/localhost/9092` | `4cc474b` |
| Eureka + 6 service Jenkins wait loops grepping body for `UP` | HTTP 200 via `curl -sf ... >/dev/null 2>&1` | ~18:57 BST |
| Eureka `curl -sf` unreliable from Jenkins agent | `docker inspect --format='{{.State.Health.Status}}' eureka-server \| grep -q 'healthy'` | `897b742` |
| 6 app service Jenkins wait loops using `curl -sf localhost:PORT` | Same `docker inspect` pattern for all 6 | `46381db` |
| All 7 `docker-compose.yml` healthchecks using `wget \| grep -q UP` | `wget -qO- ... >/dev/null 2>&1` (HTTP 200, no body parse) | `0520950` |
| Karate stage archiving from `surefire-reports/` (wrong path) | Identified — JUnit reports must come from `failsafe-reports/` | latest |

## Root Cause Summary
- **Kafka:** Healthcheck used `localhost:9092` but broker advertises as `kafka:9092` → replaced with TCP check.
- **Eureka + all services — `grep -q UP`:** Spring Boot 3.3 actuator JSON body does not guarantee the literal string `UP` → replaced with HTTP 200 check only.
- **Jenkins `curl -sf localhost`:** Unreliable from the Jenkins agent shell regardless of port mapping → replaced with `docker inspect` which reads Docker's own internal healthcheck verdict.
- **`docker-compose.yml` healthchecks:** Were still using `wget | grep -q UP` internally, so Docker was marking containers `unhealthy` even though services were up → fixed to HTTP 200 (`wget -qO- ... >/dev/null 2>&1`).
- **Karate tests silently skipped (current issue):** `mvn test` triggers only surefire (unit test phase) — it never reaches the `integration-test` phase where Karate runs under `maven-failsafe-plugin`. The log shows `Tests are skipped.` because surefire has `<skipTests>` or `<skip>` set in the karate-tests `pom.xml`. The fix requires using `mvn verify` (or `mvn failsafe:integration-test failsafe:verify`) and the Jenkinsfile must archive from `failsafe-reports/*.xml`, not `surefire-reports/*.xml`.

## ⚠️ Current Open Issue — Karate Tests Not Running (April 15 2026)

**Symptom (build #44):**
- Stage runs `mvn test -pl karate-tests -Dkarate.env=ci`
- Maven output: `Tests are skipped.` → `BUILD SUCCESS`
- Jenkins archive step: `No test report files were found` in `surefire-reports/`

**Root cause:**
Karate is wired to `maven-failsafe-plugin` (integration-test phase), not surefire. `mvn test` never executes it. The `Tests are skipped.` message comes from surefire finding the runner class but having skip enabled.

**Fix needed in Jenkinsfile:**
```groovy
// Change this:
sh 'mvn test -pl karate-tests -Dkarate.env=ci'

// To this:
sh 'mvn verify -pl karate-tests -Dkarate.env=ci -DskipUnitTests=true'
```

**Fix needed in archive step:**
```groovy
// Change this:
junit 'karate-tests/target/surefire-reports/*.xml'

// To this:
junit 'karate-tests/target/failsafe-reports/*.xml'
```

**Also verify in `karate-tests/pom.xml`:**
- `maven-failsafe-plugin` is present and bound to the `integration-test` + `verify` goals
- No `<skip>true</skip>` or `<skipITs>true</skipITs>` blocking execution
- The Karate runner class is named `*IT.java` or `*ITRunner.java` (failsafe naming convention), or the plugin is explicitly configured to include `**/*Runner.java`

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
- **Infrastructure:** ✅ Fully stable — Kafka, Postgres, Eureka, all 6 services healthy and confirmed working
- **Karate stage:** ⚠️ Tests silently skipped — `mvn test` wrong phase; needs `mvn verify` + archive path fix to `failsafe-reports/`
- **Next:** Fix Jenkinsfile `mvn verify` command + JUnit archive path, then verify Karate tests actually execute and produce reports
