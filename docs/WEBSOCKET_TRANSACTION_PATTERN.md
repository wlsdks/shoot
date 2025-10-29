# WebSocket 트랜잭션 패턴

> Slack, Discord 등 업계 표준을 따르는 메시지 영속화 및 WebSocket 브로드캐스트 패턴

## 📋 목차

- [개요](#개요)
- [문제점: 기존 패턴](#문제점-기존-패턴)
- [해결책: 업계 표준 패턴](#해결책-업계-표준-패턴)
- [구현 방법](#구현-방법)
- [장점](#장점)
- [업계 사례](#업계-사례)
- [코드 예제](#코드-예제)

---

## 개요

실시간 채팅 애플리케이션에서 **메시지 영속화**와 **WebSocket 브로드캐스트**의 순서는 매우 중요합니다. 이 문서는 Slack, Discord, WhatsApp 등 프로덕션 시스템에서 검증된 패턴을 설명합니다.

## 문제점: 기존 패턴

### ❌ 잘못된 패턴: 트랜잭션 내에서 WebSocket 전송

```kotlin
@Transactional  // 트랜잭션 시작
fun editMessage(command: EditMessageCommand): ChatMessage {
    val message = messageRepository.save(updatedMessage)  // 1. DB 저장

    webSocketBroker.sendMessage("/topic/chat", message)   // 2. WebSocket 전송
    // ⚠️ WebSocket 실패 시 예외 발생 → 트랜잭션 롤백 → 메시지 유실!

    return message
}  // 트랜잭션 커밋
```

### 🚨 문제점

| 문제 | 설명 | 영향 |
|------|------|------|
| **메시지 유실 위험** | WebSocket 전송 실패 시 트랜잭션 롤백 | 사용자가 보낸 메시지가 DB에서 사라짐 |
| **트랜잭션 결합** | 외부 시스템(WebSocket)과 트랜잭션 결합 | 시스템 안정성 저하 |
| **복구 불가능** | 메시지가 DB에 없으면 재전송 불가 | 데이터 손실 |

### 실제 시나리오

```
사용자: "중요한 메시지" 전송
↓
서버: DB에 저장 시작
↓
서버: WebSocket 전송 시도
↓
WebSocket 서버: 연결 끊김 (네트워크 문제)
↓
서버: 예외 발생 → 트랜잭션 롤백
↓
결과: 메시지가 DB에서 삭제됨 ❌
사용자: 메시지가 전송되지 않았음을 알 수 없음 😢
```

---

## 해결책: 업계 표준 패턴

### ✅ 올바른 패턴: DB 저장 → 커밋 → WebSocket 전송

```kotlin
@Transactional  // 트랜잭션 시작
fun editMessage(command: EditMessageCommand): ChatMessage {
    val message = messageRepository.save(updatedMessage)  // 1. DB 저장

    eventPublisher.publish(MessageEditedEvent(message))    // 2. 이벤트 발행

    return message
}  // 3. 트랜잭션 커밋 ✅

// 별도 리스너 (트랜잭션 외부)
@TransactionalEventListener(phase = AFTER_COMMIT)
fun handleMessageEdited(event: MessageEditedEvent) {
    webSocketBroker.sendMessage("/topic/chat", event.message)  // 4. WebSocket 전송
    // ⚠️ 실패해도 메시지는 이미 DB에 저장됨 ✅
}
```

### 🎯 핵심 원칙

```
1️⃣ DB에 영속화 (트랜잭션 내)
2️⃣ 트랜잭션 커밋
3️⃣ WebSocket 브로드캐스트 (트랜잭션 밖)
```

---

## 구현 방법

### 1. 도메인 이벤트 수정

이벤트에 WebSocket 전송에 필요한 메시지 객체 포함:

```kotlin
data class MessageEditedEvent(
    val messageId: MessageId,
    val roomId: ChatRoomId,
    val userId: UserId,
    val oldContent: String,
    val newContent: String,
    val message: ChatMessage,  // ✅ WebSocket 전송용
    val editedAt: Instant,
    override val occurredOn: Long = System.currentTimeMillis()
) : DomainEvent
```

### 2. 이벤트 리스너 생성

`@TransactionalEventListener`로 커밋 후 WebSocket 전송:

```kotlin
@Component
class MessageEventWebSocketListener(
    private val webSocketMessageBroker: WebSocketMessageBroker
) {
    private val logger = KotlinLogging.logger {}

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleMessageEdited(event: MessageEditedEvent) {
        try {
            // 트랜잭션 커밋 후 실행 → 메시지는 이미 DB에 안전하게 저장됨
            webSocketMessageBroker.sendMessage(
                "/topic/message/edit/${event.roomId.value}",
                event.message
            )

            logger.debug { "WebSocket 전송 완료: messageId=${event.messageId.value}" }
        } catch (e: Exception) {
            // WebSocket 실패는 로깅만 (메시지는 이미 DB에 있음)
            logger.error(e) {
                "WebSocket 전송 실패 (메시지는 DB에 저장됨): messageId=${event.messageId.value}"
            }
        }
    }
}
```

### 3. 서비스 리팩토링

WebSocket 코드 제거, 이벤트 발행만:

```kotlin
@Transactional
@UseCase
class EditMessageService(
    private val messageCommandPort: MessageCommandPort,
    private val eventPublisher: EventPublishPort
) : EditMessageUseCase {

    override fun editMessage(command: EditMessageCommand): ChatMessage {
        // 1. 메시지 수정
        val message = messageEditDomainService.editMessage(existing, command.newContent)

        // 2. DB에 영속화 (트랜잭션 내)
        val savedMessage = messageCommandPort.save(message)

        // 3. 이벤트 발행 (리스너가 트랜잭션 커밋 후 WebSocket 전송)
        eventPublisher.publish(
            MessageEditedEvent.create(
                messageId = savedMessage.id!!,
                roomId = savedMessage.roomId,
                userId = command.userId,
                oldContent = oldContent,
                newContent = savedMessage.content.text,
                message = savedMessage
            )
        )

        return savedMessage  // 4. 트랜잭션 커밋
    }
}
```

---

## 장점

### ✅ 메시지 유실 방지

```
WebSocket 실패 시나리오:

기존 패턴 (트랜잭션 내):
DB 저장 → WebSocket 실패 → 롤백 → 메시지 유실 ❌

새 패턴 (트랜잭션 밖):
DB 저장 → 커밋 ✅ → WebSocket 실패 → 메시지는 DB에 존재 ✅
```

### ✅ 복구 가능성

```kotlin
// 클라이언트가 재연결 시
fun reconnect() {
    val lastSyncedMessageId = getLastSyncedId()
    val missedMessages = api.getMessages(after = lastSyncedMessageId)
    // ✅ DB에 저장된 메시지를 가져올 수 있음
}
```

### ✅ 트랜잭션 독립성

- 외부 시스템(WebSocket, Redis, Kafka) 실패가 트랜잭션에 영향 없음
- 시스템 안정성 향상
- 각 컴포넌트가 독립적으로 실패/복구 가능

### ✅ 확장성

```
메시지 저장 (DB)
   ↓ (이벤트)
   ├── WebSocket 브로드캐스트
   ├── 알림 전송 (FCM, APNS)
   ├── 검색 인덱싱 (Elasticsearch)
   └── 분석 (Data Warehouse)

각 리스너가 독립적으로 동작, 하나가 실패해도 다른 것에 영향 없음
```

---

## 업계 사례

### Slack

> **Old System**: "If the web app went down, the channel server couldn't persist messages, but might still tell users they'd sent them."
>
> **New System**: "The old system showed messages fast, but sometimes dropped them. The new one does more work up front, but makes a stronger promise."

**메시지 흐름 (Slack 신시스템)**:
```
Client → HTTP API → Chat Service
                       ↓
                   DB에 저장 (영속화)
                       ↓
                   커밋 완료
                       ↓
                   Gateway Server
                       ↓
                   WebSocket 브로드캐스트
```

### 일반적인 메시징 패턴

> "The chat messages should be **persisted in the chat database BEFORE broadcasting** to the users for improved durability and fault tolerance."

**표준 메시지 흐름**:
```
1. Sender → Messaging API/WebSocket
2. Message Queue (Kafka, RabbitMQ)
3. Persistent Storage (SQL/NoSQL) ← 먼저 저장
4. Sender ← ACK (성공 확인)
5. WebSocket → Recipients (브로드캐스트)
```

### Discord & WhatsApp

- **공통점**: 모두 "영속화 우선" 패턴 사용
- **이유**: 수억 명의 사용자, 99.9% 이상 메시지 전달 보장 필요

---

## 코드 예제

### Before (잘못된 패턴)

```kotlin
@Transactional
class EditMessageService(
    private val messageCommandPort: MessageCommandPort,
    private val webSocketBroker: WebSocketMessageBroker  // ❌
) {
    override fun editMessage(command: EditMessageCommand): ChatMessage {
        val saved = messageCommandPort.save(message)

        // ❌ 트랜잭션 내에서 WebSocket 전송
        webSocketBroker.sendMessage("/topic/chat", saved)

        return saved
    }
}
```

**문제점**:
- WebSocket 실패 시 메시지 유실
- 트랜잭션과 외부 시스템 결합

### After (올바른 패턴)

```kotlin
@Transactional
class EditMessageService(
    private val messageCommandPort: MessageCommandPort,
    private val eventPublisher: EventPublishPort  // ✅
) {
    override fun editMessage(command: EditMessageCommand): ChatMessage {
        val saved = messageCommandPort.save(message)

        // ✅ 이벤트 발행만 (WebSocket은 리스너가 처리)
        eventPublisher.publish(MessageEditedEvent(saved))

        return saved
    }  // 트랜잭션 커밋
}

// ✅ 별도 리스너에서 WebSocket 처리
@Component
class MessageEventWebSocketListener(
    private val webSocketBroker: WebSocketMessageBroker
) {
    @TransactionalEventListener(phase = AFTER_COMMIT)
    fun handleMessageEdited(event: MessageEditedEvent) {
        webSocketBroker.sendMessage("/topic/chat", event.message)
    }
}
```

**장점**:
- 메시지는 항상 DB에 저장됨
- WebSocket 실패해도 안전
- 시스템 확장 가능

---

## 관련 파일

### 도메인 이벤트
- `domain/event/MessageEditedEvent.kt`
- `domain/event/MessageDeletedEvent.kt`

### 이벤트 리스너
- `application/service/message/listener/MessageEventWebSocketListener.kt`

### 서비스
- `application/service/message/EditMessageService.kt`
- `application/service/message/DeleteMessageService.kt`

---

## 참고 자료

### 업계 사례
- [Slack Architecture - Real-Time Messaging](https://slack.engineering/real-time-messaging/)
- [How Slack Supports Billions of Daily Messages](https://blog.bytebytego.com/p/how-slack-supports-billions-of-daily)
- [Designing a Real-time Chat App (WhatsApp, Slack)](https://codefarm0.medium.com/designing-a-real-time-chat-app-whatsapp-slack-bf17912356d7)

### Spring Framework
- [Spring @TransactionalEventListener Documentation](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html)
- [Transaction Management Best Practices](https://docs.spring.io/spring-framework/reference/data-access/transaction.html)

### 관련 문서
- `CLAUDE.md` - 프로젝트 아키텍처 및 설계 원칙
- `DOMAIN.md` - 도메인 모델 및 비즈니스 규칙

---

**✅ 결론**: 이 패턴은 Slack, Discord 등 프로덕션 시스템에서 검증된 표준이며, 메시지 유실을 방지하고 시스템 안정성을 크게 향상시킵니다.
