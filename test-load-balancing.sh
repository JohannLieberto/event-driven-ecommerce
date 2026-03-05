#!/bin/bash

# Get JWT token
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
 -H "Content-Type: application/json" \
 -d '{"username":"customer1","password":"pass123"}' \
 | jq -r '.token')

echo "Token: $TOKEN"

# Send 20 requests
for i in {1..20}; do
 echo "Request $i"
 curl -s http://localhost:8080/api/orders \
   -H "Authorization: Bearer $TOKEN"
done