#!/bin/bash

# CDC 성능 테스트 스크립트

PGPASSWORD=1234
PGHOST=localhost
PGPORT=5432
PGUSER=root
PGDATABASE=member

echo "============================================================"
echo "CDC 성능 테스트"
echo "============================================================"
echo "시작 시간: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

# 테스트 1: 단일 이벤트
echo "============================================================"
echo "Test 1: 단일 이벤트"
echo "============================================================"

SAGA_ID="perf-single-$(date +%s%3N)"

echo "이벤트 삽입 중... (saga_id: $SAGA_ID)"

RESULT=$(PGPASSWORD=$PGPASSWORD psql -h $PGHOST -p $PGPORT -U $PGUSER -d $PGDATABASE -t -A -c "
INSERT INTO outbox_events (
    saga_id, saga_state, event_type, payload,
    processed, retry_count, created_at, idempotency_key
) VALUES (
    '$SAGA_ID',
    'STARTED',
    'com.stark.shoot.domain.event.MessageSentEvent',
    '{\"message\":{\"id\":null,\"roomId\":1,\"senderId\":1,\"content\":{\"text\":\"Performance test single\",\"type\":\"TEXT\",\"isDeleted\":false,\"isEdited\":false},\"status\":\"SENT\",\"replyToMessageId\":null,\"threadId\":null,\"expiresAt\":null,\"messageReactions\":{\"reactions\":{}},\"mentions\":[],\"createdAt\":\"2025-10-26T04:00:00Z\",\"updatedAt\":null,\"readBy\":{},\"metadata\":{},\"isPinned\":false,\"pinnedBy\":null,\"pinnedAt\":null},\"occurredOn\":1761456000000}',
    false,
    0,
    NOW(),
    '$SAGA_ID-MessageSentEvent'
)
RETURNING id, created_at;
")

EVENT_ID=$(echo $RESULT | cut -d'|' -f1)
CREATED_AT=$(echo $RESULT | cut -d'|' -f2)

echo "  - Event ID: $EVENT_ID"
echo "  - Created At: $CREATED_AT"
echo ""

echo "처리 대기 중..."
sleep 5

# 처리 여부 확인
RESULT=$(PGPASSWORD=$PGPASSWORD psql -h $PGHOST -p $PGPORT -U $PGUSER -d $PGDATABASE -t -A -c "
SELECT
    processed,
    EXTRACT(EPOCH FROM (processed_at - created_at)) as latency_seconds
FROM outbox_events
WHERE id = $EVENT_ID;
")

PROCESSED=$(echo $RESULT | cut -d'|' -f1)
LATENCY=$(echo $RESULT | cut -d'|' -f2)

if [ "$PROCESSED" = "t" ]; then
    LATENCY_MS=$(echo "$LATENCY * 1000" | bc)
    echo "✅ 처리 완료!"
    echo "  - Latency: ${LATENCY}초 (${LATENCY_MS}ms)"
else
    echo "❌ 아직 처리되지 않음"
fi

echo ""

# 테스트 2: 10개 배치
echo "============================================================"
echo "Test 2: 10개 배치"
echo "============================================================"

TIMESTAMP=$(date +%s%3N)
echo "10개 이벤트 삽입 중..."

for i in {1..10}; do
    SAGA_ID="perf-batch10-${TIMESTAMP}-${i}"
    PGPASSWORD=$PGPASSWORD psql -h $PGHOST -p $PGPORT -U $PGUSER -d $PGDATABASE -q -c "
    INSERT INTO outbox_events (
        saga_id, saga_state, event_type, payload,
        processed, retry_count, created_at, idempotency_key
    ) VALUES (
        '$SAGA_ID',
        'STARTED',
        'com.stark.shoot.domain.event.MessageSentEvent',
        '{\"message\":{\"id\":null,\"roomId\":1,\"senderId\":1,\"content\":{\"text\":\"Batch test 10-$i\",\"type\":\"TEXT\",\"isDeleted\":false,\"isEdited\":false},\"status\":\"SENT\",\"replyToMessageId\":null,\"threadId\":null,\"expiresAt\":null,\"messageReactions\":{\"reactions\":{}},\"mentions\":[],\"createdAt\":\"2025-10-26T04:00:00Z\",\"updatedAt\":null,\"readBy\":{},\"metadata\":{},\"isPinned\":false,\"pinnedBy\":null,\"pinnedAt\":null},\"occurredOn\":1761456000000}',
        false,
        0,
        NOW(),
        '$SAGA_ID-MessageSentEvent'
    );"
done

echo "  - 10개 삽입 완료"
echo ""
echo "처리 대기 중..."
sleep 5

# 통계 조회
echo "통계 계산 중..."
PGPASSWORD=$PGPASSWORD psql -h $PGHOST -p $PGPORT -U $PGUSER -d $PGDATABASE -c "
SELECT
    COUNT(*) as total_events,
    COUNT(CASE WHEN processed THEN 1 END) as processed_events,
    ROUND(AVG(EXTRACT(EPOCH FROM (processed_at - created_at)))::numeric, 3) as avg_latency_sec,
    ROUND(MIN(EXTRACT(EPOCH FROM (processed_at - created_at)))::numeric, 3) as min_latency_sec,
    ROUND(MAX(EXTRACT(EPOCH FROM (processed_at - created_at)))::numeric, 3) as max_latency_sec,
    ROUND((AVG(EXTRACT(EPOCH FROM (processed_at - created_at))) * 1000)::numeric, 0) as avg_latency_ms
FROM outbox_events
WHERE saga_id LIKE 'perf-batch10-${TIMESTAMP}%';
"

echo ""

# 테스트 3: 50개 배치
echo "============================================================"
echo "Test 3: 50개 배치"
echo "============================================================"

TIMESTAMP=$(date +%s%3N)
echo "50개 이벤트 삽입 중..."

for i in {1..50}; do
    SAGA_ID="perf-batch50-${TIMESTAMP}-${i}"
    PGPASSWORD=$PGPASSWORD psql -h $PGHOST -p $PGPORT -U $PGUSER -d $PGDATABASE -q -c "
    INSERT INTO outbox_events (
        saga_id, saga_state, event_type, payload,
        processed, retry_count, created_at, idempotency_key
    ) VALUES (
        '$SAGA_ID',
        'STARTED',
        'com.stark.shoot.domain.event.MessageSentEvent',
        '{\"message\":{\"id\":null,\"roomId\":1,\"senderId\":1,\"content\":{\"text\":\"Batch test 50-$i\",\"type\":\"TEXT\",\"isDeleted\":false,\"isEdited\":false},\"status\":\"SENT\",\"replyToMessageId\":null,\"threadId\":null,\"expiresAt\":null,\"messageReactions\":{\"reactions\":{}},\"mentions\":[],\"createdAt\":\"2025-10-26T04:00:00Z\",\"updatedAt\":null,\"readBy\":{},\"metadata\":{},\"isPinned\":false,\"pinnedBy\":null,\"pinnedAt\":null},\"occurredOn\":1761456000000}',
        false,
        0,
        NOW(),
        '$SAGA_ID-MessageSentEvent'
    );"

    if [ $((i % 10)) -eq 0 ]; then
        echo "  - $i/50 삽입 완료"
    fi
done

echo "  - 50개 삽입 완료"
echo ""
echo "처리 대기 중..."
sleep 10

# 통계 조회
echo "통계 계산 중..."
PGPASSWORD=$PGPASSWORD psql -h $PGHOST -p $PGPORT -U $PGUSER -d $PGDATABASE -c "
SELECT
    COUNT(*) as total_events,
    COUNT(CASE WHEN processed THEN 1 END) as processed_events,
    ROUND(AVG(EXTRACT(EPOCH FROM (processed_at - created_at)))::numeric, 3) as avg_latency_sec,
    ROUND(MIN(EXTRACT(EPOCH FROM (processed_at - created_at)))::numeric, 3) as min_latency_sec,
    ROUND(MAX(EXTRACT(EPOCH FROM (processed_at - created_at)))::numeric, 3) as max_latency_sec,
    ROUND((AVG(EXTRACT(EPOCH FROM (processed_at - created_at))) * 1000)::numeric, 0) as avg_latency_ms
FROM outbox_events
WHERE saga_id LIKE 'perf-batch50-${TIMESTAMP}%';
"

echo ""
echo "============================================================"
echo "테스트 완료"
echo "============================================================"
echo "종료 시간: $(date '+%Y-%m-%d %H:%M:%S')"
