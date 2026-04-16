Feature: End-to-End Event-Driven Flow Tests

  Background:
    * def loginResult = call read('classpath:features/auth/auth.feature')
    * def authToken = loginResult.authToken
    * url gatewayUrl
    * header Authorization = 'Bearer ' + authToken
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
    And header Authorization = 'Bearer ' + authToken
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

    # Step 3 & 4: Poll until shipment is created via Kafka event (up to 30s)
    Given url shippingServiceUrl
    And header Authorization = 'Bearer ' + authToken
    * configure retry = { count: 10, interval: 3000 }
    Given path '/api/shipments/order/' + orderId
    When method GET
    Then retry until responseStatus == 200
    And match response.status == 'SHIPMENT_SCHEDULED'
    And match response.trackingNumber == '#string'
    * print 'Shipment scheduled with tracking:', response.trackingNumber

    # Step 5: Verify Notifications were sent
    Given url notificationServiceUrl
    And header Authorization = 'Bearer ' + authToken
    Given path '/api/notifications/order/' + orderId
    When method GET
    Then status 200
    And match response == '#[_ > 0]'
    * print 'Notifications sent:', response.length
