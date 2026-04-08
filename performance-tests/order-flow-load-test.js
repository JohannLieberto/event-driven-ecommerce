import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// Custom metrics
const orderCreated = new Counter('orders_created');
const orderFailRate = new Rate('order_fail_rate');
const orderDuration = new Trend('order_duration_ms');

// Test configuration
export const options = {
  stages: [
    { duration: '30s', target: 10 },   // Ramp up to 10 users
    { duration: '1m',  target: 50 },   // Ramp up to 50 users
    { duration: '2m',  target: 50 },   // Stay at 50 users
    { duration: '30s', target: 100 },  // Ramp up to 100 users
    { duration: '1m',  target: 100 },  // Stay at 100 users
    { duration: '30s', target: 0 },    // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],  // 95% of requests under 2s
    http_req_failed:   ['rate<0.05'],   // Less than 5% failure rate
    order_fail_rate:   ['rate<0.05'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  // 1 - Place an order
  const orderPayload = JSON.stringify({
    customerId: Math.floor(Math.random() * 100) + 1,
    items: [
      {
        productId: Math.floor(Math.random() * 10) + 1,
        quantity: Math.floor(Math.random() * 3) + 1,
      },
    ],
  });

  const orderStart = Date.now();
  const orderRes = http.post(`${BASE_URL}/api/orders`, orderPayload, {
    headers: { 'Content-Type': 'application/json' },
  });
  orderDuration.add(Date.now() - orderStart);

  const orderOk = check(orderRes, {
    'order created - status 200/201': (r) => r.status === 200 || r.status === 201,
    'order has orderId': (r) => JSON.parse(r.body).orderId !== undefined,
  });

  if (orderOk) {
    orderCreated.add(1);
    orderFailRate.add(0);
  } else {
    orderFailRate.add(1);
  }

  sleep(1);

  // 2 - Check inventory health
  const inventoryRes = http.get(`${BASE_URL}/api/inventory/health`);
  check(inventoryRes, {
    'inventory health ok': (r) => r.status === 200,
  });

  sleep(0.5);

  // 3 - Check payment health
  const paymentRes = http.get(`${BASE_URL}/api/payments/health`);
  check(paymentRes, {
    'payment health ok': (r) => r.status === 200,
  });

  sleep(1);
}
