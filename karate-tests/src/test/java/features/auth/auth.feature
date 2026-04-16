Feature: Authentication

  Scenario: Login and get JWT token
    Given url gatewayUrl
    And path '/auth/login'
    And request { username: 'customer1', password: 'pass123' }
    When method POST
    Then status 200
    And match response.token == '#string'
    * def authToken = response.token
