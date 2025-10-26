#!/bin/bash

TIMESTAMP=$(date +%s)

echo "=========================================="
echo "최종 최적화 후 성능 테스트"
echo "=========================================="
echo ""
echo "적용된 최적화:"
echo "  PostgreSQL:"
echo "    - wal_writer_delay: 10ms"
echo "    - synchronous_commit: off"
echo "  Debezium:"
echo "    - poll.interval.ms: 50ms"
echo "    - heartbeat.interval.ms: 500ms"
echo "    - max.batch.size: 4096"
echo ""
echo "시작: $(date '+%H:%M:%S')"
echo ""

for i in {1..15}; do
    SAGA_ID="final-${TIMESTAMP}-${i}"

    PGPASSWORD=1234 psql -h localhost -p 5432 -U root -d member -q -c "
    INSERT INTO outbox_events (
        saga_id, saga_state, event_type, payload,
        processed, retry_count, created_at, idempotency_key
    ) VALUES (
        '$SAGA_ID',
        'STARTED',
        'com.stark.shoot.domain.event.MessageSentEvent',
        '{\"message\":{\"id\":null,\"roomId\":1,\"senderId\":1,\"content\":{\"text\":\"Final test $i\",\"type\":\"TEXT\",\"isDeleted\":false,\"isEdited\":false},\"status\":\"SENT\",\"replyToMessageId\":null,\"threadId\":null,\"expiresAt\":null,\"messageReactions\":{\"reactions\":{}},\"mentions\":[],\"createdAt\":\"2025-10-26T04:00:00Z\",\"updatedAt\":null,\"readBy\":{},\"metadata\":{},\"isPinned\":false,\"pinnedBy\":null,\"pinnedAt\":null},\"occurredOn\":1761456000000}',
        false,
        0,
        NOW(),
        '$SAGA_ID-MessageSentEvent'
    );" 2>/dev/null

    echo "[$i/15] 삽입 완료"
    sleep 0.3
done

echo ""
echo "처리 대기 중 (5초)..."
sleep 5

echo ""
echo "=========================================="
echo "결과 분석"
echo "=========================================="

PGPASSWORD=1234 psql -h localhost -p 5432 -U root -d member -c "
SELECT
    COUNT(*) as total,
    COUNT(CASE WHEN processed THEN 1 END) as processed,
    ROUND(AVG(EXTRACT(EPOCH FROM (processed_at - created_at)) * 1000)::numeric, 0) as avg_ms,
    ROUND(MIN(EXTRACT(EPOCH FROM (processed_at - created_at)) * 1000)::numeric, 0) as min_ms,
    ROUND(MAX(EXTRACT(EPOCH FROM (processed_at - created_at)) * 1000)::numeric, 0) as max_ms,
    ROUND(PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (processed_at - created_at)) * 1000)::numeric, 0) as p50_ms,
    ROUND(PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (processed_at - created_at)) * 1000)::numeric, 0) as p95_ms
FROM outbox_events
WHERE saga_id LIKE 'final-${TIMESTAMP}%'
  AND processed = true;
"

echo ""
echo "처리 시간 분포:"
PGPASSWORD=1234 psql -h localhost -p 5432 -U root -d member -c "
SELECT
    ROUND((EXTRACT(EPOCH FROM (processed_at - created_at)) * 1000)::numeric, 0) as latency_ms,
    TO_CHAR(created_at, 'HH24:MI:SS.MS') as created,
    TO_CHAR(processed_at, 'HH24:MI:SS.MS') as processed
FROM outbox_events
WHERE saga_id LIKE 'final-${TIMESTAMP}%'
  AND processed = true
ORDER BY id;
"

echo ""
echo "완료: $(date '+%H:%M:%S')"
