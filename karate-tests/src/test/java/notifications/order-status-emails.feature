Feature: Order status email notifications

  Background:
    * url 'http://localhost:8086'

  Scenario: verify notifications can be fetched for an order
    Given path 'api/notifications/order/9101'
    When method get
    Then status 200