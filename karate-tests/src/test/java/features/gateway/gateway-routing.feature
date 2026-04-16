Feature: API Gateway Routing Tests

  Background:
    * url gatewayUrl
    * def loginResult = call read('classpath:features/auth/auth.feature')
    * def authToken = loginResult.authToken
    * header Authorization = 'Bearer ' + authToken

  Scenario: Gateway routes to order-service health
    Given path '/api/orders/health'
    When method GET
    Then status 200

  Scenario: Gateway routes to inventory-service health
    Given path '/api/inventory/health'
    When method GET
    Then status 200

  Scenario: Gateway routes to payment-service health
    Given path '/api/payments/health'
    When method GET
    Then status 200

  Scenario: Gateway routes to shipping-service health
    Given path '/api/shipments/health'
    When method GET
    Then status 200

  Scenario: Gateway routes to notification-service health
    Given path '/api/notifications/health'
    When method GET
    Then status 200

  Scenario: Create order via gateway end-to-end
    Given path '/api/orders'
    And request
      """
      {
        "customerId": 100,
        "items": [
          { "productId": 1, "productName": "Test Item", "price": 9.99, "quantity": 2 }
        ]
      }
      """
    When method POST
    Then status 201
    And match response.orderId == '#number'
    And match response.status == 'PENDING'
