Feature: Shipping Service API Tests

  Background:
    * url shippingServiceUrl

  Scenario: Get shipment by order ID - should return 200 if exists
    Given path '/api/shipments/order/1'
    When method GET
    Then status 200
    And match response.orderId == 1
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
