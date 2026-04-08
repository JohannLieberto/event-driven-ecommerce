Feature: Payment Service API Tests

  Background:
    * url paymentServiceUrl

  Scenario: Process a successful payment - should return 200 with PAYMENT_SUCCESS
    Given path '/api/payments/process'
    And request
      """
      {
        "orderId": 1,
        "customerId": 100,
        "amount": 99.99,
        "paymentMethod": "CREDIT_CARD"
      }
      """
    When method POST
    Then status 200
    And match response.status == 'PAYMENT_SUCCESS'
    And match response.transactionId == '#string'
    And match response.orderId == 1

  Scenario: Process payment with missing fields - should return 400
    Given path '/api/payments/process'
    And request { "orderId": null }
    When method POST
    Then status 400

  Scenario: Get payment by order ID
    Given path '/api/payments/order/1'
    When method GET
    Then status 200
    And match response.orderId == 1

  Scenario: Payment health check
    Given path '/api/payments/health'
    When method GET
    Then status 200
