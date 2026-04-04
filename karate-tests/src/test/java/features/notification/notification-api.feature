Feature: Notification Service API Tests

  Background:
    * url notificationServiceUrl

  Scenario: Get notifications by customer ID
    Given path '/api/notifications/customer/100'
    When method GET
    Then status 200
    And match response == '#array'

  Scenario: Get notifications by order ID
    Given path '/api/notifications/order/1'
    When method GET
    Then status 200
    And match response == '#array'

  Scenario: Notification health check
    Given path '/api/notifications/health'
    When method GET
    Then status 200
