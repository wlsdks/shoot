# CDC 인프라 테스트 가이드

> Debezium + Kafka Connect CDC 풀스택 테스트

## 사전 준비

### 1. Docker Desktop 시작

```bash
# Docker Desktop 실행 확인
docker info

# Docker가 실행되지 않은 경우
open -a Docker  # macOS
```

### 2. 기존 컨테이너 정리 (선택)

```bash
# 기존 Kafka 컨테이너 확인
docker ps -a | grep kafka

# 필요시 정리
docker-compose -f docker-compose-kafka.yml down
```

## 테스트 시나리오

### 시나리오 1: CDC 전체 스택 테스트

#### Step 1: CDC 인프라 시작

```bash
# PostgreSQL + Kafka Connect + Debezium UI 시작
docker-compose -f docker-compose-cdc.yml up -d

# 로그 확인
docker-compose -f docker-compose-cdc.yml logs -f
```

**예상 결과:**
```
shoot-postgres-cdc     | ready to accept connections
shoot-kafka-connect    | Kafka Connect started
shoot-debezium-ui      | Debezium UI listening on port 8084
```

#### Step 2: Kafka Connect 준비 대기

```bash
# Kafka Connect 헬스 체크
curl http://localhost:8083/

# 예상 응답:
# {"version":"3.7.0","commit":"..."}
```

#### Step 3: Debezium Connector 등록

```bash
# 자동 등록 스크립트 실행
./docker/debezium/register-connector.sh

# 또는 수동 등록
curl -X POST \
  -H "Content-Type: application/json" \
  --data @docker/debezium/outbox-connector.json \
  http://localhost:8083/connectors
```

**예상 결과:**
```json
{
  "name": "shoot-outbox-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    ...
  },
  "tasks": [],
  "type": "source"
}
```

#### Step 4: Connector 상태 확인

```bash
# Connector 상태
curl http://localhost:8083/connectors/shoot-outbox-connector/status | jq .

# 예상 결과:
# {
#   "name": "shoot-outbox-connector",
#   "connector": {
#     "state": "RUNNING",
#     "worker_id": "kafka-connect:8083"
#   },
#   "tasks": [
#     {
#       "id": 0,
#       "state": "RUNNING",
#       "worker_id": "kafka-connect:8083"
#     }
#   ]
# }
```

#### Step 5: PostgreSQL Publication 확인

```bash
# PostgreSQL 컨테이너 접속
docker exec -it shoot-postgres-cdc psql -U root -d member

# Publication 확인
SELECT * FROM pg_publication WHERE pubname = 'outbox_publication';

# Publication 테이블 확인
SELECT * FROM pg_publication_tables WHERE pubname = 'outbox_publication';

# 예상 결과:
#  pubname          | schemaname | tablename
# ------------------+------------+-------------
#  outbox_publication | public     | outbox_events

# WAL Level 확인
SHOW wal_level;
# 예상: logical

# Replication Slot 확인
SELECT * FROM pg_replication_slots;
# slot_name: shoot_outbox_slot
```

#### Step 6: Spring Boot 애플리케이션 시작

```bash
# 애플리케이션 시작
./gradlew bootRun

# 또는 JAR 실행
java -jar build/libs/shoot-0.0.1-SNAPSHOT.jar
```

**로그 확인:**
```
Flyway migration started...
✓ V5__cdc_setup.sql applied successfully
CDC Publication created: outbox_publication

Starting Shoot Application...
```

#### Step 7: 이벤트 발행 테스트

**방법 1: API 호출**

```bash
# 메시지 전송 (Saga 시작)
curl -X POST http://localhost:8100/api/chat/messages \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "roomId": 1,
    "content": "CDC Test Message",
    "type": "TEXT"
  }'
```

**방법 2: PostgreSQL 직접 삽입**

```bash
docker exec -it shoot-postgres-cdc psql -U root -d member

INSERT INTO outbox_events (
    saga_id, idempotency_key, saga_state, event_type, payload, processed
) VALUES (
    'test-saga-001',
    'test-saga-001-MessageSentEvent',
    'IN_PROGRESS',
    'com.stark.shoot.domain.event.MessageSentEvent',
    '{"messageId":"msg-001","content":"CDC Test"}',
    false
);
```

#### Step 8: CDC 동작 확인

**1. Kafka 토픽 확인**

```bash
# Kafka UI 접속
open http://localhost:8085

# 토픽 검색:
# - shoot.events.MessageSentEvent
# - shoot.heartbeat
```

**2. 애플리케이션 로그**

```
[CDCEventConsumer] CDC 이벤트 수신:
  topic=shoot.events.MessageSentEvent,
  sagaId=test-saga-001,
  eventType=MessageSentEvent

[CDCEventConsumer] CDC 이벤트 처리 완료
[OutboxEventProcessor] No outbox events to process  # CDC가 이미 처리함!
```

**3. Outbox 테이블 확인**

```sql
SELECT id, saga_id, processed, processed_at
FROM outbox_events
WHERE saga_id = 'test-saga-001';

-- processed = true  (CDC가 처리함)
```

### 시나리오 2: CDC 장애 시 백업 메커니즘 테스트

#### Step 1: CDC Connector 중지

```bash
# Connector 일시 중지
curl -X PUT http://localhost:8083/connectors/shoot-outbox-connector/pause

# 상태 확인
curl http://localhost:8083/connectors/shoot-outbox-connector/status | jq '.connector.state'
# "PAUSED"
```

#### Step 2: 이벤트 발행

```sql
INSERT INTO outbox_events (
    saga_id, idempotency_key, saga_state, event_type, payload, processed
) VALUES (
    'test-saga-002',
    'test-saga-002-MessageSentEvent',
    'IN_PROGRESS',
    'com.stark.shoot.domain.event.MessageSentEvent',
    '{"messageId":"msg-002","content":"Backup Test"}',
    false
);
```

#### Step 3: OutboxEventProcessor 동작 확인

**5초 후 로그:**

```
[OutboxEventProcessor] Processing 1 outbox events
[OutboxEventProcessor] Outbox event processed: id=XXX, type=MessageSentEvent
```

**Outbox 확인:**

```sql
SELECT processed, processed_at
FROM outbox_events
WHERE saga_id = 'test-saga-002';

-- processed = true  (OutboxEventProcessor가 처리함)
```

#### Step 4: CDC 재개

```bash
# Connector 재개
curl -X PUT http://localhost:8083/connectors/shoot-outbox-connector/resume

# 상태 확인
curl http://localhost:8083/connectors/shoot-outbox-connector/status | jq '.connector.state'
# "RUNNING"
```

### 시나리오 3: DLQ 테스트

#### Step 1: 실패하는 이벤트 생성

```sql
-- 잘못된 JSON 페이로드 (역직렬화 실패 유도)
INSERT INTO outbox_events (
    saga_id, idempotency_key, saga_state, event_type, payload, processed
) VALUES (
    'test-saga-fail',
    'test-saga-fail-InvalidEvent',
    'IN_PROGRESS',
    'com.stark.shoot.domain.event.NonExistentEvent',  -- 존재하지 않는 클래스
    '{"invalid":"data"}',
    false
);
```

#### Step 2: 재시도 로그 확인

```
[OutboxEventProcessor] Failed to process outbox event: id=XXX
[OutboxEventProcessor] OptimisticLockException occurred, retrying... (attempt 1/5)
[OutboxEventProcessor] OptimisticLockException occurred, retrying... (attempt 2/5)
...
[OutboxEventProcessor] OptimisticLockException occurred, retrying... (attempt 5/5)
```

#### Step 3: DLQ 이동 확인

```
[OutboxEventProcessor] 이벤트를 DLQ로 이동:
  outboxId=XXX, sagaId=test-saga-fail,
  reason=ClassNotFoundException: com.stark.shoot.domain.event.NonExistentEvent

[SlackWebhookAdapter] Slack 알림 전송 (또는 No-Op 로그)
```

**DLQ 테이블 확인:**

```sql
SELECT * FROM outbox_dead_letter_queue
WHERE saga_id = 'test-saga-fail';

-- resolved = false
-- failure_reason: ClassNotFoundException...
```

#### Step 4: DLQ 해결

```bash
# Admin API로 해결 (ADMIN 권한 필요)
curl -X POST http://localhost:8100/api/admin/outbox-dlq/1/resolve \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN" \
  -d '{
    "resolvedBy": "admin@example.com",
    "note": "수동으로 Kafka에 재발행함"
  }'
```

## 모니터링 UI

### Debezium UI
```
http://localhost:8084
```

- Connector 상태 실시간 모니터링
- 메트릭 확인
- 로그 확인

### Kafka UI
```
http://localhost:8085
```

- 토픽별 메시지 확인
- Consumer lag 모니터링
- 파티션 분포

### Kafka Connect REST API
```bash
# 등록된 Connector 목록
curl http://localhost:8083/connectors | jq .

# Connector 상세 정보
curl http://localhost:8083/connectors/shoot-outbox-connector | jq .

# Connector 설정
curl http://localhost:8083/connectors/shoot-outbox-connector/config | jq .

# Connector 재시작
curl -X POST http://localhost:8083/connectors/shoot-outbox-connector/restart

# Connector 삭제
curl -X DELETE http://localhost:8083/connectors/shoot-outbox-connector
```

## 성능 측정

### CDC 지연 시간 측정

```sql
-- Outbox 이벤트 생성 시간과 처리 시간 비교
SELECT
    saga_id,
    created_at,
    processed_at,
    EXTRACT(EPOCH FROM (processed_at - created_at)) * 1000 as latency_ms
FROM outbox_events
WHERE processed = true
ORDER BY created_at DESC
LIMIT 10;

-- 예상: CDC는 50~100ms, 백업 폴링은 5000ms
```

### Throughput 측정

```bash
# 대량 이벤트 생성 스크립트
for i in {1..100}; do
  docker exec shoot-postgres-cdc psql -U root -d member -c "
    INSERT INTO outbox_events (saga_id, idempotency_key, saga_state, event_type, payload, processed)
    VALUES ('load-test-$i', 'load-test-$i-Event', 'IN_PROGRESS',
            'com.stark.shoot.domain.event.MessageSentEvent',
            '{\"test\":\"data\"}', false);
  "
done

# 처리 시간 확인
SELECT COUNT(*), AVG(EXTRACT(EPOCH FROM (processed_at - created_at)) * 1000) as avg_latency_ms
FROM outbox_events
WHERE saga_id LIKE 'load-test-%';
```

## 트러블슈팅

### 1. Connector가 시작되지 않음

```bash
# Kafka Connect 로그 확인
docker logs shoot-kafka-connect --tail 100

# Publication 확인
docker exec shoot-postgres-cdc psql -U root -d member -c \
  "SELECT * FROM pg_publication WHERE pubname = 'outbox_publication';"

# WAL Level 확인
docker exec shoot-postgres-cdc psql -U root -d member -c "SHOW wal_level;"
```

### 2. 이벤트가 Kafka에 발행되지 않음

```bash
# Replication Slot 상태
docker exec shoot-postgres-cdc psql -U root -d member -c \
  "SELECT * FROM pg_replication_slots;"

# Connector Tasks 상태
curl http://localhost:8083/connectors/shoot-outbox-connector/tasks/0/status | jq .
```

### 3. Replication Slot이 가득 참

```bash
# Replication Slot 삭제
docker exec shoot-postgres-cdc psql -U root -d member -c \
  "SELECT pg_drop_replication_slot('shoot_outbox_slot');"

# Connector 재시작
curl -X POST http://localhost:8083/connectors/shoot-outbox-connector/restart
```

## 클린업

```bash
# CDC 인프라 중지
docker-compose -f docker-compose-cdc.yml down

# 볼륨 포함 완전 삭제
docker-compose -f docker-compose-cdc.yml down -v

# 네트워크 정리
docker network prune
```

## 검증 체크리스트

- [ ] Docker Desktop 실행 중
- [ ] PostgreSQL WAL level=logical
- [ ] Publication 생성됨
- [ ] Kafka Connect 실행 중 (port 8083)
- [ ] Debezium Connector RUNNING 상태
- [ ] Replication Slot 생성됨
- [ ] 이벤트 발행 시 Kafka 토픽 생성
- [ ] CDC 처리 시간 < 100ms
- [ ] CDC 장애 시 백업 폴링 동작
- [ ] DLQ 이동 및 Slack 알림
- [ ] Admin API 보안 (ADMIN 권한)

---

**다음 단계**: 모든 테스트 통과 후 프로덕션 배포 준비
