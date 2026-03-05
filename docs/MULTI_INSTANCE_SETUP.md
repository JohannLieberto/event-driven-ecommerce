# Multi-Instance Deployment Guide

## Overview

The platform supports running multiple instances of services for:

- High availability
- Load distribution
- Zero-downtime deployment

## Prerequisites

- Eureka Server
- Config Server
- API Gateway
- PostgreSQL running

## Starting Multiple Instances

### Order Service

Instance 1:

SERVER_PORT=8081 mvn -pl order-service spring-boot:run

Instance 2:

SERVER_PORT=8083 mvn -pl order-service spring-boot:run

### Inventory Service

Instance 1:

SERVER_PORT=8082 mvn -pl inventory-service spring-boot:run

Instance 2:

SERVER_PORT=8084 mvn -pl inventory-service spring-boot:run

## Verification

Open:

http://localhost:8761

Check all services show **UP**.

## Load Balancing

API Gateway uses **Eureka service discovery**.

Requests are distributed using **round-robin load balancing**.

## Health Check

curl http://localhost:8081/actuator/health  
curl http://localhost:8083/actuator/health