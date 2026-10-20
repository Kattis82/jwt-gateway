#!/bin/bash

AUTH_URL="https://auth-serverproject-1-0-0.onrender.com"
PRODUCT_URL="https://product-service-1-a55s.onrender.com"
ORDER_URL="https://order-service-1-0-0.onrender.com"
EMAIL_URL="https://email-service-1-0-1.onrender.com"
GATEWAY_URL="https://gateway-tyjw.onrender.com"

INTERVAL=300

echo "Startar script.."
echo "Tryck Ctrl+C för att avsluta"

while true
do
   echo "Pingar auth-server..."
   curl -s -o /dev/null -w "Auth status: %{http_code}\n" "$AUTH_URL"

   echo "Pingar product-service..."
   curl -s -o /dev/null -w "Product status: %{http_code}\n" "$PRODUCT_URL"

   echo "Pingar order-service..."
   curl -s -o /dev/null -w "Order status: %{http_code}\n" "$ORDER_URL"

   echo "Pingar email-service..."
   curl -s -o /dev/null -w "Email status: %{http_code}\n" "$EMAIL_URL"

   echo "Pingar gateway..."
   curl -s -o /dev/null -w "Gateway status: %{http_code}\n" "$GATEWAY_URL"

   sleep $INTERVAL
done