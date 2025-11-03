# 메시지 수정/삭제 비즈니스 플로우

> MongoDB 원자적 작업 + @Async 이벤트 리스너 패턴

**작성일:** 2025-11-02
**분석 범위:** 메시지 수정/삭제 API부터 WebSocket 알림까지

---

## 📋 전체 플로우 개요

```
User → REST API → EditMessageService/DeleteMessageService
→ MongoDB 영속화 (atomic)
→ 이벤트 발행
→ MessageEventWebSocketListener (@Async)
→ WebSocket 브로드캐스트
```

---

## 🔍 상세 플로우 분석

### 1. 메시지 수정 플로우 (EditMessageService)

**파일:** `EditMessageService.kt`

**트랜잭션:** ❌ 없음 (MongoDB 단일 Document는 원자적)

```kotlin
@UseCase
class EditMessageService {
    override fun editMessage(command: EditMessageCommand): ChatMessage {
        // 1. 메시지 조회
        val existingMessage = messageQueryPort.findById(command.messageId)

        // 2. 도메인 로직으로 메시지 수정
        val updatedMessage = messageEditDomainService.editMessage(existingMessage, command.newContent)

        // 3. MongoDB에 영속화 (atomic 작업)
        val savedMessage = messageCommandPort.save(updatedMessage)

        // 4. 이벤트 발행
        publishMessageEditedEvent(savedMessage, command.userId, oldContent, newContent)

        return savedMessage
    }
}
```

**처리 단계:**
1. ✅ 메시지 조회 (MongoDB)
2. ✅ 도메인 검증:
   - 24시간 이내 메시지만 수정 가능
   - 삭제된 메시지 수정 불가
   - TEXT 타입만 수정 가능
   - 빈 내용으로 수정 불가
3. ✅ MongoDB 저장 (단일 Document → atomic)
4. ✅ MessageEditedEvent 발행

**데이터베이스:** MongoDB `messages` 컬렉션

**트랜잭션 특성:**
- MongoDB 단일 Document 작업은 원자적
- `@Transactional` 불필요
- save() 성공 or 실패 (중간 상태 없음)

---

### 2. 메시지 삭제 플로우 (DeleteMessageService)

**파일:** `DeleteMessageService.kt`

**트랜잭션:** ❌ 없음 (MongoDB 단일 Document는 원자적)

```kotlin
@UseCase
class DeleteMessageService {
    override fun deleteMessage(command: DeleteMessageCommand): ChatMessage {
        // 1. 메시지 조회
        val existingMessage = messageQueryPort.findById(command.messageId)

        // 2. 소프트 삭제
        existingMessage.markAsDeleted()

        // 3. MongoDB에 영속화 (atomic 작업)
        val savedMessage = messageCommandPort.save(existingMessage)

        // 4. 이벤트 발행
        publishMessageDeletedEvent(savedMessage, command.userId)

        return savedMessage
    }
}
```

**처리 단계:**
1. ✅ 메시지 조회 (MongoDB)
2. ✅ 소프트 삭제: `isDeleted = true`
3. ✅ MongoDB 저장 (단일 Document → atomic)
4. ✅ MessageDeletedEvent 발행

**소프트 삭제:**
- 메시지를 물리적으로 삭제하지 않음
- `isDeleted = true` 플래그만 설정
- 이유:
  - 삭제 이력 추적
  - 감사(Audit) 로그
  - 복구 가능성

---

### 3. WebSocket 브로드캐스트 (MessageEventWebSocketListener)

**파일:** `MessageEventWebSocketListener.kt`

**트랜잭션:** ❌ 없음 (비동기 처리)

**패턴:** @Async + @EventListener

```kotlin
@Component
class MessageEventWebSocketListener {
    @Async
    @EventListener
    fun handleMessageEdited(event: MessageEditedEvent) {
        try {
            // 1. 채팅방 모든 참여자에게 브로드캐스트
            webSocketMessageBroker.sendMessage(
                "/topic/message/edit/${event.roomId.value}",
                event.message
            )

            // 2. 요청자에게 성공 응답
            webSocketMessageBroker.sendMessage(
                "/queue/message/edit/response/${event.userId.value}",
                WebSocketResponseBuilder.success(event.message, "메시지가 수정되었습니다.")
            )
        } catch (e: Exception) {
            // WebSocket 실패는 로깅만 (메시지는 이미 저장됨)
            logger.error(e) { "WebSocket 전송 실패 (메시지는 이미 MongoDB에 저장됨)" }
        }
    }

    @Async
    @EventListener
    fun handleMessageDeleted(event: MessageDeletedEvent) {
        // 삭제 이벤트도 동일한 패턴
    }
}
```

**처리 단계:**
1. ✅ 비동기 실행 (@Async)
2. ✅ 채팅방 브로드캐스트 (`/topic/message/edit/{roomId}`)
3. ✅ 요청자 응답 (`/queue/message/edit/response/{userId}`)
4. ✅ 실패 시 로깅만 (예외 전파 안 함)

**비동기 처리 장점:**
- MongoDB 저장 완료 후 별도 스레드에서 실행
- API 응답 속도에 영향 없음
- WebSocket 전송 지연이 사용자 경험에 영향 없음

---

## 📊 타임라인 다이어그램

### 메시지 수정 플로우:

```
Time →

t0: REST API: PUT /messages/{id}
    └─> EditMessageService.editMessage()

t1: MongoDB 조회
    └─> messageQueryPort.findById()
    └─> existingMessage (version 확인 등)

t2: 도메인 검증
    └─> messageEditDomainService.editMessage()
    ├─> 24시간 체크
    ├─> 삭제 여부 체크
    ├─> TEXT 타입 체크
    └─> 내용 검증

t3: MongoDB 저장 (atomic)
    └─> messageCommandPort.save(updatedMessage)
    └─> 성공 ✅

t4: MessageEditedEvent 발행
    └─> eventPublisher.publishEvent(event)
    └─> API 응답 반환 (200 OK)

t5: @Async MessageEventWebSocketListener 실행 (별도 스레드)
    ├─> WebSocket 브로드캐스트 (/topic/message/edit/{roomId})
    └─> 요청자 응답 (/queue/message/edit/response/{userId})
```

**핵심 포인트:**
- **t3에서 MongoDB 저장 완료 → 메시지 안전하게 저장됨**
- **t4에서 API 응답 → 사용자는 빠른 응답 받음**
- **t5는 별도 스레드 → 실패해도 메시지 유실 없음**

---

### 메시지 삭제 플로우:

```
Time →

t0: REST API: DELETE /messages/{id}
    └─> DeleteMessageService.deleteMessage()

t1: MongoDB 조회
    └─> messageQueryPort.findById()

t2: 소프트 삭제
    └─> existingMessage.markAsDeleted()
    └─> isDeleted = true

t3: MongoDB 저장 (atomic)
    └─> messageCommandPort.save(existingMessage)
    └─> 성공 ✅

t4: MessageDeletedEvent 발행
    └─> eventPublisher.publishEvent(event)
    └─> API 응답 반환 (200 OK)

t5: @Async MessageEventWebSocketListener 실행 (별도 스레드)
    ├─> WebSocket 브로드캐스트 (/topic/message/delete/{roomId})
    └─> 요청자 응답 (/queue/message/delete/response/{userId})
```

---

## ✅ 패턴 분석: Slack/Discord 표준

### 이 패턴의 장점:

1. **메시지 유실 방지**
   - WebSocket 실패해도 메시지는 이미 MongoDB에 저장됨
   - 클라이언트 재연결 시 동기화 가능

2. **작업 독립성**
   - 외부 시스템(WebSocket) 실패가 저장 작업에 영향 없음
   - MongoDB 저장과 WebSocket 전송이 독립적

3. **빠른 응답 속도**
   - MongoDB 저장만 완료되면 API 응답
   - WebSocket은 비동기로 처리

4. **복구 가능성**
   - 메시지가 MongoDB에 영속화됨
   - 클라이언트가 `/messages` API로 재조회 가능

### Slack/Discord 동작 방식:

```
1. 사용자가 메시지 수정 클릭
   ↓
2. API 서버가 DB에 먼저 저장
   ↓
3. API 응답 반환 (200 OK)
   ↓
4. 비동기로 WebSocket 브로드캐스트
   ↓
5. WebSocket 실패 시:
   - 사용자는 재연결
   - 클라이언트가 최신 메시지 재조회
   - 수정된 메시지 표시
```

---

## 🔄 MongoDB vs PostgreSQL 트랜잭션 비교

### MongoDB (메시지 수정/삭제):

```kotlin
// ❌ @Transactional 없음
@UseCase
class EditMessageService {
    override fun editMessage(...): ChatMessage {
        val savedMessage = messageCommandPort.save(updatedMessage)  // atomic
        publishMessageEditedEvent(savedMessage)
        return savedMessage
    }
}
```

**특징:**
- 단일 Document 작업은 원자적
- `@Transactional` 불필요
- save() 성공 or 실패 (중간 상태 없음)

---

### PostgreSQL (메시지 전송 - Saga):

```kotlin
// ✅ @Transactional 있음 (Step 2)
@Component
class UpdateChatRoomMetadataStep {
    @Transactional  // PostgreSQL 트랜잭션 시작
    override fun execute(context: MessageSagaContext): Boolean {
        val chatRoom = chatRoomQueryPort.findById(roomId)  // JPA 조회
        chatRoomCommandPort.save(updatedRoom)  // JPA 저장
        return true
    }
}

@Component
class PublishEventToOutboxStep {
    @Transactional(propagation = Propagation.MANDATORY)  // 기존 트랜잭션 참여
    override fun execute(context: MessageSagaContext): Boolean {
        outboxEventRepository.save(outboxEvent)  // JPA 저장
        return true
    }
}
```

**특징:**
- 여러 테이블 업데이트 필요
- `@Transactional` 필수
- Step 2, 3가 하나의 트랜잭션으로 묶임

---

## 🎯 트랜잭션 전략 요약

### MongoDB 작업 (메시지 수정/삭제):

| 항목 | 설정 |
|------|------|
| @Transactional | ❌ 불필요 |
| 원자성 보장 | ✅ 단일 Document (atomic) |
| 격리 수준 | N/A (Document 레벨) |
| 롤백 메커니즘 | N/A (save 성공/실패만) |

### PostgreSQL 작업 (채팅방 메타데이터):

| 항목 | 설정 |
|------|------|
| @Transactional | ✅ 필수 |
| 원자성 보장 | ✅ 트랜잭션으로 보장 |
| 격리 수준 | READ_COMMITTED (기본) |
| 롤백 메커니즘 | ✅ 예외 시 자동 롤백 |

---

## ⚠️ 주의사항

### 1. MongoDB 트랜잭션을 사용하지 않는 이유

**잘못된 패턴:**
```kotlin
@Transactional  // ← MongoDB에는 불필요!
@UseCase
class EditMessageService {
    override fun editMessage(...): ChatMessage {
        messageCommandPort.save(updatedMessage)
        return savedMessage
    }
}
```

**이유:**
- MongoDB 단일 Document 작업은 이미 원자적
- Spring `@Transactional`은 JPA/JDBC용
- MongoDB에 `@Transactional`을 적용하면:
  - 불필요한 오버헤드
  - 성능 저하
  - 복잡도 증가

### 2. 이벤트 리스너 실행 시점

**@EventListener vs @TransactionalEventListener:**

```kotlin
// ✅ 현재 구현 (MongoDB)
@Async
@EventListener  // ← 이벤트 발행 즉시 실행
fun handleMessageEdited(event: MessageEditedEvent) {
    webSocketMessageBroker.sendMessage(...)
}

// ❌ MongoDB에는 부적합
@Async
@TransactionalEventListener  // ← 트랜잭션 커밋 후 실행 (PostgreSQL용)
fun handleMessageEdited(event: MessageEditedEvent) {
    // MongoDB에는 트랜잭션이 없으므로 실행되지 않을 수 있음!
}
```

**차이점:**
- `@EventListener`: 이벤트 발행 즉시 실행
- `@TransactionalEventListener`: 트랜잭션 커밋 후 실행

**MongoDB에서는 `@EventListener` 사용!**

---

## 🟢 정상 동작 확인

### ✅ 트랜잭션 없이 작동하는 이유:

1. **MongoDB 원자성:**
   - 단일 Document 작업은 원자적
   - save() 성공 = 메시지 완전히 저장됨
   - save() 실패 = 아무것도 저장 안 됨

2. **비동기 이벤트 리스너:**
   - @Async로 별도 스레드 실행
   - MongoDB 저장 완료 후 실행
   - WebSocket 실패해도 메시지 안전

3. **멱등성 보장:**
   - 같은 messageId로 재수정 시 덮어쓰기
   - 중복 처리 문제 없음

---

## 📝 결론

### 정상 동작 부분:
✅ MongoDB 단일 Document 원자성 활용
✅ @Transactional 없이 정확히 동작
✅ @Async + @EventListener 패턴으로 비동기 WebSocket
✅ Slack/Discord 표준 패턴 준수
✅ 메시지 유실 방지

### 개선 불필요:
- 현재 구현이 최적
- MongoDB 특성을 정확히 이해하고 구현
- 불필요한 트랜잭션 없음

---

**작성자:** Claude Code
**검토 날짜:** 2025-11-02
**상태:** ✅ 문제 없음
