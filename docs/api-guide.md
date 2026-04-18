# Event-Driven Ecommerce — API Guide

## Base URL

| Environment | URL |
|-------------|-----|
| Local       | `http://localhost:8080` |
| EC2         | `http://<your-ec2-ip>:8080` |

All requests go through the **API Gateway** on port 8080.

---

## Order Service `/api/orders`

### Place an Order
```
POST /api/orders
Content-Type: application/json

{
  "customerId": 1,
  "items": [
    { "productId": 1, "quantity": 2 }
  ]
}
```
**Response 200:**
```json
{
  "orderId": 7,
  "customerId": 1,
  "status": "PENDING",
  "items": [...],
  "createdAt": "2026-04-08T12:00:00Z"
}
```

### Get Order by ID
```
GET /api/orders/{id}
```

### Health Check
```
GET /api/orders/health
```

---

## Inventory Service `/api/inventory`

### Get All Products
```
GET /api/inventory
```

### Add Stock
```
POST /api/inventory/{productId}/add?quantity=10
```

### Reserve Stock
```
PUT /api/inventory/{productId}/reserve?quantity=2
```

### Release Stock
```
PUT /api/inventory/{productId}/release?quantity=2
```

### Health Check
```
GET /api/inventory/health
```

---

## Payment Service `/api/payments`

### Process Payment
```
POST /api/payments/process
Content-Type: application/json

{
  "orderId": 7,
  "customerId": 1,
  "amount": 49.99,
  "paymentMethod": "CREDIT_CARD"
}
```

### Health Check
```
GET /api/payments/health
```

---

## Shipping Service `/api/shipments`

### Get Shipment by Order ID
```
GET /api/shipments/order/{orderId}
```

### Health Check
```
GET /api/shipments/health
```

---

## Notification Service `/api/notifications`

### Get Notifications by Customer
```
GET /api/notifications/customer/{customerId}
```

### Health Check
```
GET /api/notifications/health
```

---

## Postman Collection

Import `docs/postman/ecommerce-collection.json` into Postman.

Set the environment variable `BASE_URL` to your gateway URL.

---

## Kafka Event Flow

```
Order Created
    └── order.created
            ├── inventory-service  → reserves stock → inventory.reserved
            │       └── payment-service → processes payment → payment.completed
            │               ├── shipping-service → schedules shipment → shipment.scheduled
            │               └── notification-service → notifies customer
            └── notification-service (order confirmation)
```

---

## Running Tests

```bash
# Karate API Tests
mvn test -pl karate-tests

# k6 Performance Tests
k6 run performance-tests/order-flow-load-test.js

# With custom base URL (e.g. EC2)
k6 run -e BASE_URL=http://<ec2-ip>:8080 performance-tests/order-flow-load-test.js
```
