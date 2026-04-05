Feature: End-to-End Event-Driven Flow Tests

  Background:
    * url gatewayUrl
    * def orderId = null

  Scenario: Full flow - create order, process payment, verify shipment and notifications
    # Step 1: Create an Order
    Given path '/api/orders'
    And request
      """
      {
        "customerId": 100,
        "items": [
          { "productId": 1, "productName": "Widget", "price": 49.99, "quantity": 1 }
        ]
      }
      """
    When method POST
    Then status 201
    And match response.status == 'PENDING'
    * def orderId = response.orderId
    * print 'Created order with id:', orderId

    # Step 2: Process Payment
    Given url paymentServiceUrl
    Given path '/api/payments/process'
    And request
      """
      {
        "orderId": #(orderId),
        "customerId": 100,
        "amount": 49.99,
        "paymentMethod": "CREDIT_CARD"
      }
      """
    When method POST
    Then status 200
    And match response.status == 'PAYMENT_SUCCESS'
    * print 'Payment processed, waiting for Kafka events to propagate...'

    # Step 3: Wait for async events to propagate (Kafka processing time)
    * def sleep = function(ms){ java.lang.Thread.sleep(ms) }
    * sleep(3000)

    # Step 4: Verify Shipment was scheduled
    Given url shippingServiceUrl
    Given path '/api/shipments/order/' + orderId
    When method GET
    Then status 200
    And match response.status == 'SHIPMENT_SCHEDULED'
    And match response.trackingNumber == '#string'
    * print 'Shipment scheduled with tracking:', response.trackingNumber

    # Step 5: Verify Notifications were sent
    Given url notificationServiceUrl
    Given path '/api/notifications/order/' + orderId
    When method GET
    Then status 200
    And match response == '#[_ > 0]'
    * print 'Notifications sent:', response.length
