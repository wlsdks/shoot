# Context Map & Integration Patterns

> DDD Context Map: Shoot 실시간 채팅 애플리케이션

**작성일:** 2025-11-02

---

## 🗺️ Context Map

### 전체 Context 관계도:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                                                                           │
│                         Identity & Access Context                         │
│                                (User)                                     │
│                                                                           │
│  Aggregates: User, RefreshToken                                          │
│  Database: PostgreSQL (users, refresh_tokens)                            │
│  Events: UserCreatedEvent, UserDeletedEvent                              │
│                                                                           │
└───────────────────────────┬───────────────────────────────────────────────┘
                            │
                            │ Shared Kernel (UserId)
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ↓                   ↓                   ↓
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│     Social    │   │   ChatRoom    │   │   Messaging   │
│   (Friends)   │   │ (Conversation)│   │  (Messages)   │
│               │   │               │   │               │
│ Aggregates:   │   │ Aggregates:   │   │ Aggregates:   │
│ FriendRequest │   │ ChatRoom      │   │ ChatMessage   │
│ Friendship    │   │               │   │               │
│ BlockedUser   │   │ DB: PostgreSQL│   │ DB: MongoDB   │
│ FriendGroup   │   │               │   │               │
│               │   │ Events:       │   │ Events:       │
│ DB: PostgreSQL│   │ ChatRoom-     │   │ MessageSent-  │
│               │   │ CreatedEvent  │   │ Event         │
│ Events:       │   │               │   │               │
│ Friend-       │───┼───────────────┼───│───────────────│→ 1:1 채팅방
│ AddedEvent    │   │               │   │  자동 생성    │
│               │   │               │   │               │
└───────┬───────┘   └───────┬───────┘   └───────┬───────┘
        │                   │                   │
        │                   │                   │
        │         Publisher-Subscriber          │
        │             (Domain Events)           │
        │                   │                   │
        └───────────────────┼───────────────────┘
                            ↓
                ┌───────────────────────┐
                │    Notification       │
                │                       │
                │ Aggregates:           │
                │ Notification          │
                │ NotificationSettings  │
                │                       │
                │ DB: PostgreSQL        │
                │                       │
                │ Pattern:              │
                │ Anti-Corruption Layer │
                └───────────────────────┘
```

---

## 📊 Context 간 관계 상세

### 1. Identity ↔ Social (Shared Kernel)

**관계:** Shared Kernel (UserId)

**통합 방식:**
```kotlin
// Social Context가 Identity의 UserId를 직접 사용
data class FriendRequest(
    val senderId: UserId,  // Identity Context의 Value Object
    val receiverId: UserId
)
```

**패턴:**
- **Shared Kernel**: UserId가 공통 Value Object
- **Conformist**: Social이 Identity의 모델을 그대로 사용

**장점:**
- ✅ 간단하고 명확한 참조
- ✅ 타입 안전성 보장

**단점:**
- ⚠️ Identity 변경 시 Social에 영향

**개선 필요:**
- 없음 (적절한 패턴 사용)

---

### 2. Identity ↔ ChatRoom (Shared Kernel)

**관계:** Shared Kernel (UserId)

**통합 방식:**
```kotlin
// ChatRoom이 participants로 UserId 집합 사용
data class ChatRoom(
    val participants: Set<UserId>  // Identity Context 참조
)
```

**패턴:**
- **Shared Kernel**: UserId 공유
- **Customer-Supplier**: ChatRoom이 User 존재 여부에 의존

**장점:**
- ✅ 참여자 관리 간단

**단점:**
- ⚠️ User 삭제 시 ChatRoom 정리 필요 → CASCADE DELETE

**개선:**
- ✅ 현재 UserDeletedEvent로 처리 중

---

### 3. Identity ↔ Messaging (Shared Kernel)

**관계:** Shared Kernel (UserId)

**통합 방식:**
```kotlin
data class ChatMessage(
    val senderId: UserId,  // 발신자
    val mentions: Set<UserId>,  // 멘션
    val readBy: Map<UserId, Boolean>  // 읽음 상태
)
```

**패턴:**
- **Shared Kernel**: UserId 공유
- **Customer-Supplier**: Message가 User 존재 여부에 의존

**문제점:**
- ⚠️ User 삭제 시 Message 정리 필요
- ⚠️ readBy Map이 메모리 소모 가능성

**개선:**
- ✅ 현재 UserDeletedEvent → MongoDB 메시지 소프트 삭제 구현됨

---

### 4. **Social → ChatRoom (Publisher-Subscriber)** ✅

**관계:** 이벤트 기반 통합

**통합 방식:**
```kotlin
// Social Context
class FriendReceiveService {
    fun acceptFriendRequest() {
        // ... 친구 관계 생성
        publishEvent(FriendAddedEvent(userId, friendId))
    }
}

// ChatRoom Context
@TransactionalEventListener
class FriendAddedEventListener {
    fun handleFriendAdded(event: FriendAddedEvent) {
        // 1:1 채팅방 자동 생성
        createChatRoomUseCase.createDirectChat(
            CreateDirectChatCommand(event.userId, event.friendId)
        )
    }
}
```

**패턴:**
- **Publisher-Subscriber**: 느슨한 결합
- **Customer-Supplier**: ChatRoom이 Social 이벤트 구독

**장점:**
- ✅ 느슨한 결합 (Social은 ChatRoom 존재 몰라도 됨)
- ✅ 비동기 처리
- ✅ 확장 가능 (다른 리스너 추가 쉬움)

**단점:**
- ⚠️ 결과적 일관성 (즉시 채팅방 생성 안 될 수 있음)

---

### 5. **Messaging ↔ ChatRoom (Conformist + Saga)** ⚠️

**관계:** 강한 결합 (Saga 패턴)

**통합 방식:**
```kotlin
// Messaging Context
class MessageSagaOrchestrator {
    fun execute(message: ChatMessage) {
        // Step 1: MongoDB에 메시지 저장
        saveMessageToMongoStep.execute()

        // Step 2: ChatRoom 메타데이터 업데이트 (강한 결합!)
        updateChatRoomMetadataStep.execute() {
            val chatRoom = chatRoomQueryPort.findById(message.roomId)  // 직접 조회
            chatRoom.update(lastMessageId = message.id)  // 직접 수정
            chatRoomCommandPort.save(chatRoom)  // 직접 저장
        }

        // Step 3: Outbox 저장
        publishEventToOutboxStep.execute()
    }
}
```

**패턴:**
- **Conformist**: Messaging이 ChatRoom 모델을 그대로 사용
- **Shared Kernel**: MessageId 공유
- **Saga**: 분산 트랜잭션 패턴

**문제점:**
- ❌ **양방향 의존성**
  - Message → ChatRoom (roomId 참조)
  - ChatRoom → Message (lastMessageId 참조)
- ❌ **트랜잭션 경계 복잡** (MongoDB + PostgreSQL Saga)
- ❌ **보상 로직 복잡** (OptimisticLock 처리)
- ❌ **테스트 어려움** (두 Context 동시 필요)

**개선 제안:**
```kotlin
// Publisher-Subscriber 패턴으로 전환
class SendMessageService {
    fun sendMessage(command: SendMessageCommand): ChatMessage {
        // MongoDB에만 저장
        val savedMessage = messageCommandPort.save(message)

        // 이벤트 발행 (느슨한 결합)
        publishEvent(MessageSentEvent(
            messageId = savedMessage.id,
            roomId = savedMessage.roomId,
            senderId = savedMessage.senderId,
            sentAt = savedMessage.createdAt
        ))

        return savedMessage
    }
}

// ChatRoom Context가 이벤트 구독
@TransactionalEventListener
class MessageSentEventListener {
    fun handleMessageSent(event: MessageSentEvent) {
        val chatRoom = chatRoomQueryPort.findById(event.roomId)
        chatRoom.update(
            lastMessageId = event.messageId,
            lastActiveAt = event.sentAt
        )
        chatRoomCommandPort.save(chatRoom)
    }
}
```

**개선 후 장점:**
- ✅ 느슨한 결합 (Message는 ChatRoom 몰라도 됨)
- ✅ 독립적 트랜잭션 (MongoDB, PostgreSQL 분리)
- ✅ 보상 로직 불필요 (재시도로 해결)
- ✅ 테스트 간단 (각 Context 독립)

---

### 6. **Messaging → Notification (Publisher-Subscriber)** ✅

**관계:** 이벤트 기반 통합

**통합 방식:**
```kotlin
// Messaging Context
publishEvent(MessageSentEvent(messageId, roomId, senderId))

// Notification Context
@TransactionalEventListener
class MessageSentEventListener {
    fun handleMessageSent(event: MessageSentEvent) {
        // Mention 알림 생성
        if (message.mentions.isNotEmpty()) {
            message.mentions.forEach { mentionedUserId ->
                notificationCommandPort.save(
                    Notification.create(
                        userId = mentionedUserId,
                        type = NotificationType.MENTION,
                        sourceType = SourceType.MESSAGE,
                        sourceId = event.messageId.value.toString()
                    )
                )
            }
        }
    }
}
```

**패턴:**
- **Publisher-Subscriber**: 느슨한 결합
- **Anti-Corruption Layer**: Notification이 자신의 모델로 변환

**장점:**
- ✅ 완벽한 분리 (Messaging은 Notification 몰라도 됨)
- ✅ 비동기 처리
- ✅ 확장 가능

---

### 7. **Social → Notification (Publisher-Subscriber)** ✅

**관계:** 이벤트 기반 통합

**통합 방식:**
```kotlin
// Social Context
publishEvent(FriendRequestSentEvent(senderId, receiverId))

// Notification Context
@TransactionalEventListener
class FriendRequestSentEventListener {
    fun handleFriendRequestSent(event: FriendRequestSentEvent) {
        notificationCommandPort.save(
            Notification.create(
                userId = event.receiverId,
                type = NotificationType.FRIEND_REQUEST,
                sourceType = SourceType.USER,
                sourceId = event.senderId.value.toString()
            )
        )
    }
}
```

**패턴:**
- **Publisher-Subscriber**: 느슨한 결합
- **Anti-Corruption Layer**: sourceId String 변환

**장점:**
- ✅ 완벽한 분리

---

### 8. **ChatRoom → Notification (Publisher-Subscriber)** ✅

**관계:** 이벤트 기반 통합

**통합 방식:**
```kotlin
// ChatRoom Context
publishEvent(ChatRoomCreatedEvent(roomId, createdBy, participants))

// Notification Context
@TransactionalEventListener
class ChatRoomCreatedEventListener {
    fun handleChatRoomCreated(event: ChatRoomCreatedEvent) {
        event.participants.forEach { participantId ->
            notificationCommandPort.save(
                Notification.create(
                    userId = participantId,
                    type = NotificationType.CHAT_ROOM_CREATED,
                    sourceType = SourceType.CHAT_ROOM,
                    sourceId = event.roomId.value.toString()
                )
            )
        }
    }
}
```

**패턴:**
- **Publisher-Subscriber**: 느슨한 결합

---

## 📈 통합 패턴 분류

### Shared Kernel (공유 커널)

**사용 위치:**
- Identity ↔ All Contexts (UserId)
- Messaging ↔ ChatRoom (MessageId)
- All Contexts (DomainEvent, Constants)

**특징:**
- ✅ 양쪽이 합의한 공통 모델
- ⚠️ 변경 시 양쪽 영향

**평가:** ✅ 적절히 사용됨

---

### Publisher-Subscriber (발행-구독)

**사용 위치:**
- Social → ChatRoom (FriendAddedEvent)
- Social → Notification (FriendRequestSentEvent)
- Messaging → Notification (MessageSentEvent)
- ChatRoom → Notification (ChatRoomCreatedEvent)

**특징:**
- ✅ 느슨한 결합
- ✅ 비동기 처리
- ✅ 확장 가능

**평가:** ✅ 매우 잘 구현됨

---

### Conformist (순응자)

**사용 위치:**
- ⚠️ Messaging → ChatRoom (Saga 패턴)

**특징:**
- ❌ 강한 결합
- ❌ 하류 Context가 상류 모델에 의존

**평가:** ⚠️ 개선 필요 (Publisher-Subscriber로 전환)

---

### Anti-Corruption Layer (ACL)

**사용 위치:**
- ✅ Notification Context (sourceId String 변환)

**특징:**
- ✅ 외부 모델을 자신의 모델로 변환
- ✅ 오염 방지

**평가:** ✅ 적절히 사용됨

---

## 🎯 Context 독립성 평가

| Context | 독립 배포 가능 | 독립 테스트 가능 | 독립 DB | 이벤트 기반 | 점수 |
|---------|-------------|--------------|--------|----------|------|
| Identity | ⚠️ 3/5 | ✅ 5/5 | ✅ 5/5 | ✅ 5/5 | **18/20** |
| Social | ✅ 4/5 | ✅ 5/5 | ✅ 5/5 | ✅ 5/5 | **19/20** |
| Messaging | ❌ 1/5 | ⚠️ 2/5 | ✅ 5/5 | ⚠️ 2/5 | **10/20** |
| ChatRoom | ⚠️ 2/5 | ⚠️ 3/5 | ✅ 5/5 | ✅ 5/5 | **15/20** |
| Notification | ✅ 4/5 | ✅ 5/5 | ✅ 5/5 | ✅ 5/5 | **19/20** |

**평균:** **16.2/20** (81%)

---

## 🚀 개선 우선순위

### Priority 1: Messaging ↔ ChatRoom 결합 제거

**현재 문제:**
- Saga 패턴으로 강한 결합
- 양방향 의존성
- 복잡한 트랜잭션 경계

**개선 방법:**
```kotlin
// Before (Saga)
SendMessageService → MessageSagaOrchestrator
  ├─> SaveMessageToMongoStep (MongoDB)
  ├─> UpdateChatRoomMetadataStep (PostgreSQL) ← 강한 결합
  └─> PublishEventToOutboxStep (PostgreSQL)

// After (Event-driven)
SendMessageService
  └─> MongoDB 저장
  └─> MessageSentEvent 발행

ChatRoomMetadataUpdateListener (독립)
  └─> MessageSentEvent 구독
  └─> PostgreSQL 업데이트

OutboxPublisher (독립)
  └─> MessageSentEvent 구독
  └─> Outbox 저장
```

**예상 효과:**
- ✅ 독립 배포 가능 (+4점)
- ✅ 독립 테스트 가능 (+3점)
- ✅ 이벤트 기반 통합 (+3점)
- 🎯 **Messaging Context 점수: 10/20 → 20/20**

---

### Priority 2: Notification Context 타입 안전성

**현재 문제:**
```kotlin
data class Notification(
    val sourceId: String,  // 타입 안전성 없음
    val sourceType: SourceType
)
```

**개선 방법:**
```kotlin
sealed class NotificationSource {
    data class FriendSource(val friendRequestId: FriendRequestId) : NotificationSource()
    data class MessageSource(val messageId: MessageId, val roomId: ChatRoomId) : NotificationSource()
    data class ChatRoomSource(val roomId: ChatRoomId) : NotificationSource()
}

data class Notification(
    val id: NotificationId,
    val userId: UserId,
    val type: NotificationType,
    val source: NotificationSource  // 타입 안전
)
```

---

### Priority 3: ChatRoom 이름 변경

**제안:** `ChatRoom` → `Conversation`

**이유:**
- "Room"은 물리적 공간 느낌
- "Conversation"이 도메인 개념에 더 적합
- 1:1 채팅도 자연스럽게 표현

---

## 📝 결론

### 현재 상태:
- ✅ **이벤트 기반 통합이 대부분 잘 구현됨**
- ✅ **Publisher-Subscriber 패턴 우수**
- ⚠️ **Messaging ↔ ChatRoom 강한 결합** (Saga)
- ✅ **대부분 Context가 독립적**

### 개선 후 예상 상태:
- ✅ **모든 Context 이벤트 기반 통합**
- ✅ **독립 배포 가능**
- ✅ **독립 테스트 가능**
- ✅ **평균 점수: 81% → 95%**

---

**작성자:** Claude Code
**검토 날짜:** 2025-11-02
**다음 단계:** Messaging ↔ ChatRoom 결합 제거 구현
