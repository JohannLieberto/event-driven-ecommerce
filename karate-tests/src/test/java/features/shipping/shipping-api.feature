Feature: Shipping Service API Tests

  Background:
    * def loginResult = call read('classpath:features/auth/auth.feature')
    * def authToken = loginResult.authToken
    * url shippingServiceUrl
    * header Authorization = 'Bearer ' + authToken

  Scenario: Get shipment by order ID - should return 200 if exists
    # Create order and process payment first so a shipment exists
    Given url gatewayUrl
    And header Authorization = 'Bearer ' + authToken
    And path '/api/orders'
    And request { customerId: 100, items: [{ productId: 1, productName: 'Widget', price: 9.99, quantity: 1 }] }
    When method POST
    Then status 201
    * def createdOrderId = response.orderId

    Given url paymentServiceUrl
    And header Authorization = 'Bearer ' + authToken
    And path '/api/payments/process'
    And request { orderId: '#(createdOrderId)', customerId: 100, amount: 9.99, paymentMethod: 'CREDIT_CARD' }
    When method POST
    Then status 200

    # Poll until shipment is created via Kafka event (up to 30s)
    Given url shippingServiceUrl
    And header Authorization = 'Bearer ' + authToken
    * configure retry = { count: 10, interval: 3000 }
    Given path '/api/shipments/order/' + createdOrderId
    When method GET
    Then retry until responseStatus == 200
    And match response.orderId == createdOrderId
    And match response.trackingNumber == '#string'
    And match response.status == 'SHIPMENT_SCHEDULED'

  Scenario: Get shipments by customer ID
    Given path '/api/shipments/customer/100'
    When method GET
    Then status 200
    And match response == '#array'

  Scenario: Get shipment for non-existent order - should return 404
    Given path '/api/shipments/order/99999'
    When method GET
    Then status 404

  Scenario: Shipping health check
    Given path '/api/shipments/health'
    When method GET
    Then status 200
