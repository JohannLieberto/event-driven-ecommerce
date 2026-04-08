Feature: Inventory reservation

  Background:
    * url baseUrl
    * def productId = 4

  Scenario: create order with enough stock and verify inventory shows reduced available stock
    Given path 'api', 'inventory', productId
    When method get
    Then status 200
    * def initialStock = response.stockQuantity

    Given path 'api', 'inventory', productId, 'reserve'
    And request
    """
    {
      "orderId": 7001,
      "quantity": 2
    }
    """
    When method put
    Then status 200
    And match response.stockQuantity == initialStock - 2

    Given path 'api', 'inventory', productId
    When method get
    Then status 200
    And match response.stockQuantity == initialStock - 2

  Scenario: create order exceeding available stock and verify failure behaviour
    Given path 'api', 'inventory', productId
    When method get
    Then status 200
    * def initialStock = response.stockQuantity

    Given path 'api', 'inventory', productId, 'reserve'
    And request
    """
    {
      "orderId": 7002,
      "quantity": 9999
    }
    """
    When method put
    Then status 409
    And match response.message contains 'Insufficient stock'

    Given path 'api', 'inventory', productId
    When method get
    Then status 200
    And match response.stockQuantity == initialStock