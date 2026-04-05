Feature: Inventory Service API Tests

  Background:
    * url inventoryServiceUrl
    # Create a test product before each scenario so we have a known product to work with.
    # We capture the returned id so scenarios don't rely on hardcoded id=1.
    Given path '/api/inventory'
    And request { name: 'Test Product', description: 'Karate test product', price: 9.99, stockQuantity: 100 }
    When method POST
    Then status 201
    * def productId = response.id

  Scenario: Get all inventory items - should return 200
    Given path '/api/inventory'
    When method GET
    Then status 200
    And match response == '#array'

  Scenario: Add stock to a product - should return updated inventory
    Given path '/api/inventory/' + productId + '/add'
    And request { quantity: 10, orderId: null }
    When method POST
    Then status 200
    And match response.id == productId
    And match response.stockQuantity == '#number'

  Scenario: Reserve stock for an order - should reduce quantity
    Given path '/api/inventory/' + productId + '/reserve'
    And request { quantity: 1, orderId: 999 }
    When method PUT
    Then status 200
    And match response.id == productId

  Scenario: Reserve stock with insufficient quantity - should return 409
    Given path '/api/inventory/' + productId + '/reserve'
    And request { quantity: 999999, orderId: 998 }
    When method PUT
    Then status 409

  Scenario: Get inventory health
    Given path '/api/inventory/health'
    When method GET
    Then status 200
