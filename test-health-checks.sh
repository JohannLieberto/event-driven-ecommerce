#!/usr/bin/env bash
# Run this after: docker compose -f docker-compose.yml up -d --build
# Usage: bash test-health-checks.sh

set -e

wait_for_healthy() {
    local NAME=$1
    local RETRIES=48
    local COUNT=0
    until [ "$(docker inspect --format="{{.State.Health.Status}}" "$NAME" 2>/dev/null)" = "healthy" ]; do
        COUNT=$((COUNT+1))
        if [ $COUNT -ge $RETRIES ]; then
            echo "ERROR: $NAME did not become healthy after $((RETRIES * 5))s"
            docker compose -f docker-compose.yml logs "$NAME" --tail=30
            exit 1
        fi
        echo "$NAME not ready yet... $COUNT/$RETRIES (status: $(docker inspect --format="{{.State.Health.Status}}" "$NAME" 2>/dev/null || echo 'container not found'))"
        sleep 5
    done
    echo "$NAME is ready ✅"
}

echo "=== Waiting for all services ==="
wait_for_healthy eureka-server
wait_for_healthy api-gateway
wait_for_healthy order-service
wait_for_healthy inventory-service
wait_for_healthy payment-service
wait_for_healthy shipping-service
wait_for_healthy notification-service

echo ""
echo "=== Verifying gateway routing for all services ==="

verify_route() {
    local SVC=$1
    local PATH=$2
    local RETRIES=24
    local COUNT=0
    until docker exec api-gateway wget -qO- "http://localhost:8080${PATH}" >/dev/null 2>&1; do
        COUNT=$((COUNT+1))
        if [ $COUNT -ge $RETRIES ]; then
            echo "ERROR: Gateway cannot route to $SVC after $((RETRIES * 5))s"
            exit 1
        fi
        echo "Gateway -> $SVC not ready yet... $COUNT/$RETRIES"
        sleep 5
    done
    echo "Gateway -> $SVC routing verified ✅"
}

verify_route order-service     /api/orders/health
verify_route inventory-service /api/inventory/health
verify_route payment-service   /api/payments/health
verify_route shipping-service  /api/shipments/health

echo ""
echo "=== All checks passed! Stack is ready for Karate tests. ==="
