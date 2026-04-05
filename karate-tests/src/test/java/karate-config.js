function fn() {
  var env = karate.env || 'local';
  karate.log('karate.env =', env);

  var config = {
    env: env,
    gatewayUrl: 'http://localhost:8080',
    orderServiceUrl: 'http://localhost:8081',
    paymentServiceUrl: 'http://localhost:8084',
    inventoryServiceUrl: 'http://localhost:8083',
    shippingServiceUrl: 'http://localhost:8085',
    notificationServiceUrl: 'http://localhost:8086'
  };

  if (env === 'docker') {
    config.gatewayUrl = 'http://api-gateway:8080';
    config.orderServiceUrl = 'http://order-service:8081';
    config.paymentServiceUrl = 'http://payment-service:8084';
    config.inventoryServiceUrl = 'http://inventory-service:8083';
    config.shippingServiceUrl = 'http://shipping-service:8085';
    config.notificationServiceUrl = 'http://notification-service:8086';
  }

  if (env === 'ci') {
    config.gatewayUrl = 'http://localhost:8080';
    config.orderServiceUrl = 'http://localhost:8081';
    config.paymentServiceUrl = 'http://localhost:8084';
    config.inventoryServiceUrl = 'http://localhost:8083';
    config.shippingServiceUrl = 'http://localhost:8085';
    config.notificationServiceUrl = 'http://localhost:8086';
  }

  return config;
}
