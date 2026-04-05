-- Seed data for Karate tests
-- Ensures a shipment exists for orderId=1 so shipping-api.feature passes without requiring a full Kafka flow
INSERT INTO shipments (order_id, customer_id, status, tracking_number, created_at, updated_at)
SELECT 1, 100, 'SHIPMENT_SCHEDULED', 'TRACK-SEED-001', NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM shipments WHERE order_id = 1
);
