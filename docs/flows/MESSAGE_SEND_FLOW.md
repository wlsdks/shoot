# 메시지 전송 비즈니스 플로우

> PostgreSQL + MongoDB 분산 트랜잭션을 Saga 패턴으로 처리

**작성일:** 2025-11-02
**분석 범위:** 메시지 전송부터 최종 저장 및 이벤트 발행까지

---

## 📋 전체 플로우 개요

```
User → REST API → SendMessageService
→ Redis/Kafka 발행
→ HandleMessageEventService
→ MessageSagaOrchestrator
→ [Step1: MongoDB 저장]
→ [Step2: PostgreSQL ChatRoom 업데이트]
→ [Step3: Outbox 이벤트 저장]
→ WebSocket 알림
```

---

## 🔍 상세 플로우 분석

### 1단계: 메시지 전송 요청 (SendMessageService)

**파일:** `SendMessageService.kt`

**트랜잭션:** ❌ 없음 (순수 도메인 로직 + 발행만)

```kotlin
@UseCase
class SendMessageService {
    override fun sendMessage(command: SendMessageCommand) {
        runCatching {
            // 1. 도메인 객체 생성 및 비즈니스 로직 처리
            val domainMessage = createAndProcessDomainMessage(messageRequest)
        }.onSuccess { domainMessage ->
            // 2. Redis/Kafka로 메시지 발행
            messagePublisherPort.publish(messageRequest, domainMessage)
        }
    }
}
```

**처리 내용:**
1. ✅ 도메인 객체 생성 (ChatMessage)
2. ✅ URL 추출 및 미리보기 캐시 조회
3. ✅ 메시지 요청 DTO에 도메인 정보 반영 (tempId, previewUrl 등)
4. ✅ Redis Stream/Kafka로 발행

**데이터베이스 접근:** ❌ 없음 (Redis 캐시 조회만)

**특징:**
- 트랜잭션 없음 → 빠른 응답
- 실패 시 Redis Stream에 에러 상태 발행

---

### 2단계: 메시지 이벤트 처리 (HandleMessageEventService)

**파일:** `HandleMessageEventService.kt`

**트랜잭션:** ❌ 없음 (Saga Orchestrator가 관리)

```kotlin
@UseCase
class HandleMessageEventService {
    override fun handle(event: MessageEvent): Boolean {
        // Saga 실행: MongoDB 저장 → PostgreSQL 업데이트 → Outbox 저장
        val sagaContext = messageSagaOrchestrator.execute(message)

        when (sagaContext.state) {
            SagaState.COMPLETED -> notifyPersistenceSuccess()
            SagaState.COMPENSATED, SagaState.FAILED -> notifyPersistenceFailure()
        }
    }
}
```

**처리 내용:**
1. ✅ MessageSagaOrchestrator 실행
2. ✅ URL 미리보기 처리 (백그라운드)
3. ✅ 성공/실패 알림 전송 (WebSocket)

---

### 3단계: Saga 오케스트레이터 (MessageSagaOrchestrator)

**파일:** `MessageSagaOrchestrator.kt`

**트랜잭션:** ❌ 없음 (각 Step이 관리)

**OptimisticLock 재시도:**
```kotlin
fun execute(message: ChatMessage): MessageSagaContext {
    var attempt = 0

    while (attempt < MAX_RETRIES) {
        val context = MessageSagaContext(message = message)
        val success = executeInternal(context)

        if (success) {
            context.markCompleted()
            return context
        } else if (context.error is OptimisticLockException && attempt < MAX_RETRIES - 1) {
            // OptimisticLockException 재시도
            attempt++
            Thread.sleep(calculateBackoff(attempt)) // 0ms → 10ms → 100ms
            continue
        }
    }
}
```

**재시도 전략:**
- 최대 3번 재시도
- 지수 백오프: 0ms → 10ms → 100ms
- OptimisticLockException만 재시도

---

## 🔄 Saga Step 분석

### Step 1: SaveMessageToMongoStep

**파일:** `SaveMessageToMongoStep.kt`

**트랜잭션:** ❌ 없음 (MongoDB 단일 Document는 원자적)

```kotlin
@Component
class SaveMessageToMongoStep {
    override fun execute(context: MessageSagaContext): Boolean {
        // 발신자 자동 읽음 처리
        if (context.message.readBy[context.message.senderId] != true) {
            context.message.markAsRead(context.message.senderId)
        }

        val savedMessage = saveMessagePort.save(context.message)
        context.savedMessage = savedMessage
        return true
    }

    override fun compensate(context: MessageSagaContext): Boolean {
        // 보상: MongoDB에서 메시지 물리 삭제
        messageCommandPort.delete(messageId)
        return true
    }
}
```

**처리 내용:**
- ✅ MongoDB에 메시지 저장
- ✅ 발신자 자동 읽음 처리
- ✅ 보상 트랜잭션: 물리 삭제

**데이터베이스:** MongoDB `messages` 컬렉션

**트랜잭션 특성:**
- MongoDB 단일 Document 저장은 원자적
- 트랜잭션 없이도 ACID 보장

---

### Step 2: UpdateChatRoomMetadataStep

**파일:** `UpdateChatRoomMetadataStep.kt`

**트랜잭션:** ✅ `@Transactional` (PostgreSQL 트랜잭션 시작!)

```kotlin
@Component
class UpdateChatRoomMetadataStep {
    @Transactional  // ← PostgreSQL 트랜잭션 시작!
    override fun execute(context: MessageSagaContext): Boolean {
        try {
            // 1. ChatRoom 조회 (JPA)
            val chatRoom = chatRoomQueryPort.findById(savedMessage.roomId)

            // 2. 메타데이터 업데이트
            val updatedRoom = chatRoomMetadataDomainService
                .updateChatRoomWithNewMessage(chatRoom, savedMessage)
            val savedRoom = chatRoomCommandPort.save(updatedRoom)

            // 3. 마지막 읽은 메시지 ID 업데이트
            chatRoomCommandPort.updateLastReadMessageId(
                savedMessage.roomId,
                savedMessage.senderId,
                messageId
            )

            return true
        } catch (e: OptimisticLockException) {
            // Orchestrator 레벨에서 재시도
            context.markFailed(e)
            return false
        }
    }

    override fun compensate(context: MessageSagaContext): Boolean {
        // 보상: 원래 상태로 복원
        chatRoomCommandPort.save(originalRoom)
        return true
    }
}
```

**처리 내용:**
- ✅ ChatRoom 조회 (JPA)
- ✅ lastMessage, lastMessageAt, unreadCount 업데이트
- ✅ 발신자의 lastReadMessageId 업데이트
- ✅ @Version으로 OptimisticLock 적용

**데이터베이스:** PostgreSQL `chat_rooms`, `chat_room_users` 테이블

**트랜잭션 특성:**
- **이 Step에서 PostgreSQL 트랜잭션 시작!**
- Step 3도 이 트랜잭션에 참여 (MANDATORY)
- OptimisticLockException 발생 시 Orchestrator가 재시도

---

### Step 3: PublishEventToOutboxStep

**파일:** `PublishEventToOutboxStep.kt`

**트랜잭션:** ✅ `@Transactional(propagation = Propagation.MANDATORY)`

```kotlin
@Component
class PublishEventToOutboxStep {
    @Transactional(propagation = Propagation.MANDATORY)  // ← Step 2의 트랜잭션 참여!
    override fun execute(context: MessageSagaContext): Boolean {
        // 1. MessageSentEvent 발행
        val messageSentEvent = MessageSentEvent.create(savedMessage)
        saveToOutbox(context.sagaId, messageSentEvent)

        // 2. 멘션이 있으면 MentionEvent 발행
        if (savedMessage.mentions.isNotEmpty()) {
            val mentionEvent = createMentionEvent(savedMessage)
            saveToOutbox(context.sagaId, mentionEvent)
        }

        return true
    }

    private fun saveToOutbox(sagaId: String, event: Any) {
        val idempotencyKey = "$sagaId-${event::class.java.simpleName}"

        // Idempotency check
        if (outboxEventRepository.existsByIdempotencyKey(idempotencyKey)) {
            return
        }

        outboxEventRepository.save(outboxEvent)
    }

    override fun compensate(context: MessageSagaContext): Boolean {
        // 보상: Saga ID로 조회해서 Outbox 이벤트 삭제
        val events = outboxEventRepository.findBySagaIdOrderByCreatedAtAsc(context.sagaId)
        outboxEventRepository.deleteAll(events)
        return true
    }
}
```

**처리 내용:**
- ✅ Outbox에 MessageSentEvent 저장
- ✅ 멘션이 있으면 MentionEvent 저장
- ✅ Idempotency Key로 중복 방지

**데이터베이스:** PostgreSQL `outbox_events` 테이블

**트랜잭션 특성:**
- **Step 2의 트랜잭션에 필수 참여 (MANDATORY)**
- Step 2와 함께 커밋/롤백
- **Step 2, 3가 하나의 PostgreSQL 트랜잭션으로 묶임!**

---

## ⚠️ 발견된 문제점

### 🔴 문제 1: UpdateChatRoomMetadataStep의 보상 트랜잭션

**문제 코드:**
```kotlin
override fun compensate(context: MessageSagaContext): Boolean {
    val originalRoom = context.chatRoom
    if (originalRoom != null) {
        // 원래 상태로 복원
        chatRoomCommandPort.save(originalRoom)  // ← 문제!
    }
}
```

**문제:**
1. Step 2에서 `@Transactional`이 이미 커밋된 후 보상 실행
2. `originalRoom`은 이전 버전 (version=N)
3. 하지만 DB에는 이미 version=N+1로 업데이트됨
4. save() 시 OptimisticLockException 발생 가능!

**영향:**
- 보상 트랜잭션 실패 → Saga 상태가 COMPENSATED가 아닌 FAILED
- 데이터 불일치 발생 (MongoDB에만 메시지 존재, ChatRoom 업데이트 안 됨)
- **수동 개입 필요**

**해결 방법:**
1. **Option 1:** 보상 시 DB에서 다시 조회 후 역산하여 복원
2. **Option 2:** 보상 트랜잭션을 별도 메서드로 분리하고 `@Transactional` 추가
3. **Option 3:** 보상 시 OptimisticLockException을 무시하고 강제로 버전 증가 없이 복원

**권장:** Option 2 + 재시도 로직

---

### 🟡 문제 2: SaveMessageToMongoStep의 readBy 수정

**현재 코드:**
```kotlin
// 발신자 읽음 처리
if (context.message.readBy[context.message.senderId] != true) {
    context.message.markAsRead(context.message.senderId)
}
```

**문제:**
- `context.message`를 직접 수정함
- 이후 보상 트랜잭션 시 원본 메시지가 이미 변경된 상태
- 멱등성 문제 (같은 메시지로 재시도 시 이미 readBy 수정됨)

**해결 방법:**
```kotlin
// 복사본을 만들어서 수정
val messageToSave = if (context.message.readBy[context.message.senderId] != true) {
    context.message.copy().also { it.markAsRead(context.message.senderId) }
} else {
    context.message
}

val savedMessage = saveMessagePort.save(messageToSave)
```

---

### 🟢 잘된 점

1. **✅ Saga 패턴 적용:**
   - MongoDB (Step 1) + PostgreSQL (Step 2, 3) 분산 트랜잭션 처리
   - 실패 시 보상 트랜잭션 자동 실행

2. **✅ OptimisticLock 재시도:**
   - Orchestrator 레벨에서 재시도
   - 각 재시도마다 새 트랜잭션 → JPA 1차 캐시 문제 해결

3. **✅ Idempotency 보장:**
   - Outbox 이벤트에 idempotencyKey 사용
   - 중복 이벤트 방지

4. **✅ 트랜잭션 경계 명확:**
   - Step 1: 트랜잭션 없음 (MongoDB 원자성)
   - Step 2: `@Transactional` 시작
   - Step 3: `@Transactional(MANDATORY)` 참여

---

## 📊 트랜잭션 흐름 다이어그램

```
Time →

t0: SendMessageService.sendMessage()
    └─> Redis/Kafka 발행 (트랜잭션 없음)

t1: HandleMessageEventService.handle()
    └─> MessageSagaOrchestrator.execute()

t2: Step 1: SaveMessageToMongoStep
    └─> MongoDB save() (트랜잭션 없음, 원자적)
    └─> context.savedMessage = savedMessage

t3: Step 2: UpdateChatRoomMetadataStep
    ┌─────────────────────────────────────┐
    │ @Transactional 시작                  │
    │ - ChatRoom 조회 (JPA)                │
    │ - 메타데이터 업데이트                 │
    │ - lastReadMessageId 업데이트         │
    └─────────────────────────────────────┘
                    │
t4: Step 3: PublishEventToOutboxStep      │
    ┌─────────────────────────────────────┤
    │ @Transactional(MANDATORY) 참여      │
    │ - Outbox 이벤트 저장                 │
    │ - MentionEvent 저장 (조건부)         │
    └─────────────────────────────────────┘
                    │
t5: PostgreSQL 트랜잭션 커밋 ✅
    - Step 2, 3의 모든 변경사항 커밋

t6: Saga COMPLETED
    └─> WebSocket 알림 전송 (성공)
```

**실패 시:**
```
t3: Step 2 실패 (OptimisticLockException)
    └─> PostgreSQL 트랜잭션 롤백
    └─> Step 1 보상 실행 (MongoDB 메시지 삭제)
    └─> Saga COMPENSATED
```

---

## 🎯 개선 권장사항

### 1. UpdateChatRoomMetadataStep 보상 트랜잭션 개선

**Before:**
```kotlin
override fun compensate(context: MessageSagaContext): Boolean {
    chatRoomCommandPort.save(originalRoom)  // OptimisticLockException 가능
    return true
}
```

**After:**
```kotlin
@Transactional
override fun compensate(context: MessageSagaContext): Boolean {
    return try {
        // DB에서 현재 상태 조회
        val currentRoom = chatRoomQueryPort.findById(context.savedMessage.roomId)
            ?: return true

        // 역산하여 원래 상태로 복원
        val restoredRoom = currentRoom.rollbackLastMessage()
        chatRoomCommandPort.save(restoredRoom)
        true
    } catch (e: OptimisticLockException) {
        // 재시도 로직 필요
        logger.error(e) { "Compensation failed - retry needed" }
        false
    }
}
```

### 2. SaveMessageToMongoStep readBy 수정 방식 개선

**Before:**
```kotlin
context.message.markAsRead(context.message.senderId)  // 원본 수정
```

**After:**
```kotlin
val messageToSave = context.message.copy()
messageToSave.markAsRead(context.message.senderId)  // 복사본 수정
```

### 3. 보상 트랜잭션 재시도 로직 추가

```kotlin
@Transactional
override fun compensate(context: MessageSagaContext): Boolean {
    var attempt = 0
    val maxRetries = 3

    while (attempt < maxRetries) {
        try {
            // 보상 로직
            return true
        } catch (e: OptimisticLockException) {
            attempt++
            if (attempt >= maxRetries) {
                logger.error(e) { "Compensation failed after $maxRetries attempts" }
                return false
            }
            Thread.sleep(100L * attempt)
        }
    }
    return false
}
```

---

## 📝 결론

### 정상 동작 부분:
✅ Saga 패턴으로 분산 트랜잭션 처리
✅ MongoDB와 PostgreSQL의 트랜잭션 경계 명확
✅ OptimisticLock 재시도 (Orchestrator 레벨)
✅ Idempotency 보장 (Outbox)

### 개선 필요 부분:
⚠️ UpdateChatRoomMetadataStep 보상 트랜잭션 (OptimisticLockException 처리)
⚠️ SaveMessageToMongoStep readBy 수정 방식 (멱등성)

---

**작성자:** Claude Code
**검토 날짜:** 2025-11-02
