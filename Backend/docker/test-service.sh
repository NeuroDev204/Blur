#!/bin/bash

echo "🧪 Testing All Services"
echo "======================="
echo ""

test_endpoint() {
    local NAME=$1
    local URL=$2

    echo -n "Testing $NAME... "
    if curl -sf "$URL" > /dev/null 2>&1; then
        echo "✅ OK"
        return 0
    else
        echo "❌ FAILED"
        return 1
    fi
}

echo "Backend Services:"
test_endpoint "Profile Service" "http://localhost:8081/actuator/health"
test_endpoint "Content Service" "http://localhost:8082/actuator/health"
test_endpoint "Notification Service" "http://localhost:8082/notification/actuator/health"
test_endpoint "Chat Service" "http://localhost:8083/chat/actuator/health"
test_endpoint "API Gateway" "http://localhost:8888/actuator/health"

echo ""
echo "Frontend:"
test_endpoint "React App" "http://localhost"

echo ""
echo "======================================"
echo "✅ All tests completed!"
echo "======================================"
