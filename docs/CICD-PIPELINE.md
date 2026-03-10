# CI/CD Pipeline Documentation

## Overview
Automated Jenkins pipeline for building, testing, and deploying the event-driven e-commerce microservices platform.

## Pipeline Architecture
GitHub Push → Jenkins Poll → Checkout → Build → Test → Quality → Package → Deploy ↓ ↓ ↓ ↓ ↓ ↓ ↓ Trigger Clone Compile Unit Static JARs Docker Tests Analysis Images

## Stages Explained

### 1. Checkout
- **Purpose:** Clone latest code from GitHub
- **Tool:** Git
- **Branch:** develop
- **Duration:** ~10 seconds

### 2. Build
- **Purpose:** Compile all Java source code
- **Tool:** Maven (`mvn clean compile`)
- **Modules:** 5 services (order, inventory, eureka, config, gateway)
- **Duration:** ~60 seconds

### 3. Unit Tests
- **Purpose:** Execute isolated unit tests
- **Tool:** Maven Surefire Plugin
- **Coverage:** 45 tests across all modules
- **Pass Criteria:** 100% must pass
- **Duration:** ~40 seconds

### 4. Integration Tests
- **Purpose:** Test service interactions
- **Tool:** Maven Failsafe Plugin
- **Key Tests:** Order-Inventory integration, REST API endpoints
- **Duration:** ~60 seconds

### 5. Package
- **Purpose:** Create executable JAR files
- **Tool:** Maven (`mvn package`)
- **Output:** `target/*.jar` for each service
- **Duration:** ~30 seconds

## How to Trigger

### Manual Build
1. Go to Jenkins: http://localhost:8080
2. Select: event-driven-ecommerce-pipeline
3. Click: Build Now

### Automatic Build (Configured)
- **Trigger:** Push to develop branch
- **Polling:** Every 5 minutes
- **GitHub Webhook:** (configure in GitHub settings for instant triggers)

## Build Status Indicators

| Status | Icon | Meaning |
|--------|------|---------|
| Success | ☀️ Blue ball | All stages passed |
| Failure | ⚠️ Red ball | At least one stage failed |
| Unstable | ⚡ Yellow ball | Tests failed but build succeeded |
| Aborted | ⚪ Gray ball | Manually stopped |

## Test Reports

After each build, view detailed test reports:
- **Location:** Workspace → order-service/target/surefire-reports/
- **Jenkins UI:** Build → Test Results
- **HTML Report:** Build → Order Service Test Report

## Troubleshooting

### Build Fails at Compile Stage
- Check: Java version (must be 17+)
- Check: Maven dependencies download
- Fix: `mvn clean install -U` to force update

### Tests Fail
- Check: Database containers running
- Check: Port conflicts (8081, 8082)
- Fix: Restart Docker containers

### Pipeline Not Triggering
- Check: SCM polling enabled
- Check: GitHub repository accessible
- Fix: Verify Git credentials in Jenkins
