# CDC (Change Data Capture) 설정 가이드

## 개요

Debezium + Kafka Connect를 사용하여 PostgreSQL Outbox 테이블의 변경사항을 실시간으로 Kafka에 발행합니다.

## 아키텍처

```
[Message 저장] → [Outbox 저장] ──┬→ [OutboxEventProcessor] (백업/재시도)
                   (트랜잭션)    │     폴링: 5초마다
                                 │
                                 └→ [Debezium CDC] → [Kafka] (실시간)
                                        WAL 로그 읽기
                                        지연: <100ms
```

### 장점
- ✅ **실시간 이벤트 발행** (밀리초 단위 지연)
- ✅ **DB 부하 없음** (WAL 로그 읽기, 폴링 불필요)
- ✅ **이중화** (OutboxEventProcessor가 백업 역할)
- ✅ **At-least-once 보장** (CDC + 백업 폴링)

### 구성 요소

1. **PostgreSQL** (WAL enabled)
   - `wal_level=logical`
   - Publication: `outbox_publication`
   - Replication Slot: `shoot_outbox_slot`

2. **Kafka Connect**
   - Debezium PostgreSQL Connector
   - Outbox Event Router Transform
   - 포트: 8083 (REST API)

3. **Debezium UI** (선택사항)
   - Connector 모니터링
   - 포트: 8084

## 시작하기

### 1. 전제 조건

- Docker & Docker Compose 설치
- Kafka 클러스터 실행 중 (`docker-compose-kafka.yml`)

### 2. CDC 인프라 시작

```bash
# CDC 전용 PostgreSQL + Kafka Connect 시작
docker-compose -f docker-compose-cdc.yml up -d

# 로그 확인
docker-compose -f docker-compose-cdc.yml logs -f
```

### 3. Debezium Connector 등록

```bash
# Connector 등록 (Kafka Connect가 완전히 시작된 후)
./docker/debezium/register-connector.sh
```

또는 수동 등록:

```bash
curl -X POST \
  -H "Content-Type: application/json" \
  --data @docker/debezium/outbox-connector.json \
  http://localhost:8083/connectors
```

### 4. 상태 확인

```bash
# Connector 상태
curl http://localhost:8083/connectors/shoot-outbox-connector/status | jq .

# 등록된 Connector 목록
curl http://localhost:8083/connectors | jq .

# Kafka 토픽 확인 (Kafka UI)
open http://localhost:8085
```

## Debezium Outbox Pattern

### 동작 방식

1. **Outbox 테이블에 이벤트 저장** (트랜잭션 내)
   ```sql
   INSERT INTO outbox_events (saga_id, event_type, payload, ...)
   ```

2. **Debezium이 WAL에서 변경 감지**
   - PostgreSQL WAL (Write-Ahead Log) 읽기
   - `outbox_events` 테이블의 INSERT 감지

3. **Outbox Event Router Transform 적용**
   - 이벤트 타입별로 다른 Kafka 토픽으로 라우팅
   - 예: `MessageSentEvent` → `shoot.events.MessageSentEvent` 토픽

4. **Kafka에 이벤트 발행**
   - Key: `saga_id`
   - Value: `payload` (JSON)
   - Headers: `sagaId`, `eventType`

### Connector 설정 주요 항목

```json
{
  "transforms": "outbox",
  "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
  "transforms.outbox.table.field.event.type": "event_type",
  "transforms.outbox.table.field.event.payload": "payload",
  "transforms.outbox.route.topic.replacement": "shoot.events.${routedByValue}"
}
```

### 토픽 라우팅

| 이벤트 타입 | Kafka 토픽 |
|-----------|-----------|
| `MessageSentEvent` | `shoot.events.MessageSentEvent` |
| `MentionEvent` | `shoot.events.MentionEvent` |
| `ChatRoomCreatedEvent` | `shoot.events.ChatRoomCreatedEvent` |

## 모니터링

### Debezium UI

```
http://localhost:8084
```

- Connector 상태 확인
- 메트릭 및 로그 확인
- Connector 재시작

### Kafka UI

```
http://localhost:8085
```

- 토픽별 메시지 확인
- Consumer lag 모니터링
- 파티션 분포 확인

### Kafka Connect REST API

```bash
# Connector 상태
curl http://localhost:8083/connectors/shoot-outbox-connector/status

# Connector 재시작
curl -X POST http://localhost:8083/connectors/shoot-outbox-connector/restart

# Connector 일시 중지
curl -X PUT http://localhost:8083/connectors/shoot-outbox-connector/pause

# Connector 재개
curl -X PUT http://localhost:8083/connectors/shoot-outbox-connector/resume

# Connector 삭제
curl -X DELETE http://localhost:8083/connectors/shoot-outbox-connector
```

## OutboxEventProcessor 백업 모드

CDC가 실행 중일 때도 `OutboxEventProcessor`는 계속 동작합니다:

1. **정상 상태**: CDC가 밀리초 단위로 이벤트 발행
2. **CDC 장애 시**: OutboxEventProcessor가 5초마다 폴링하여 발행
3. **중복 방지**: Kafka의 idempotency 설정으로 중복 메시지 방지

### 설정

```kotlin
// OutboxEventProcessor는 계속 동작하지만
// CDC가 처리한 이벤트는 이미 processed=true이므로 스킵됨
@Scheduled(fixedDelay = 5000, initialDelay = 10000)
fun processOutboxEvents() {
    val unprocessedEvents = outboxEventRepository
        .findByProcessedFalseOrderByCreatedAtAsc()
    // CDC가 처리 못한 이벤트만 발행
}
```

## 트러블슈팅

### 1. Connector가 시작되지 않음

**증상**:
```
"state": "FAILED"
```

**해결**:
```bash
# 로그 확인
docker logs shoot-kafka-connect

# PostgreSQL publication 확인
docker exec -it shoot-postgres-cdc psql -U root -d member -c \
  "SELECT * FROM pg_publication WHERE pubname='outbox_publication';"

# Replication slot 확인
docker exec -it shoot-postgres-cdc psql -U root -d member -c \
  "SELECT * FROM pg_replication_slots;"
```

### 2. 이벤트가 Kafka에 발행되지 않음

**확인 사항**:
1. Outbox 테이블에 새 이벤트가 있는지
2. Connector 상태가 RUNNING인지
3. Kafka 토픽이 생성되었는지

```bash
# Connector 상태
curl http://localhost:8083/connectors/shoot-outbox-connector/status

# Kafka 토픽 목록 (Kafka UI 또는)
docker exec -it shoot-Kafka00Container kafka-topics.sh \
  --bootstrap-server localhost:9092 --list
```

### 3. Replication Slot이 가득 참

**증상**:
```
ERROR: replication slot "shoot_outbox_slot" is active for PID ...
```

**해결**:
```bash
# Replication slot 삭제
docker exec -it shoot-postgres-cdc psql -U root -d member -c \
  "SELECT pg_drop_replication_slot('shoot_outbox_slot');"

# Connector 재시작
curl -X POST http://localhost:8083/connectors/shoot-outbox-connector/restart
```

## 성능 최적화

### Connector 설정

```json
{
  "max.queue.size": "8192",
  "max.batch.size": "2048",
  "poll.interval.ms": "100",
  "heartbeat.interval.ms": "5000"
}
```

### PostgreSQL WAL 설정

```sql
-- WAL 보관 크기 증가 (높은 처리량 시)
ALTER SYSTEM SET max_wal_size = '2GB';
ALTER SYSTEM SET min_wal_size = '80MB';
SELECT pg_reload_conf();
```

## 비용 vs 효과

| 항목 | Outbox Only | Outbox + CDC |
|-----|------------|-------------|
| 평균 지연 | 2.5초 | 50ms |
| DB 부하 | 높음 (폴링) | 낮음 (WAL) |
| 인프라 복잡도 | 낮음 | 중간 |
| 운영 난이도 | 낮음 | 중간 |
| 이중화 | 없음 | 있음 (폴링 백업) |

## 참고 자료

- [Debezium Documentation](https://debezium.io/documentation/reference/stable/)
- [Outbox Pattern](https://microservices.io/patterns/data/transactional-outbox.html)
- [Kafka Connect](https://docs.confluent.io/platform/current/connect/index.html)
- [PostgreSQL Logical Replication](https://www.postgresql.org/docs/current/logical-replication.html)

---

**다음 단계**: CDC 이벤트를 소비하는 Kafka Consumer 구현 → `CDCEventConsumer.kt`
