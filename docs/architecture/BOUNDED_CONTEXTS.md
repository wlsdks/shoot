# Bounded Context 분석 및 정의

> DDD 아키텍처 분석: Shoot 실시간 채팅 애플리케이션

**작성일:** 2025-11-02
**분석 범위:** Domain 모델, Aggregate, Entity, 이벤트, 서비스 레이어

---

## 📋 Executive Summary

### 현재 상태:
- ⚠️ **Bounded Context 분리가 명확하지 않음**
- ⚠️ **도메인 경계가 모호한 부분 존재**
- ✅ **Aggregate Root는 대체로 잘 정의됨**
- ⚠️ **일부 Context 간 강한 결합 존재**

### 주요 발견 사항:
1. **User Context와 ChatRoom Context 강한 결합**
2. **Message와 ChatRoom의 경계 모호**
3. **Notification Context의 역할 불분명**
4. **Saga 패턴으로 인한 복잡한 의존성**

---

## 🎯 식별된 Bounded Context

현재 코드베이스에서 6개의 Bounded Context를 식별했습니다:

### 1. **Identity & Access Context** (사용자 및 인증)

**핵심 도메인 개념:**
- 사용자 계정 및 인증
- 사용자 프로필 및 상태
- 리프레시 토큰 관리

**Aggregate Roots:**
- `User` (Aggregate Root)
  - UserId (식별자)
  - Username, Nickname, UserCode
  - UserStatus (ONLINE, OFFLINE, BUSY, AWAY)
  - Profile (profileImageUrl, backgroundImageUrl, bio)

- `RefreshToken` (Aggregate Root)
  - 사용자 세션 관리
  - 토큰 갱신

**도메인 이벤트:**
- `UserCreatedEvent`
- `UserDeletedEvent`

**비즈니스 규칙:**
- Username: 3-20자
- Nickname: 1-30자
- UserCode: 8자리 대문자+숫자, 중복 불가
- 비밀번호: 최소 8자

**서비스:**
- `UserCreateService`
- `UserDeleteService`
- `UpdateUserProfileService`
- `LoginService`
- `RefreshTokenService`

**문제점:**
- ⚠️ User가 다른 Context에서 너무 많이 참조됨
- ⚠️ UserStatus가 활성 사용자 추적에도 사용됨 (책임 분산 필요)

---

### 2. **Social Context** (친구 및 소셜 관계)

**핵심 도메인 개념:**
- 친구 관계 및 요청
- 사용자 차단
- 친구 그룹 관리

**Aggregate Roots:**
- `FriendRequest` (Aggregate Root)
  - senderId, receiverId
  - FriendRequestStatus (PENDING, ACCEPTED, REJECTED, CANCELLED)
  - 생성일, 응답일

- `Friendship` (Aggregate Root)
  - userId, friendId
  - 양방향 관계 (2개 레코드 생성)

- `BlockedUser` (Aggregate Root)
  - blockerId, blockedUserId

- `FriendGroup` (Aggregate Root)
  - userId, groupName
  - 친구 그룹 관리

**도메인 이벤트:**
- `FriendRequestSentEvent`
- `FriendRequestCancelledEvent`
- `FriendRequestRejectedEvent`
- `FriendAddedEvent`
- `FriendRemovedEvent`

**비즈니스 규칙:**
- 자기 자신에게 친구 요청 불가
- 이미 친구인 경우 요청 불가
- 중복 요청 불가
- 최대 친구 수: 1,000명
- PENDING 상태에서만 처리 가능

**서비스:**
- `FriendRequestService` (요청 전송, 취소)
- `FriendReceiveService` (수락, 거절)
- `FriendRemoveService`
- `BlockUserService`
- `FriendGroupService`
- `RecommendFriendService` (BFS 알고리즘, depth 3)

**문제점:**
- ✅ Bounded Context 분리가 명확함
- ⚠️ User Context와의 의존성이 강함 (UserId 참조)
- ✅ 이벤트 기반 통합 잘 구현됨

---

### 3. **Messaging Context** (메시지 및 채팅)

**핵심 도메인 개념:**
- 채팅 메시지 전송, 수정, 삭제
- 메시지 리액션 및 멘션
- 메시지 읽음 상태
- 메시지 고정 및 북마크
- 스레드 및 답장

**Aggregate Roots:**
- `ChatMessage` (Aggregate Root)
  - MessageId (MongoDB ObjectId)
  - roomId (ChatRoomId 참조)
  - senderId (UserId 참조)
  - MessageContent (text, isDeleted, type)
  - MessageStatus (SENDING, SENT_TO_KAFKA, PROCESSING, SAVED, FAILED)
  - MessageReactions (Map<ReactionType, Set<UserId>>)
  - readBy (Map<UserId, Boolean>)
  - replyToMessageId, threadId
  - isPinned, pinnedBy, pinnedAt
  - mentions (Set<UserId>)

- `MessageBookmark` (Aggregate Root)
  - userId, messageId, roomId

**Value Objects:**
- `MessageContent` (text, type, isDeleted, deletedAt)
- `MessageReactions`
- `ChatMessageMetadata`

**도메인 이벤트:**
- `MessageSentEvent`
- `MessageEditedEvent`
- `MessageDeletedEvent`
- `MessageReactionEvent`
- `MessageBulkReadEvent`
- `MessagePinEvent`
- `MentionEvent`

**비즈니스 규칙:**
- 최대 길이: 4,000자
- 최대 첨부파일: 50MB
- 수정 시간 제한: 24시간 (생성 후)
- TEXT 타입만 수정 가능
- 삭제된 메시지 수정 불가
- 빈 내용으로 수정 불가
- 소프트 삭제 (isDeleted 플래그)

**서비스:**
- `SendMessageService` (Saga 패턴)
- `EditMessageService`
- `DeleteMessageService`
- `ToggleMessageReactionService`
- `MarkMessageAsReadService`
- `PinMessageService`
- `BookmarkMessageService`
- `ForwardMessageService`
- `ScheduledMessageService`
- `ThreadMessageService`

**Saga 통합:**
- `MessageSagaOrchestrator`
  - Step 1: SaveMessageToMongoStep (MongoDB)
  - Step 2: UpdateChatRoomMetadataStep (PostgreSQL)
  - Step 3: PublishEventToOutboxStep (PostgreSQL)

**문제점:**
- ⚠️ ChatRoom과의 경계가 모호함
  - ChatMessage가 roomId를 직접 참조
  - ChatRoom 메타데이터를 Message 전송 시 업데이트 (강한 결합)
- ⚠️ User Context와 강한 결합 (senderId, mentions, readBy)
- ✅ MongoDB 사용으로 독립적인 저장소 분리
- ⚠️ Saga 패턴으로 인한 복잡한 트랜잭션 경계

**개선 제안:**
```
현재: Message → ChatRoom 직접 업데이트 (Saga)
제안: Message → MessageSentEvent → ChatRoom이 이벤트 구독
```

---

### 4. **ChatRoom Context** (채팅방 관리)

**핵심 도메인 개념:**
- 채팅방 생성 및 관리
- 참여자 관리
- 채팅방 설정
- 읽음 상태 추적

**Aggregate Roots:**
- `ChatRoom` (Aggregate Root)
  - ChatRoomId
  - ChatRoomTitle
  - ChatRoomType (DIRECT, GROUP)
  - participants (Set<UserId>)
  - lastMessageId (MessageId 참조)
  - lastActiveAt
  - announcement
  - pinnedParticipants (Set<UserId>)

- `ChatRoomSettings` (Entity, ChatRoom의 일부)
  - isNotificationEnabled (기본: true)
  - retentionDays (메시지 보존 기간, 기본: null = 무기한)
  - isEncrypted (기본: false)
  - customSettings (Map<String, Any>)

**도메인 이벤트:**
- `ChatRoomCreatedEvent`
- `ChatRoomParticipantChangedEvent`
- `ChatRoomTitleChangedEvent`

**비즈니스 규칙:**
- 1:1 채팅: 정확히 2명
- 그룹 채팅: 2~100명
- 자기 자신과 채팅 불가
- 참여자 0명이면 자동 삭제
- 최대 핀 채팅방: 사용자별 5개 (PostgreSQL 트리거로 강제)
- 최대 고정 메시지: 5개

**서비스:**
- `CreateChatRoomService`
- `ManageChatRoomService` (참여자 추가/제거, 제목/공지사항 업데이트)
- `UpdateChatRoomFavoriteService`
- `ChatRoomSearchService`

**문제점:**
- ⚠️ **Message Context와 경계 모호**
  - ChatRoom이 lastMessageId를 직접 저장 (Message 의존)
  - Message 전송 시 ChatRoom 메타데이터 업데이트 (양방향 의존)
- ⚠️ **User Context와 강한 결합**
  - participants가 Set<UserId> (직접 참조)
- ✅ PostgreSQL 사용으로 트랜잭션 일관성 보장

**개선 제안:**
```
현재: ChatRoom.lastMessageId = MessageId (직접 참조)
제안:
  1. 이벤트 기반: MessageSentEvent → ChatRoomMetadataUpdated
  2. Read Model: ChatRoomReadModel에 lastMessage 정보 저장
```

---

### 5. **Notification Context** (알림)

**핵심 도메인 개념:**
- 시스템 알림 생성 및 전달
- 알림 읽음 상태 관리

**Aggregate Roots:**
- `Notification` (Aggregate Root)
  - NotificationId
  - userId (UserId 참조)
  - title, message
  - NotificationType (FRIEND_REQUEST, FRIEND_ACCEPTED, MESSAGE, MENTION, SYSTEM)
  - SourceType (USER, MESSAGE, CHAT_ROOM, FRIEND, SYSTEM)
  - sourceId (외부 엔티티 ID)
  - isRead, readAt
  - isDeleted, deletedAt

- `NotificationSettings` (Aggregate Root)
  - userId
  - enablePush, enableEmail, enableWebSocket
  - mutedChatRooms (Set<ChatRoomId>)
  - mutedUsers (Set<UserId>)

**도메인 이벤트:**
- `NotificationEvent` (생성, 읽음, 삭제)

**비즈니스 규칙:**
- 알림은 사용자별로 독립적
- sourceId로 원본 엔티티 추적
- 소프트 삭제

**서비스:**
- `CreateNotificationService`
- `ReadNotificationService`
- `DeleteNotificationService`
- `UpdateNotificationSettingsService`

**이벤트 리스너:** (다른 Context 이벤트 구독)
- `FriendRequestSentEventListener` → Notification 생성
- `FriendAddedEventListener` → Notification 생성
- `MessageSentEventListener` → Mention 알림 생성
- `ChatRoomCreatedEventListener` → 참여자 알림

**문제점:**
- ⚠️ **Generic Notification 모델**
  - sourceId가 String (타입 안전성 없음)
  - 다양한 소스 타입을 하나의 모델로 처리 (SRP 위반 가능)
- ✅ 이벤트 기반 통합으로 느슨한 결합
- ⚠️ 모든 Context에 의존 (User, Message, ChatRoom, Friend)

**개선 제안:**
```
현재: 단일 Notification 엔티티로 모든 알림 처리
제안:
  1. 알림 타입별 별도 Aggregate (FriendNotification, MessageNotification)
  2. 또는 Notification을 읽기 전용 Read Model로 취급
```

---

### 6. **Shared Kernel** (공유 커널)

**핵심 요소:**
- `DomainEvent` (Base interface)
- `DomainConstants` (도메인 상수)
- `SagaOrchestrator`, `SagaStep` (Saga 인프라)
- Domain Exceptions (공통 예외)
- Value Objects (UserId, ChatRoomId, MessageId 등)

**문제점:**
- ⚠️ **Value Object가 실제로는 Primitive Obsession**
  - UserId, ChatRoomId 등이 단순 Long wrapper
  - 도메인 로직이 거의 없음
- ✅ DomainEvent 추상화는 적절함
- ⚠️ Saga 인프라가 Shared Kernel에 있음 (Messaging Context에 속해야 함)

---

## 🔍 Context 간 의존성 분석

### 의존성 다이어그램:

```
┌─────────────────────────────────────────────────────────────┐
│                     Identity & Access                        │
│                        (User)                                 │
└─────────────────────────────────────────────────────────────┘
            ↑                    ↑                    ↑
            │                    │                    │
    ┌───────┴────────┐   ┌──────┴──────┐   ┌────────┴────────┐
    │     Social     │   │  ChatRoom   │   │   Messaging     │
    │   (Friends)    │   │             │   │   (Messages)    │
    └────────────────┘   └─────────────┘   └─────────────────┘
            ↓                    ↓                    ↓
            │                    │                    │
            └──────────┬─────────┴────────────────────┘
                       ↓
            ┌─────────────────────┐
            │    Notification     │
            └─────────────────────┘
```

### 통합 패턴:

| From Context | To Context | 통합 방식 | 패턴 | 문제점 |
|--------------|-----------|----------|------|--------|
| Social | Identity | UserId 참조 | Shared Kernel | ✅ 적절 |
| ChatRoom | Identity | UserId 참조 | Shared Kernel | ✅ 적절 |
| Messaging | Identity | UserId 참조 | Shared Kernel | ✅ 적절 |
| Messaging | ChatRoom | roomId 참조 + Saga | **Conformist** | ⚠️ 강한 결합 |
| ChatRoom | Messaging | lastMessageId 참조 | **Shared Kernel** | ⚠️ 양방향 의존 |
| Social | ChatRoom | FriendAddedEvent | **Publisher-Subscriber** | ✅ 이벤트 기반 |
| Social | Notification | FriendRequestSentEvent | **Publisher-Subscriber** | ✅ 이벤트 기반 |
| Messaging | Notification | MessageSentEvent | **Publisher-Subscriber** | ✅ 이벤트 기반 |
| ChatRoom | Notification | ChatRoomCreatedEvent | **Publisher-Subscriber** | ✅ 이벤트 기반 |

---

## ⚠️ 발견된 문제점

### 1. **Message ↔ ChatRoom 강한 결합**

**문제:**
```kotlin
// Message가 ChatRoom을 직접 업데이트 (Saga Step 2)
class UpdateChatRoomMetadataStep {
    fun execute(context: MessageSagaContext): Boolean {
        val chatRoom = chatRoomQueryPort.findById(message.roomId)  // 직접 조회
        chatRoom.update(lastMessageId = message.id, lastActiveAt = now())  // 직접 수정
        chatRoomCommandPort.save(chatRoom)  // 직접 저장
    }
}
```

**영향:**
- Message 저장 시 ChatRoom에 강한 의존
- 트랜잭션 경계가 복잡해짐 (MongoDB + PostgreSQL Saga)
- 테스트 어려움

**해결책:**
```kotlin
// 이벤트 기반으로 분리
class SendMessageService {
    fun sendMessage(command: SendMessageCommand): ChatMessage {
        val savedMessage = messageCommandPort.save(message)  // MongoDB만 저장
        publishEvent(MessageSentEvent(message))  // 이벤트 발행
        return savedMessage
    }
}

// ChatRoom Context가 이벤트 구독
class MessageSentEventListener {
    @TransactionalEventListener
    fun handleMessageSent(event: MessageSentEvent) {
        val chatRoom = chatRoomQueryPort.findById(event.roomId)
        chatRoom.update(lastMessageId = event.messageId, lastActiveAt = now())
        chatRoomCommandPort.save(chatRoom)
    }
}
```

---

### 2. **User Context가 너무 많은 곳에서 참조됨**

**문제:**
- User가 Identity Context의 Aggregate Root이지만
- 다른 모든 Context가 UserId를 직접 참조

**현재 참조 위치:**
- Social: FriendRequest.senderId, Friendship.userId
- ChatRoom: ChatRoom.participants
- Messaging: ChatMessage.senderId, mentions, readBy
- Notification: Notification.userId

**영향:**
- User 삭제 시 모든 Context에 영향
- CASCADE DELETE 의존성 복잡

**해결책:**
- ✅ **현재 구현은 적절함** (UserId는 Shared Kernel의 Value Object)
- ✅ UserDeletedEvent로 다른 Context가 정리 수행
- ⚠️ 다만 UserId가 단순 Long wrapper라는 점은 개선 가능

---

### 3. **Notification Context의 역할 모호**

**문제:**
```kotlin
data class Notification(
    val sourceId: String,  // 타입 안전성 없음
    val sourceType: SourceType,  // USER, MESSAGE, CHAT_ROOM, FRIEND, SYSTEM
    val type: NotificationType  // FRIEND_REQUEST, MESSAGE, MENTION, SYSTEM
)
```

**영향:**
- sourceId가 String (MessageId는 ObjectId, UserId는 Long)
- 모든 알림을 하나의 모델로 처리 (Generic 모델)
- 알림별 특화된 로직 어려움

**해결책:**
```kotlin
// Option 1: 알림 타입별 Aggregate 분리
sealed class Notification {
    data class FriendNotification(
        val friendRequestId: FriendRequestId,
        val senderId: UserId
    ) : Notification()

    data class MessageNotification(
        val messageId: MessageId,
        val roomId: ChatRoomId
    ) : Notification()
}

// Option 2: Read Model로 취급 (현재 방식 유지)
// - 이벤트 기반으로 생성
// - sourceId를 JSON metadata로 저장
// - 타입별 조회 쿼리 제공
```

---

### 4. **Saga 패턴의 복잡성**

**문제:**
- Message 전송이 3단계 Saga (MongoDB → PostgreSQL → PostgreSQL)
- 보상 로직 복잡 (OptimisticLock 처리)
- 트랜잭션 경계 이해 어려움

**해결책:**
```
현재: Saga (3단계, 보상 로직)
제안: 이벤트 기반 (결과적 일관성)

1. SendMessageService: MongoDB만 저장 → MessageSentEvent 발행
2. ChatRoomMetadataUpdater: MessageSentEvent 구독 → PostgreSQL 업데이트
3. OutboxPublisher: MessageSentEvent 구독 → Outbox 저장

장점:
- 각 단계가 독립적
- 보상 로직 불필요 (재시도로 해결)
- 트랜잭션 경계 명확
```

---

## ✅ 잘 구현된 부분

### 1. **이벤트 기반 통합**

✅ Social → Notification 통합:
```kotlin
// Friend Context
publishEvent(FriendRequestSentEvent(senderId, receiverId))

// Notification Context (이벤트 구독)
@TransactionalEventListener
fun handleFriendRequestSent(event: FriendRequestSentEvent) {
    notificationCommandPort.save(
        Notification.create(
            userId = event.receiverId,
            type = NotificationType.FRIEND_REQUEST
        )
    )
}
```

### 2. **Redis 분산 락**

✅ 동시성 제어:
```kotlin
// Friend 요청 중복 방지
val lockKey = "friend-request:${sortedIds[0]}:${sortedIds[1]}"
redisLockManager.withLock(lockKey) {
    // 친구 요청 처리
}

// ChatRoom 중복 생성 방지
val lockKey = "chatroom:direct:${sortedIds[0]}:${sortedIds[1]}"
redisLockManager.withLock(lockKey) {
    // 채팅방 생성
}
```

### 3. **OptimisticLock + Retry**

✅ 동시 수정 처리:
```kotlin
@Retryable(
    retryFor = [OptimisticLockException::class],
    maxAttempts = 3,
    backoff = Backoff(delay = 100, multiplier = 2.0)
)
@Transactional
fun addParticipant(command: AddParticipantCommand) {
    // JPA @Version 필드로 충돌 감지
    // 자동 재시도로 대부분 해결
}
```

### 4. **도메인 이벤트 설계**

✅ 명확한 이벤트 명명:
- `FriendRequestSentEvent` (친구 요청 전송됨)
- `FriendAddedEvent` (친구 관계 생성됨)
- `MessageSentEvent` (메시지 전송됨)
- `ChatRoomCreatedEvent` (채팅방 생성됨)

---

## 🎯 권장 Bounded Context 재정의

### 제안하는 Context 구조:

```
1. Identity Context (사용자 인증 및 계정)
   - User, RefreshToken
   - 인증, 프로필 관리

2. Social Context (친구 관계)
   - FriendRequest, Friendship, BlockedUser, FriendGroup
   - 친구 요청, 수락, 차단

3. Messaging Context (메시지)
   - ChatMessage, MessageBookmark
   - 메시지 전송, 수정, 삭제, 리액션
   - (Saga 제거, 이벤트 기반으로 전환)

4. Conversation Context (대화방) ← 이름 변경
   - ChatRoom, ChatRoomSettings
   - 채팅방 생성, 참여자 관리
   - (Message 의존성 제거)

5. Notification Context (알림)
   - Notification, NotificationSettings
   - 시스템 알림 (Read Model로 취급)

6. Shared Kernel
   - DomainEvent, Value Objects
   - (Saga 제거)
```

---

## 📝 개선 로드맵

### Phase 1: Message ↔ ChatRoom 결합 제거 (우선순위: 높음)

**Before:**
```
SendMessageService
  └─> MessageSagaOrchestrator
      ├─> SaveMessageToMongoStep (MongoDB)
      ├─> UpdateChatRoomMetadataStep (PostgreSQL)  ← 강한 결합
      └─> PublishEventToOutboxStep (PostgreSQL)
```

**After:**
```
SendMessageService
  └─> MongoDB 저장
  └─> MessageSentEvent 발행

ChatRoomMetadataUpdateListener
  └─> MessageSentEvent 구독
  └─> PostgreSQL 업데이트 (독립적)

OutboxPublisher
  └─> MessageSentEvent 구독
  └─> Outbox 저장 (독립적)
```

**작업:**
1. Saga 패턴 제거
2. 이벤트 기반으로 전환
3. 보상 로직 대신 재시도 메커니즘

---

### Phase 2: Notification Context 개선 (우선순위: 중간)

**Option A: 알림 타입별 Aggregate 분리**
```kotlin
sealed class Notification

data class FriendNotification(
    val id: NotificationId,
    val userId: UserId,
    val friendRequestId: FriendRequestId,
    val type: FriendNotificationType  // REQUEST_SENT, ACCEPTED, REJECTED
) : Notification()

data class MessageNotification(
    val id: NotificationId,
    val userId: UserId,
    val messageId: MessageId,
    val roomId: ChatRoomId,
    val type: MessageNotificationType  // NEW_MESSAGE, MENTION, REPLY
) : Notification()
```

**Option B: Read Model 유지 + 타입 안전성 개선**
```kotlin
data class Notification(
    val id: NotificationId,
    val userId: UserId,
    val type: NotificationType,
    val source: NotificationSource  // sealed class로 타입 안전성 보장
)

sealed class NotificationSource {
    data class FriendSource(val friendRequestId: FriendRequestId) : NotificationSource()
    data class MessageSource(val messageId: MessageId, val roomId: ChatRoomId) : NotificationSource()
}
```

---

### Phase 3: Value Object 강화 (우선순위: 낮음)

**현재:**
```kotlin
@JvmInline
value class UserId(val value: Long)  // 단순 wrapper
```

**개선:**
```kotlin
@JvmInline
value class UserId(val value: Long) {
    init {
        require(value > 0) { "UserId must be positive" }
    }

    companion object {
        fun from(value: Long): UserId = UserId(value)
        fun fromString(value: String): UserId = UserId(value.toLong())
    }
}
```

---

## 📊 Context 성숙도 평가

| Context | Aggregate 명확성 | 경계 명확성 | 이벤트 기반 | 독립 배포 가능 | 점수 |
|---------|----------------|-----------|-----------|-------------|------|
| Identity | ✅ 5/5 | ✅ 5/5 | ✅ 5/5 | ⚠️ 3/5 | **18/20** |
| Social | ✅ 5/5 | ✅ 5/5 | ✅ 5/5 | ✅ 4/5 | **19/20** |
| Messaging | ✅ 5/5 | ⚠️ 2/5 | ⚠️ 3/5 | ❌ 1/5 | **11/20** |
| ChatRoom | ✅ 4/5 | ⚠️ 2/5 | ✅ 5/5 | ⚠️ 2/5 | **13/20** |
| Notification | ⚠️ 3/5 | ⚠️ 3/5 | ✅ 5/5 | ✅ 4/5 | **15/20** |

**평균:** **15.2/20** (76%)

---

## 🎯 결론

### 현재 상태:
- ✅ DDD 기본 원칙은 대체로 준수
- ✅ 이벤트 기반 통합 잘 구현
- ⚠️ Bounded Context 경계가 일부 모호
- ⚠️ Message ↔ ChatRoom 강한 결합

### 핵심 개선 사항:
1. **Saga 패턴 제거** → 이벤트 기반으로 전환
2. **Message ↔ ChatRoom 결합 제거**
3. **Notification Context 타입 안전성 개선**

### 긍정적 평가:
- Redis 분산 락으로 동시성 제어 우수
- OptimisticLock + Retry 패턴 적절
- 도메인 이벤트 명명 명확
- Hexagonal Architecture 잘 적용

---

**작성자:** Claude Code
**검토 날짜:** 2025-11-02
**다음 단계:** Phase 1 개선 작업 (Saga → 이벤트 기반 전환)
