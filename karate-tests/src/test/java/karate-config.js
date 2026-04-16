function fn() {
  var env = karate.env || 'local';
  karate.log('karate.env =', env);

  var config = {
    env: env,
    gatewayUrl: 'http://localhost:8088',
    orderServiceUrl: 'http://localhost:8081',
    paymentServiceUrl: 'http://localhost:8084',
    inventoryServiceUrl: 'http://localhost:8083',
    shippingServiceUrl: 'http://localhost:8085',
    notificationServiceUrl: 'http://localhost:8086'
  };

  if (env === 'docker') {
    config.gatewayUrl = 'http://api-gateway:8088';
    config.orderServiceUrl = 'http://order-service:8081';
    config.paymentServiceUrl = 'http://payment-service:8084';
    config.inventoryServiceUrl = 'http://inventory-service:8083';
    config.shippingServiceUrl = 'http://shipping-service:8085';
    config.notificationServiceUrl = 'http://notification-service:8086';
  }

  if (env === 'ci') {
    // Jenkins runs inside a container on the bridge network (172.17.0.2)
    // localhost inside Jenkins resolves to the Jenkins container, not the host
    // Use 172.17.0.1 (Docker bridge gateway) to reach host-mapped ports
    config.gatewayUrl = 'http://172.17.0.1:8088';
    config.orderServiceUrl = 'http://172.17.0.1:8081';
    config.paymentServiceUrl = 'http://172.17.0.1:8084';
    config.inventoryServiceUrl = 'http://172.17.0.1:8083';
    config.shippingServiceUrl = 'http://172.17.0.1:8085';
    config.notificationServiceUrl = 'http://172.17.0.1:8086';
  }

  return config;
}
