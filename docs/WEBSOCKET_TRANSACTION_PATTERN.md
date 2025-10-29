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

### ❌ 잘못된 패턴: 저장 작업과 WebSocket 전송 결합

```kotlin
fun editMessage(command: EditMessageCommand): ChatMessage {
    val message = messageRepository.save(updatedMessage)  // 1. MongoDB 저장

    webSocketBroker.sendMessage("/topic/chat", message)   // 2. WebSocket 전송
    // ⚠️ WebSocket 실패 시 예외 발생 → 메서드 실패 → 메시지 유실!

    return message
}
```

**MongoDB 컨텍스트**: MongoDB 단일 document 저장은 atomic하지만, WebSocket 전송이 같은 메서드에 있으면 WebSocket 실패 시 예외가 전파되어 전체 작업이 실패한 것처럼 보일 수 있습니다.

### 🚨 문제점

| 문제 | 설명 | 영향 |
|------|------|------|
| **메시지 유실 위험** | WebSocket 전송 실패 시 예외 전파로 작업 실패 | 사용자가 보낸 메시지가 저장되지 않음 |
| **작업 결합** | 외부 시스템(WebSocket)과 저장 작업 결합 | 시스템 안정성 저하 |
| **복구 불가능** | 메시지가 MongoDB에 없으면 재전송 불가 | 데이터 손실 |

### 실제 시나리오

```
사용자: "중요한 메시지" 전송
↓
서버: MongoDB에 저장 시작
↓
서버: WebSocket 전송 시도
↓
WebSocket 서버: 연결 끊김 (네트워크 문제)
↓
서버: 예외 발생 → 메서드 실패
↓
결과: MongoDB 저장도 실패 (save() 전에 실패) ❌
     또는 저장 성공했지만 예외로 인해 실패로 처리됨
사용자: 메시지가 전송되지 않았음을 알 수 없음 😢
```

---

## 해결책: 업계 표준 패턴

### ✅ 올바른 패턴: MongoDB 저장 → 이벤트 발행 → 비동기 WebSocket 전송

```kotlin
// @Transactional 불필요 (MongoDB 단일 document는 atomic)
fun editMessage(command: EditMessageCommand): ChatMessage {
    val message = messageRepository.save(updatedMessage)  // 1. MongoDB 저장 (atomic)

    eventPublisher.publish(MessageEditedEvent(message))    // 2. 이벤트 발행

    return message  // 3. 저장 완료, 즉시 반환 ✅
}

// 별도 리스너 (비동기 실행)
@Async
@EventListener
fun handleMessageEdited(event: MessageEditedEvent) {
    // 4. 별도 스레드에서 WebSocket 전송
    webSocketBroker.sendMessage("/topic/chat", event.message)
    // ⚠️ 실패해도 메시지는 이미 MongoDB에 저장됨 ✅
}
```

### 🎯 핵심 원칙

```
1️⃣ MongoDB에 영속화 (단일 document는 atomic 보장)
2️⃣ 이벤트 발행 (Spring ApplicationEvent)
3️⃣ 비동기 WebSocket 브로드캐스트 (@Async + @EventListener)

💡 MongoDB 특성: 단일 document 작업은 트랜잭션 없이도 atomic
💡 @Async 사용: @TransactionalEventListener는 트랜잭션 없으면 동작 안함
                @Async + @EventListener로 비동기 처리
```

---

## 구현 방법

### 1. Async 설정 추가

비동기 처리를 위한 설정 추가:

```kotlin
@Configuration
@EnableAsync
class AsyncConfig : AsyncConfigurer {

    @Bean(name = ["taskExecutor"])
    override fun getAsyncExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 5
        executor.maxPoolSize = 20
        executor.queueCapacity = 100
        executor.setThreadNamePrefix("async-")
        executor.initialize()
        return executor
    }

    override fun getAsyncUncaughtExceptionHandler(): AsyncUncaughtExceptionHandler {
        return AsyncUncaughtExceptionHandler { ex, method, params ->
            logger.error(ex) { "Async method failed: ${method.name}" }
        }
    }
}
```

### 2. 도메인 이벤트 수정

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

### 3. 이벤트 리스너 생성

`@Async + @EventListener`로 비동기 WebSocket 전송:

```kotlin
@Component
class MessageEventWebSocketListener(
    private val webSocketMessageBroker: WebSocketMessageBroker
) {
    private val logger = KotlinLogging.logger {}

    @Async
    @EventListener
    fun handleMessageEdited(event: MessageEditedEvent) {
        try {
            // 별도 스레드에서 실행 → 메시지는 이미 MongoDB에 저장됨
            webSocketMessageBroker.sendMessage(
                "/topic/message/edit/${event.roomId.value}",
                event.message
            )

            logger.debug { "WebSocket 전송 완료: messageId=${event.messageId.value}" }
        } catch (e: Exception) {
            // WebSocket 실패는 로깅만 (메시지는 이미 MongoDB에 있음)
            logger.error(e) {
                "WebSocket 전송 실패 (메시지는 MongoDB에 저장됨): messageId=${event.messageId.value}"
            }
        }
    }
}
```

### 4. 서비스 리팩토링

WebSocket 코드 제거, 이벤트 발행만:

```kotlin
@UseCase  // @Transactional 불필요 (MongoDB 단일 document는 atomic)
class EditMessageService(
    private val messageCommandPort: MessageCommandPort,
    private val eventPublisher: EventPublishPort
) : EditMessageUseCase {

    override fun editMessage(command: EditMessageCommand): ChatMessage {
        // 1. 메시지 수정
        val message = messageEditDomainService.editMessage(existing, command.newContent)

        // 2. MongoDB에 영속화 (atomic 작업)
        val savedMessage = messageCommandPort.save(message)

        // 3. 이벤트 발행 (리스너가 저장 완료 후 WebSocket 전송)
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

        return savedMessage  // 4. 저장 완료
    }
}
```

---

## 장점

### ✅ 메시지 유실 방지

```
WebSocket 실패 시나리오:

기존 패턴 (저장과 전송 결합):
MongoDB 저장 → WebSocket 실패 → 예외 발생 → 메시지 유실 ❌

새 패턴 (저장 완료 후 전송):
MongoDB 저장 → 완료 ✅ → WebSocket 실패 → 메시지는 MongoDB에 존재 ✅

💡 MongoDB 단일 document 작업은 atomic하므로 save() 성공 시 즉시 영속화됨
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

### ✅ 작업 독립성

- 외부 시스템(WebSocket, Redis, Kafka) 실패가 저장 작업에 영향 없음
- 시스템 안정성 향상
- 각 컴포넌트가 독립적으로 실패/복구 가능
- MongoDB 저장 성공 여부와 WebSocket 전송 성공 여부가 분리됨

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
@UseCase
class EditMessageService(
    private val messageCommandPort: MessageCommandPort,
    private val webSocketBroker: WebSocketMessageBroker  // ❌
) {
    override fun editMessage(command: EditMessageCommand): ChatMessage {
        val saved = messageCommandPort.save(message)  // MongoDB 저장

        // ❌ 저장 작업과 WebSocket 전송이 결합됨
        webSocketBroker.sendMessage("/topic/chat", saved)

        return saved
    }
}
```

**문제점**:
- WebSocket 실패 시 예외 발생으로 메시지 유실 가능
- 저장 작업과 외부 시스템(WebSocket) 결합
- WebSocket 장애가 메시지 저장에 영향

### After (올바른 패턴)

```kotlin
@UseCase  // @Transactional 불필요 (MongoDB 단일 document는 atomic)
class EditMessageService(
    private val messageCommandPort: MessageCommandPort,
    private val eventPublisher: EventPublishPort  // ✅
) {
    override fun editMessage(command: EditMessageCommand): ChatMessage {
        val saved = messageCommandPort.save(message)  // MongoDB 저장 (atomic)

        // ✅ 이벤트 발행만 (WebSocket은 리스너가 처리)
        eventPublisher.publish(MessageEditedEvent(saved))

        return saved  // 저장 완료
    }
}

// ✅ 별도 리스너에서 WebSocket 처리 (비동기)
@Component
class MessageEventWebSocketListener(
    private val webSocketBroker: WebSocketMessageBroker
) {
    @Async
    @EventListener
    fun handleMessageEdited(event: MessageEditedEvent) {
        // 별도 스레드에서 실행
        webSocketBroker.sendMessage("/topic/chat", event.message)
    }
}
```

**장점**:
- 메시지는 항상 MongoDB에 저장됨 (atomic 보장)
- WebSocket 실패해도 메시지 안전
- 저장 작업과 WebSocket 전송이 독립적
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

**💡 MongoDB + Spring Events 컨텍스트**:
- MongoDB 단일 document 작업은 atomic하므로 @Transactional 불필요
- @TransactionalEventListener는 트랜잭션 없으면 이벤트가 무시됨 (discarded)
- **해결책**: @Async + @EventListener 패턴 사용
  - MongoDB 저장 완료 후 이벤트 발행
  - 별도 스레드에서 비동기 WebSocket 전송
  - Slack/Discord 패턴과 정확히 일치 (저장 → 브로드캐스트 분리)
  - API 응답 속도 향상 (WebSocket 전송 대기 불필요)

**관련 파일**:
- `infrastructure/config/AsyncConfig.kt` - 비동기 설정
- `application/service/message/listener/MessageEventWebSocketListener.kt` - WebSocket 리스너
