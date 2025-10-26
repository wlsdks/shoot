#!/bin/bash

# 부하 테스트: 100+ events/sec

PGPASSWORD=1234
PGHOST=localhost
PGPORT=5432
PGUSER=root
PGDATABASE=member

echo "============================================================"
echo "부하 테스트: 100+ events/sec"
echo "============================================================"
echo "시작 시간: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

# 테스트 설정
EVENTS_PER_SECOND=100
DURATION_SECONDS=10
TOTAL_EVENTS=$((EVENTS_PER_SECOND * DURATION_SECONDS))

TIMESTAMP=$(date +%s)

echo "설정:"
echo "  - 초당 이벤트: ${EVENTS_PER_SECOND}"
echo "  - 테스트 시간: ${DURATION_SECONDS}초"
echo "  - 총 이벤트: ${TOTAL_EVENTS}개"
echo ""

echo "이벤트 삽입 시작..."
START_TIME=$(date +%s%3N)

# 이벤트 병렬 삽입
for i in $(seq 1 $TOTAL_EVENTS); do
    SAGA_ID="load-test-${TIMESTAMP}-${i}"

    # 백그라운드로 병렬 삽입
    (
        PGPASSWORD=$PGPASSWORD psql -h $PGHOST -p $PGPORT -U $PGUSER -d $PGDATABASE -q -c "
        INSERT INTO outbox_events (
            saga_id, saga_state, event_type, payload,
            processed, retry_count, created_at, idempotency_key
        ) VALUES (
            '$SAGA_ID',
            'STARTED',
            'com.stark.shoot.domain.event.MessageSentEvent',
            '{\"message\":{\"id\":null,\"roomId\":1,\"senderId\":1,\"content\":{\"text\":\"Load test $i\",\"type\":\"TEXT\",\"isDeleted\":false,\"isEdited\":false},\"status\":\"SENT\",\"replyToMessageId\":null,\"threadId\":null,\"expiresAt\":null,\"messageReactions\":{\"reactions\":{}},\"mentions\":[],\"createdAt\":\"2025-10-26T04:00:00Z\",\"updatedAt\":null,\"readBy\":{},\"metadata\":{},\"isPinned\":false,\"pinnedBy\":null,\"pinnedAt\":null},\"occurredOn\":1761456000000}',
            false,
            0,
            NOW(),
            '$SAGA_ID-MessageSentEvent'
        );" 2>/dev/null
    ) &

    # 진행 상황 표시
    if [ $((i % 100)) -eq 0 ]; then
        echo "  - $i/${TOTAL_EVENTS} 삽입 완료"
    fi

    # 속도 조절 (100 events/sec = 10ms per event)
    sleep 0.01
done

# 모든 백그라운드 작업 완료 대기
wait

END_TIME=$(date +%s%3N)
ELAPSED=$((END_TIME - START_TIME))
ELAPSED_SEC=$(echo "scale=2; $ELAPSED / 1000" | bc)
ACTUAL_RATE=$(echo "scale=1; $TOTAL_EVENTS / $ELAPSED_SEC" | bc)

echo ""
echo "삽입 완료:"
echo "  - 소요 시간: ${ELAPSED_SEC}초"
echo "  - 실제 처리량: ${ACTUAL_RATE} events/sec"
echo ""

echo "처리 대기 중 (15초)..."
sleep 15

echo ""
echo "============================================================"
echo "통계 분석"
echo "============================================================"

# 전체 통계
echo ""
echo "[ 전체 통계 ]"
PGPASSWORD=$PGPASSWORD psql -h $PGHOST -p $PGPORT -U $PGUSER -d $PGDATABASE -c "
SELECT
    COUNT(*) as total_events,
    COUNT(CASE WHEN processed THEN 1 END) as processed_events,
    ROUND((COUNT(CASE WHEN processed THEN 1 END)::numeric / COUNT(*)::numeric * 100)::numeric, 1) as processing_rate,
    ROUND(AVG(EXTRACT(EPOCH FROM (processed_at - created_at)))::numeric, 3) as avg_latency_sec,
    ROUND(MIN(EXTRACT(EPOCH FROM (processed_at - created_at)))::numeric, 3) as min_latency_sec,
    ROUND(MAX(EXTRACT(EPOCH FROM (processed_at - created_at)))::numeric, 3) as max_latency_sec,
    ROUND((AVG(EXTRACT(EPOCH FROM (processed_at - created_at))) * 1000)::numeric, 0) as avg_latency_ms
FROM outbox_events
WHERE saga_id LIKE 'load-test-${TIMESTAMP}%';
"

# Percentile 통계
echo ""
echo "[ Percentile 분석 ]"
PGPASSWORD=$PGPASSWORD psql -h $PGHOST -p $PGPORT -U $PGUSER -d $PGDATABASE -c "
WITH latencies AS (
    SELECT
        EXTRACT(EPOCH FROM (processed_at - created_at)) * 1000 as latency_ms
    FROM outbox_events
    WHERE saga_id LIKE 'load-test-${TIMESTAMP}%'
      AND processed = true
    ORDER BY latency_ms
)
SELECT
    ROUND(PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY latency_ms)::numeric, 0) as p50_ms,
    ROUND(PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY latency_ms)::numeric, 0) as p75_ms,
    ROUND(PERCENTILE_CONT(0.90) WITHIN GROUP (ORDER BY latency_ms)::numeric, 0) as p90_ms,
    ROUND(PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY latency_ms)::numeric, 0) as p95_ms,
    ROUND(PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY latency_ms)::numeric, 0) as p99_ms
FROM latencies;
"

# 시간대별 처리 분석
echo ""
echo "[ 시간대별 처리 분포 ]"
PGPASSWORD=$PGPASSWORD psql -h $PGHOST -p $PGPORT -U $PGUSER -d $PGDATABASE -c "
WITH time_buckets AS (
    SELECT
        WIDTH_BUCKET(EXTRACT(EPOCH FROM (processed_at - created_at)) * 1000, 0, 5000, 10) as bucket,
        COUNT(*) as cnt
    FROM outbox_events
    WHERE saga_id LIKE 'load-test-${TIMESTAMP}%'
      AND processed = true
    GROUP BY bucket
    ORDER BY bucket
)
SELECT
    CASE
        WHEN bucket = 1 THEN '0-500ms'
        WHEN bucket = 2 THEN '500-1000ms'
        WHEN bucket = 3 THEN '1-1.5s'
        WHEN bucket = 4 THEN '1.5-2s'
        WHEN bucket = 5 THEN '2-2.5s'
        WHEN bucket = 6 THEN '2.5-3s'
        WHEN bucket = 7 THEN '3-3.5s'
        WHEN bucket = 8 THEN '3.5-4s'
        WHEN bucket = 9 THEN '4-4.5s'
        WHEN bucket = 10 THEN '4.5-5s'
        ELSE '5s+'
    END as latency_range,
    cnt as event_count,
    LPAD(REPEAT('█', (cnt * 50 / GREATEST(MAX(cnt) OVER (), 1))::int), (cnt * 50 / GREATEST(MAX(cnt) OVER (), 1))::int, '█') as distribution
FROM time_buckets;
"

echo ""
echo "============================================================"
echo "부하 테스트 완료"
echo "============================================================"
echo "종료 시간: $(date '+%Y-%m-%d %H:%M:%S')"
