Feature: Inventory Service API Tests

  Background:
    * url inventoryServiceUrl

  Scenario: Get all inventory items - should return 200
    Given path '/api/inventory'
    When method GET
    Then status 200
    And match response == '#array'

  Scenario: Add stock to a product - should return updated inventory
    Given path '/api/inventory/1/add'
    And request { quantity: 10, orderId: null }
    When method POST
    Then status 200
    And match response.productId == 1
    And match response.quantity == '#number'

  Scenario: Reserve stock for an order - should reduce quantity
    Given path '/api/inventory/1/reserve'
    And request { quantity: 1, orderId: 999 }
    When method POST
    Then status 200
    And match response.productId == 1

  Scenario: Reserve stock with insufficient quantity - should return 409
    Given path '/api/inventory/1/reserve'
    And request { quantity: 999999, orderId: 998 }
    When method POST
    Then status 409

  Scenario: Get inventory health
    Given path '/api/inventory/health'
    When method GET
    Then status 200
