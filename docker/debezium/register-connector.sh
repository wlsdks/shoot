#!/bin/bash

# Debezium Outbox Connector 등록 스크립트
# Kafka Connect가 완전히 시작된 후 Connector를 등록합니다

set -e

echo "========================================="
echo "Debezium Outbox Connector 등록"
echo "========================================="

# Kafka Connect가 준비될 때까지 대기
echo "Kafka Connect 준비 대기 중..."
until curl -f http://localhost:8083/ > /dev/null 2>&1; do
    echo "Kafka Connect가 아직 준비되지 않았습니다. 5초 후 재시도..."
    sleep 5
done

echo "✓ Kafka Connect 준비 완료"

# 기존 Connector 삭제 (있다면)
echo ""
echo "기존 Connector 확인 중..."
if curl -f http://localhost:8083/connectors/shoot-outbox-connector > /dev/null 2>&1; then
    echo "기존 Connector 발견. 삭제 중..."
    curl -X DELETE http://localhost:8083/connectors/shoot-outbox-connector
    echo "✓ 기존 Connector 삭제 완료"
    sleep 2
else
    echo "기존 Connector 없음"
fi

# Connector 등록
echo ""
echo "Outbox Connector 등록 중..."
curl -X POST \
    -H "Content-Type: application/json" \
    --data @/docker/debezium/outbox-connector.json \
    http://localhost:8083/connectors

echo ""
echo "========================================="
echo "✓ Connector 등록 완료"
echo "========================================="

# Connector 상태 확인
echo ""
echo "Connector 상태 확인:"
curl -s http://localhost:8083/connectors/shoot-outbox-connector/status | jq .

echo ""
echo "========================================="
echo "CDC 구성 완료!"
echo ""
echo "Debezium UI: http://localhost:8084"
echo "Kafka Connect API: http://localhost:8083"
echo "Kafka UI: http://localhost:8085"
echo "========================================="
