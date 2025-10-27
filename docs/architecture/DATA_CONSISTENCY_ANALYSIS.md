# MongoDB ↔ PostgreSQL 데이터 정합성 분석

## 개요

이 문서는 Shoot 채팅 애플리케이션의 MongoDB와 PostgreSQL 간 데이터 정합성 전략을 분석하고, 잠재적인 이슈와 개선 방안을 제시합니다.

## 현재 아키텍처

### 데이터베이스 역할 분리
- **PostgreSQL**: Users, Friends, ChatRooms(메타데이터), Notifications, Outbox Events
- **MongoDB**: Chat Messages, Reactions

### 정합성 보장 메커니즘
1. **Saga Pattern**: 분산 트랜잭션 관리 (MessageSagaOrchestrator)
2. **Outbox Pattern**: 이벤트 발행 신뢰성 보장
3. **CDC (Debezium)**: PostgreSQL WAL 기반 이벤트 스트리밍
4. **Polling Backup**: OutboxEventProcessor (5초마다 실행)

## 메시지 생성 플로우 (잘 구현됨 ✅)

```
Client Request
    ↓
SendMessageService
    ↓
Kafka Producer → chat-messages topic
    ↓
HandleMessageEventService (Kafka Consumer)
    ↓
MessageSagaOrchestrator
    ├─ Step 1: SaveMessageToMongoStep
    │   └─ MongoDB.save(message)
    ├─ Step 2: UpdateChatRoomMetadataStep
    │   └─ PostgreSQL.update(chatRoom) [@Transactional START]
    └─ Step 3: PublishEventToOutboxStep
        └─ PostgreSQL.insert(outbox_events) [@Transactional MANDATORY]
            ↓
        [COMMIT] ← Step 2 & 3 원자적 커밋
            ↓
        ┌────────────────┐
        │ CDC (Primary)  │
        └────────────────┘
        Debezium → Kafka → CDCEventConsumer → EventPublisher

        ┌───────────────────────┐
        │ Polling (Backup)      │
        └───────────────────────┘
        OutboxEventProcessor (every 5s) → EventPublisher
```

**강점:**
- MongoDB 실패 시 보상 트랜잭션으로 롤백
- PostgreSQL 실패 시 Step 3 미실행으로 이벤트 미발행
- Outbox + CDC로 이벤트 발행 신뢰성 보장
- Idempotency Key (sagaId-eventType)로 중복 방지

## 심각한 이슈 (P0)

### P0-1: Edit/Delete/Reaction/Pin 작업이 Outbox를 우회 ❌

**영향받는 파일:**
- `EditMessageService.kt:49`
- `DeleteMessageService.kt:45`
- `ToggleMessageReactionService.kt:81`
- `MessagePinService.kt:60,90`

**문제:**
```kotlin
// 현재 코드 (잘못됨)
@Transactional  // PostgreSQL 트랜잭션 (하지만 PostgreSQL 쓰기 없음!)
override fun editMessage(command: EditMessageCommand): ChatMessage {
    // MongoDB 직접 쓰기
    val savedMessage = messageCommandPort.save(updatedMessage)

    // 인메모리 이벤트 발행 (신뢰성 없음!)
    publishMessageEditedEvent(savedMessage, ...)

    return savedMessage
}
```

**위험:**
- MongoDB 쓰기 성공 후 이벤트 발행 실패 시 알림 손실
- 감사 로그 누락
- 분석 데이터 불완전

**해결 방법:**

**Option 1: PostgreSQL 감사 로그 테이블 추가 (권장)**
```kotlin
@Transactional
override fun editMessage(command: EditMessageCommand): ChatMessage {
    // 1. MongoDB 쓰기
    val savedMessage = messageCommandPort.save(updatedMessage)

    // 2. PostgreSQL 감사 로그 쓰기 (트랜잭션 컨텍스트 제공)
    auditLogRepository.save(AuditLogEntity(
        entityType = "MESSAGE",
        entityId = savedMessage.id.value,
        action = "EDIT",
        userId = command.userId.value
    ))

    // 3. Outbox에 이벤트 저장 (같은 트랜잭션)
    outboxEventRepository.save(OutboxEventEntity(
        sagaId = "EDIT-${savedMessage.id.value}",
        idempotencyKey = "EDIT-${savedMessage.id.value}-${System.currentTimeMillis()}",
        eventType = "MessageEditedEvent",
        payload = objectMapper.writeValueAsString(MessageEditedEvent.create(...))
    ))

    return savedMessage
}
```

**Option 2: 별도 Outbox 테이블 (MongoDB용)**
- MongoDB 컬렉션에 `outbox_events` 추가
- 별도 폴링 프로세서로 처리
- 더 복잡하지만 PostgreSQL 의존성 제거

### P0-2: 알림 생성이 Outbox를 우회하고 재시도 없음 ❌

**영향받는 파일:**
- `ChatEventNotificationListener.kt:163-179`

**문제:**
```kotlin
@TransactionalEventListener
private fun processNotifications(notifications: List<Notification>, roomId: Long) {
    try {
        // MongoDB 직접 쓰기 (재시도 없음!)
        val savedNotifications = notificationCommandPort.saveNotifications(notifications)
        sendNotificationPort.sendNotifications(savedNotifications)
    } catch (e: Exception) {
        logger.error(e) { "Failed to process notifications" }
        // 예외 삼켜짐! 사용자는 알림 못 받음
    }
}
```

**위험:**
- MongoDB 일시적 장애 시 알림 영구 손실
- 재시도 메커니즘 없음
- DLQ 없음

**해결 방법:**
```kotlin
@Retryable(
    value = [MongoException::class],
    maxAttempts = 3,
    backoff = Backoff(delay = 1000, multiplier = 2.0)
)
@Recover
private fun processNotifications(notifications: List<Notification>, roomId: Long) {
    try {
        val savedNotifications = notificationCommandPort.saveNotifications(notifications)
        sendNotificationPort.sendNotifications(savedNotifications)
    } catch (e: Exception) {
        logger.error(e) { "Failed after 3 retries" }
        throw e  // Recover 메서드로 전달
    }
}

@Recover
fun recoverProcessNotifications(e: MongoException, notifications: List<Notification>, roomId: Long) {
    logger.error { "Moving to DLQ for roomId=$roomId" }
    notificationDLQRepository.saveAll(notifications.map {
        NotificationDLQ(it, e.message, failedAt = Instant.now())
    })
    // Slack 알림 전송
    slackNotificationPort.notifyNotificationFailure(roomId, notifications.size)
}
```

## 중요한 이슈 (P1)

### P1-1: Idempotency Check가 사용되지 않음 ⚠️

**영향받는 파일:**
- `PublishEventToOutboxStep.kt:78-88`

**문제:**
```kotlin
private fun saveToOutbox(sagaId: String, event: Any) {
    val idempotencyKey = "$sagaId-$eventTypeName"

    // existsByIdempotencyKey() 메서드가 있지만 사용하지 않음!
    outboxEventRepository.save(OutboxEventEntity(...))
    // 중복 시 DataIntegrityViolationException 발생
    // Saga 실패로 이어짐
}
```

**해결 방법:**
```kotlin
private fun saveToOutbox(sagaId: String, event: Any) {
    val idempotencyKey = "$sagaId-$eventTypeName"

    // Idempotency check 추가
    if (outboxEventRepository.existsByIdempotencyKey(idempotencyKey)) {
        logger.info { "Outbox event already exists: $idempotencyKey" }
        return  // 중복 방지
    }

    try {
        outboxEventRepository.save(OutboxEventEntity(...))
    } catch (e: DataIntegrityViolationException) {
        // Race condition: check와 save 사이에 다른 스레드가 생성
        logger.warn { "Duplicate caught by DB constraint: $idempotencyKey" }
        // 정상 처리 (이벤트는 이미 저장됨)
    }
}
```

### P1-2: Optimistic Lock 재시도가 같은 트랜잭션 내에서 실행 ⚠️

**영향받는 파일:**
- `UpdateChatRoomMetadataStep.kt:65-102`

**문제:**
```kotlin
@Transactional
override fun execute(context: MessageSagaContext): Boolean {
    var attempt = 0
    while (attempt < MAX_RETRIES) {
        try {
            return executeInternal(context, savedMessage)
        } catch (e: OptimisticLockException) {
            attempt++
            Thread.sleep(calculateBackoff(attempt))
            // 문제: 같은 트랜잭션 내에서 재시도
            // JPA 1차 캐시로 인해 오래된 엔티티 반환 가능
        }
    }
}
```

**해결 방법:**

재시도 로직을 Saga Orchestrator로 이동하여 각 재시도마다 새 트랜잭션 시작:
```kotlin
// MessageSagaOrchestrator.kt
fun execute(message: ChatMessage, maxRetries: Int = 3): MessageSagaContext {
    var attempt = 0

    while (attempt < maxRetries) {
        val context = MessageSagaContext(message = message)

        try {
            val success = orchestrator.execute(context)

            if (success) {
                return context
            } else if (context.error is OptimisticLockException && attempt < maxRetries - 1) {
                attempt++
                Thread.sleep(calculateBackoff(attempt))
                continue  // 새 트랜잭션으로 재시도
            } else {
                return context
            }
        } catch (e: Exception) {
            context.markFailed(e)
            return context
        }
    }
}
```

### P1-3: 보상 트랜잭션 실패 시 MongoDB에 고아 문서 남음 ⚠️

**영향받는 파일:**
- `SaveMessageToMongoStep.kt:41-58`

**문제:**
```kotlin
override fun compensate(context: MessageSagaContext): Boolean {
    return try {
        val messageId = context.savedMessage?.id
        if (messageId != null) {
            messageCommandPort.delete(messageId)  // MongoDB 삭제 실패 가능
            true
        } else {
            true  // messageId가 null이면 그냥 성공으로 처리
        }
    } catch (e: Exception) {
        logger.error(e) { "Compensation failed" }
        false  // Saga 상태 FAILED
        // MongoDB에는 메시지 남아있음! (고아 문서)
    }
}
```

**해결 방법:**

일일 스케줄러로 고아 문서 정리:
```kotlin
@Service
class OrphanedMessageCleanupService(
    private val messageQueryPort: MessageQueryPort,
    private val messageCommandPort: MessageCommandPort,
    private val chatRoomQueryPort: ChatRoomQueryPort
) {
    @Scheduled(cron = "0 0 2 * * *")  // 매일 새벽 2시
    @SchedulerLock(name = "cleanupOrphanedMessages", lockAtMostFor = "30m")
    fun cleanupOrphanedMessages() {
        val cutoffDate = Instant.now().minus(7, ChronoUnit.DAYS)
        val oldMessages = messageQueryPort.findMessagesCreatedBefore(cutoffDate)

        val orphanedCount = oldMessages.count { message ->
            val chatRoomExists = chatRoomQueryPort.findById(message.roomId) != null
            if (!chatRoomExists) {
                logger.warn { "Deleting orphaned message: ${message.id?.value}" }
                messageCommandPort.delete(message.id!!)
                true
            } else {
                false
            }
        }

        logger.info { "Cleaned up $orphanedCount orphaned messages" }
    }
}
```

## 운영 개선 사항 (P2)

### P2-1: Saga 실패에 대한 Slack 알림 없음

**영향받는 파일:**
- `OutboxEventProcessor.kt:303`

TODO 주석으로 표시되어 있음: "// TODO: 알림 전송 (Slack, 이메일 등)"

**구현:**
```kotlin
if (failedEvents.isNotEmpty()) {
    slackNotificationPort.notifySagaFailures(
        count = failedEvents.size,
        details = failedEvents.take(10).map {
            "sagaId=${it.sagaId}, type=${it.eventType}, error=${it.lastError}"
        }.joinToString("\n")
    )
}
```

### P2-2: 데이터 정합성 검증 엔드포인트 없음

**구현 필요:**
```kotlin
@RestController
@RequestMapping("/admin/reconciliation")
class DataReconciliationController {

    @PostMapping("/verify-message-refs")
    fun verifyMessageReferences(): ReconciliationReport {
        // MongoDB 메시지가 존재하는 채팅방을 참조하는지 확인
        // ChatRoom.lastMessageId가 실제 존재하는 메시지를 가리키는지 확인
        // 불일치 항목 반환
    }
}
```

## 실패 시나리오 분석

### 시나리오 1: MongoDB 성공, PostgreSQL 실패

```
Step 1: MongoDB.save(message) ✅
Step 2: PostgreSQL.update(chatRoom) ❌
Step 3: 실행 안됨
    ↓
Compensation triggered
    ↓
MongoDB.delete(message) ✅
```

**결과:** 일관성 유지 ✅

**잠재적 문제:** MongoDB 삭제 실패 시 고아 문서 → P1-3로 해결

### 시나리오 2: Debezium 다운

```
Outbox event created ✅
Debezium: ❌
    ↓
5-10초 후
    ↓
OutboxEventProcessor 처리 ✅
```

**결과:** 일관성 유지 ✅ (지연 발생)

### 시나리오 3: Edit 중 이벤트 발행 실패

```
MongoDB.update(message) ✅
EventPublisher.publishEvent(...) ❌
```

**결과:** 메시지는 수정되었지만 알림 손실 ❌ → P0-1로 해결 필요

### 시나리오 4: 동시 채팅방 업데이트

```
Thread 1: Message A → Update ChatRoom
Thread 2: Message B → Update ChatRoom (same room)
    ↓
OptimisticLockException on Thread 2
    ↓
3번 재시도 (지수 백오프)
```

**결과:** 대부분 성공 ✅

**잠재적 문제:** 고동시성 환경에서 JPA 캐시 이슈 → P1-2로 해결

## 우선순위별 작업 계획

### P0 - 긴급 (이번 주)
1. **Edit/Delete/Reaction/Pin에 Outbox 추가** (6시간)
   - PostgreSQL 감사 로그 테이블 생성
   - 각 서비스에 Outbox 쓰기 추가
   - 통합 테스트 작성

2. **알림 생성 재시도 로직 추가** (3시간)
   - Spring Retry 의존성 추가
   - @Retryable 적용
   - DLQ 저장소 구현

### P1 - 높음 (이번 스프린트)
3. **Idempotency Check 추가** (1시간)
   - `PublishEventToOutboxStep` 수정

4. **Optimistic Lock 재시도 개선** (3시간)
   - `MessageSagaOrchestrator`로 로직 이동

5. **고아 메시지 정리 작업** (2시간)
   - 스케줄러 구현

### P2 - 중간 (다음 스프린트)
6. **Saga 실패 Slack 알림** (1시간)
7. **데이터 정합성 검증 API** (4시간)
8. **Chaos Engineering 테스트** (8시간)
9. **Prometheus 메트릭** (3시간)
10. **Grafana 알림 설정** (2시간)

## 모니터링 지표

### 추가해야 할 Prometheus 메트릭

```kotlin
// Outbox 처리 시간
outbox.processing.time (Timer)

// Outbox 실패 횟수
outbox.failures (Counter)

// Saga 보상 횟수
saga.compensation (Counter)

// 고아 메시지 개수
messages.orphaned (Gauge)

// DLQ 항목 개수
dlq.entries (Gauge)
```

### Grafana 알림

1. **Outbox 미처리 이벤트 > 100** (5분 동안)
2. **DLQ 항목 > 10** (1시간 동안)
3. **Saga 보상 비율 > 5%** (15분 동안)
4. **CDC 지연 > 30초**
5. **고아 메시지 > 50**

## 테스트 전략

### 실패 주입 테스트

```kotlin
@SpringBootTest
@Testcontainers
class DataConsistencyIntegrationTest {

    @Test
    fun `MongoDB 성공 PostgreSQL 실패시 메시지 삭제됨`()

    @Test
    fun `Outbox 생성 실패시 Saga 보상 실행됨`()

    @Test
    fun `CDC 다운시 폴링 메커니즘으로 처리됨`()

    @Test
    fun `중복 메시지시 중복 Outbox 이벤트 생성 안됨`()

    @Test
    fun `알림 생성 실패시 3번 재시도 후 DLQ 이동`()
}
```

### Chaos Engineering

- MongoDB 지연 주입 (500ms-2s)
- PostgreSQL 연결 풀 고갈
- Kafka 브로커 장애
- 네트워크 파티션

## 작업 예상 시간

| 우선순위 | 작업 | 예상 시간 | 담당 인원 |
|---------|------|---------|---------|
| P0 | Edit/Delete/Reaction/Pin Outbox | 6시간 | 2명 |
| P0 | 알림 재시도 로직 | 3시간 | 1명 |
| P1 | Idempotency Check | 1시간 | 1명 |
| P1 | Optimistic Lock 개선 | 3시간 | 1명 |
| P1 | 고아 메시지 정리 | 2시간 | 1명 |
| P2 | 기타 개선사항 | 18시간 | 1명 |
| **총계** | | **33시간 (약 4주)** | |

## 결론

현재 시스템은 **메시지 생성 플로우**에 대해 견고한 정합성 보장 메커니즘을 갖추고 있습니다. 하지만 **Edit/Delete/Reaction/Pin/Notification 작업**은 Outbox 패턴을 우회하여 **알림 손실 및 데이터 불일치 위험**이 있습니다.

**P0 이슈 해결**에 1주일, **P1 이슈 해결**에 1주일, **P2 개선사항**에 2주일이 소요될 것으로 예상되며, 이를 통해 **프로덕션급 정합성 보장**을 달성할 수 있습니다.

---

*최종 업데이트: 2025-10-27*
*분석자: Claude Code Agent*
