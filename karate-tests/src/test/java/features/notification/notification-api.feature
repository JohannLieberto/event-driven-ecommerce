Feature: Notification Service API Tests

  Background:
    * def loginResult = call read('classpath:features/auth/auth.feature')
    * def authToken = loginResult.authToken
    * url notificationServiceUrl
    * header Authorization = 'Bearer ' + authToken

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
