# ACL (Anti-Corruption Layer) 패턴 가이드

> **작성일**: 2025-11-08
> **작성자**: 학습 및 지식 정리
> **목적**: ACL 패턴을 왜 사용해야 하는지, Before/After 코드를 통해 실제 이점 이해

---

## 📚 목차

1. [ACL이란?](#acl이란)
2. [문제 상황: ACL을 사용하지 않았을 때](#문제-상황-acl을-사용하지-않았을-때)
3. [해결책: ACL 적용](#해결책-acl-적용)
4. [Before vs After 코드 비교](#before-vs-after-코드-비교)
5. [실제 얻은 이점](#실제-얻은-이점)
6. [트레이드오프](#트레이드오프)
7. [언제 ACL을 사용해야 하나?](#언제-acl을-사용해야-하나)

---

## ACL이란?

**Anti-Corruption Layer (부패 방지 계층)**

DDD(Domain-Driven Design)의 전략적 패턴 중 하나로, **서로 다른 Bounded Context 간의 경계에서 모델 변환을 담당하는 계층**입니다.

### 핵심 개념

```
┌─────────────────┐         ┌─────────────────┐
│  Chat Context   │         │ ChatRoom Context│
│                 │         │                 │
│  MessageId      │   ACL   │  MessageId      │
│  (Chat용)       │ <────>  │  (ChatRoom용)   │
│                 │         │                 │
└─────────────────┘         └─────────────────┘
```

**왜 "Anti-Corruption"인가?**
- 한 Context의 변경이 다른 Context를 "오염(Corruption)"시키지 않도록 방어
- 각 Context가 독립적으로 진화할 수 있도록 보호

---

## 문제 상황: ACL을 사용하지 않았을 때

### Before: String으로 대충 처리

```kotlin
// ChatRoom Context (ChatRoom.kt)
data class ChatRoom(
    val id: ChatRoomId? = null,
    var title: ChatRoomTitle? = null,
    val type: ChatRoomType,
    var participants: Set<UserId>,
    var lastMessageId: String? = null,  // ❌ 원시 타입 사용
    var lastActiveAt: Instant = Instant.now(),
    // ...
)
```

```kotlin
// Application Layer (MessageReadService.kt)
private fun getLastMessageText(chatRoom: ChatRoom): String {
    return chatRoom.lastMessageId?.let { lastMessageIdStr ->
        try {
            // ❌ String을 직접 MessageId로 변환
            val lastMessageId = MessageId.from(lastMessageIdStr)
            val lastMessage = messageQueryPort.findById(lastMessageId)
            formatMessageContent(lastMessage)
        } catch (e: Exception) {
            logger.warn(e) { "마지막 메시지 조회 실패: $lastMessageIdStr" }
            "최근 메시지"
        }
    } ?: "메시지가 없습니다"
}
```

### 문제점

#### 1. 타입 안전성 부족
```kotlin
// ❌ String이라서 뭐든 들어갈 수 있음
chatRoom.lastMessageId = "invalid-id"
chatRoom.lastMessageId = "123"
chatRoom.lastMessageId = "msg_12345"  // 어느 Context의 MessageId인지 불명확
```

#### 2. Context 경계 모호
```kotlin
// ❌ Chat Context의 MessageId를 직접 사용
// ChatRoom Context가 Chat Context에 의존하게 됨
val messageId = MessageId.from(chatRoom.lastMessageId)  // 이건 Chat Context의 MessageId!
```

**의존성 그래프:**
```
ChatRoom Context ──(직접 의존)──> Chat Context
                                    ↑
                              (나쁜 결합!)
```

#### 3. 변경의 파급 효과
```kotlin
// Chat Context에서 MessageId 구조 변경 시:
// Before: "msg_12345"
// After:  "chat_msg_12345_v2"

// ❌ ChatRoom Context도 함께 수정해야 함!
// ❌ 모든 String 처리 로직 수정 필요
```

#### 4. 테스트 어려움
```kotlin
// ❌ String이라서 mock 객체 만들기 어려움
val chatRoom = ChatRoom(
    lastMessageId = "msg_123"  // 이게 유효한지 검증 불가
)
```

---

## 해결책: ACL 적용

### After: MessageId VO + ACL Converter

#### 1단계: 각 Context에 독립적인 VO 생성

```kotlin
// ChatRoom Context (domain/chatroom/vo/MessageId.kt)
@JvmInline
value class MessageId private constructor(val value: String) {
    companion object {
        fun from(value: String): MessageId {
            require(value.isNotBlank()) { "메시지 ID는 비어있을 수 없습니다." }
            return MessageId(value)
        }
    }

    override fun toString(): String = value
}
```

```kotlin
// Chat Context (domain/chat/message/vo/MessageId.kt)
@JvmInline
value class MessageId private constructor(val value: String) {
    companion object {
        fun from(value: String): MessageId {
            require(value.isNotBlank()) { "메시지 ID는 비어있을 수 없습니다." }
            return MessageId(value)
        }
    }

    override fun toString(): String = value
}
```

**💡 포인트**: 구조적으로 동일하지만 **타입이 다름**

#### 2단계: ACL Converter 생성

```kotlin
// Application Layer (application/acl/MessageIdConverter.kt)
object MessageIdConverter {

    /**
     * Chat Context의 MessageId를 ChatRoom Context의 MessageId로 변환
     */
    fun toMessageId(
        chatMessageId: com.stark.shoot.domain.chat.message.vo.MessageId
    ): com.stark.shoot.domain.chatroom.vo.MessageId {
        return com.stark.shoot.domain.chatroom.vo.MessageId.from(chatMessageId.value)
    }

    /**
     * ChatRoom Context의 MessageId를 Chat Context의 MessageId로 변환
     */
    fun toChatMessageId(
        messageId: com.stark.shoot.domain.chatroom.vo.MessageId
    ): com.stark.shoot.domain.chat.message.vo.MessageId {
        return com.stark.shoot.domain.chat.message.vo.MessageId.from(messageId.value)
    }
}

// Extension functions (편의성)
fun com.stark.shoot.domain.chat.message.vo.MessageId.toMessageId() =
    MessageIdConverter.toMessageId(this)

fun com.stark.shoot.domain.chatroom.vo.MessageId.toChatMessageId() =
    MessageIdConverter.toChatMessageId(this)
```

#### 3단계: ChatRoom에 VO 적용

```kotlin
// ChatRoom Context (ChatRoom.kt)
data class ChatRoom(
    val id: ChatRoomId? = null,
    var title: ChatRoomTitle? = null,
    val type: ChatRoomType,
    var participants: Set<UserId>,
    var lastMessageId: MessageId? = null,  // ✅ ChatRoom Context의 MessageId VO
    var lastActiveAt: Instant = Instant.now(),
    // ...
)
```

#### 4단계: Application Layer에서 ACL 사용

```kotlin
// Application Layer (MessageReadService.kt)
private fun getLastMessageText(chatRoom: ChatRoom): String {
    return chatRoom.lastMessageId?.let { chatRoomMessageId ->
        try {
            // ✅ ACL을 통해 명시적으로 변환
            val chatMessageId = MessageIdConverter.toChatMessageId(chatRoomMessageId)

            // ✅ Chat Context의 MessageId 사용
            val lastMessage = messageQueryPort.findById(chatMessageId)
            formatMessageContent(lastMessage)
        } catch (e: Exception) {
            logger.warn(e) { "마지막 메시지 조회 실패: ${chatRoomMessageId.value}" }
            "최근 메시지"
        }
    } ?: "메시지가 없습니다"
}
```

**의존성 그래프 (개선됨):**
```
ChatRoom Context ──(독립)

Application Layer ──(ACL 사용)──> Chat Context
        ↑                              ↑
   (명시적 변환)                  (결합도 낮음)
```

---

## Before vs After 코드 비교

### 시나리오: 채팅방 메타데이터 업데이트

#### Before (String 사용)

```kotlin
// Domain Service
class ChatRoomMetadataDomainService {
    fun updateChatRoomWithNewMessage(
        chatRoom: ChatRoom,
        messageId: String,  // ❌ 어느 Context의 ID인지 불명확
        createdAt: Instant = Instant.now()
    ): ChatRoom {
        chatRoom.update(
            lastMessageId = messageId,  // ❌ String 직접 할당
            lastActiveAt = createdAt
        )
        return chatRoom
    }
}
```

```kotlin
// Application Service (UpdateChatRoomMetadataStep.kt)
val updatedRoom = chatRoomMetadataDomainService.updateChatRoomWithNewMessage(
    chatRoom = chatRoom,
    messageId = messageIdStr,  // ❌ String 전달
    createdAt = java.time.Instant.now()
)
```

**문제:**
- `messageId: String`이 Chat Context의 ID인지 ChatRoom Context의 ID인지 알 수 없음
- 타입 안전성 제로
- Context 경계 불명확

#### After (ACL 사용)

```kotlin
// Domain Service
import com.stark.shoot.domain.chatroom.vo.MessageId

class ChatRoomMetadataDomainService {
    fun updateChatRoomWithNewMessage(
        chatRoom: ChatRoom,
        messageId: MessageId,  // ✅ ChatRoom Context의 MessageId
        createdAt: Instant = Instant.now()
    ): ChatRoom {
        chatRoom.update(
            lastMessageId = messageId,  // ✅ 타입 안전
            lastActiveAt = createdAt
        )
        return chatRoom
    }
}
```

```kotlin
// Application Service (UpdateChatRoomMetadataStep.kt)
// ✅ 명시적으로 ChatRoom Context의 MessageId로 변환
val chatRoomMessageId = com.stark.shoot.domain.chatroom.vo.MessageId.from(messageIdStr)

val updatedRoom = chatRoomMetadataDomainService.updateChatRoomWithNewMessage(
    chatRoom = chatRoom,
    messageId = chatRoomMessageId,  // ✅ ChatRoom Context의 MessageId
    createdAt = java.time.Instant.now()
)
```

**개선:**
- `MessageId` 타입만 봐도 ChatRoom Context의 ID임을 알 수 있음
- 컴파일 타임에 타입 체크
- Context 경계 명확

---

## 실제 얻은 이점

### 1. 타입 안전성 (Compile-Time Safety)

#### Before
```kotlin
// ❌ 컴파일러가 못 잡음
chatRoom.lastMessageId = "invalid"
chatRoom.lastMessageId = null
chatRoom.lastMessageId = "123"
```

#### After
```kotlin
// ✅ 컴파일 에러!
chatRoom.lastMessageId = "invalid"  // Type mismatch: String vs MessageId

// ✅ 타입 안전
chatRoom.lastMessageId = MessageId.from("msg_123")
chatRoom.lastMessageId = null  // OK (nullable)
```

### 2. Context 독립성 (Bounded Context Isolation)

#### Before: 강한 결합
```kotlin
// ChatRoom Context가 Chat Context에 직접 의존
import com.stark.shoot.domain.chat.message.vo.MessageId  // ❌ 직접 import

class ChatRoom {
    var lastMessageId: String? = null  // ❌ String이라 애매
}
```

**문제:**
- Chat Context에서 MessageId 변경 시 ChatRoom도 영향받음
- MSA 전환 시 서비스 분리 어려움

#### After: ACL을 통한 느슨한 결합
```kotlin
// ChatRoom Context (독립적)
import com.stark.shoot.domain.chatroom.vo.MessageId  // ✅ 자신의 VO

class ChatRoom {
    var lastMessageId: MessageId? = null  // ✅ ChatRoom Context의 MessageId
}

// Application Layer (변환 담당)
import com.stark.shoot.application.acl.MessageIdConverter  // ✅ ACL 사용

val chatMessageId = MessageIdConverter.toChatMessageId(chatRoomMessageId)
```

**개선:**
- 각 Context가 독립적으로 진화 가능
- MSA 전환 시 ACL만 API Gateway로 변경하면 됨

### 3. 명시적인 의도 (Explicit Intent)

#### Before
```kotlin
// ❌ 이게 뭘 하는 코드인지 불명확
val messageId = MessageId.from(chatRoom.lastMessageId)
```

**질문:**
- `MessageId.from()`이 Chat Context의 MessageId를 만드는 건가?
- ChatRoom의 lastMessageId가 Chat Context의 ID였나?

#### After
```kotlin
// ✅ 명확한 의도: ChatRoom → Chat 변환
val chatMessageId = chatRoom.lastMessageId?.toChatMessageId()

// ✅ 또는
val chatMessageId = MessageIdConverter.toChatMessageId(chatRoomMessageId)
```

**코드 리뷰어가 바로 이해:**
- "아, ChatRoom Context의 MessageId를 Chat Context용으로 변환하는구나"
- Context 경계를 넘는 시점이 명확

### 4. 테스트 용이성

#### Before
```kotlin
// ❌ String이라 mock 어려움
@Test
fun test() {
    val chatRoom = ChatRoom(
        lastMessageId = "msg_123"  // 이게 유효한지?
    )
}
```

#### After
```kotlin
// ✅ 명확한 타입으로 테스트
@Test
fun test() {
    val messageId = com.stark.shoot.domain.chatroom.vo.MessageId.from("msg_123")
    val chatRoom = ChatRoom(
        lastMessageId = messageId  // ✅ 타입 체크됨
    )
}

// ✅ ACL Converter 테스트
@Test
fun `convert chat message id to chatroom message id`() {
    val chatMessageId = com.stark.shoot.domain.chat.message.vo.MessageId.from("msg_12345")

    val chatRoomMessageId = MessageIdConverter.toMessageId(chatMessageId)

    assertThat(chatRoomMessageId.value).isEqualTo("msg_12345")
    assertThat(chatRoomMessageId).isInstanceOf(
        com.stark.shoot.domain.chatroom.vo.MessageId::class.java
    )
}
```

### 5. MSA 전환 준비

#### Before: 모놀리식에서 분리 어려움
```kotlin
// ❌ String으로 모든 Context가 엮여있음
// 서비스 분리 시 대공사 필요
```

#### After: 서비스 분리 용이
```
현재 (Monolith):
┌─────────────────────────────────────┐
│  Application Layer                  │
│  ┌────────────────────────────────┐ │
│  │  MessageIdConverter (ACL)      │ │
│  │  - toMessageId()               │ │
│  │  - toChatMessageId()           │ │
│  └────────────────────────────────┘ │
│         ↓              ↓             │
│  ChatRoom Context  Chat Context     │
└─────────────────────────────────────┘

MSA 전환 후:
┌──────────────────┐         ┌──────────────────┐
│  ChatRoom Service│         │  Chat Service    │
│                  │         │                  │
│  MessageId (VO)  │         │  MessageId (VO)  │
└──────────────────┘         └──────────────────┘
         ↑                            ↑
         │                            │
    ┌────┴────────────────────────────┴────┐
    │  API Gateway (ACL)                   │
    │  - ChatRoomMessageIdDTO              │
    │  - ChatMessageIdDTO                  │
    │  - DTO 변환 로직                      │
    └──────────────────────────────────────┘
```

**ACL이 API Gateway의 DTO 변환 로직으로 자연스럽게 전환됨!**

---

## 트레이드오프

### ACL의 단점

#### 1. 코드량 증가
```kotlin
// Before: 1줄
chatRoom.lastMessageId = "msg_123"

// After: 2줄
val messageId = MessageId.from("msg_123")
chatRoom.lastMessageId = messageId
```

**평가**: 타입 안전성을 얻기 위한 합리적인 비용

#### 2. 변환 오버헤드
```kotlin
// 매번 변환 필요
val chatMessageId = MessageIdConverter.toChatMessageId(chatRoomMessageId)
```

**평가**:
- @JvmInline value class 사용으로 런타임 오버헤드 거의 없음
- 컴파일 시 primitive 타입으로 최적화

#### 3. 러닝 커브
- 팀원들이 ACL 패턴을 이해해야 함
- "왜 이렇게 복잡하게 하나요?" 질문 받을 수 있음

**평가**: 문서화 + 코드 리뷰로 해결 가능

### 언제 ACL을 스킵해도 될까?

#### 1. Shared Kernel
```kotlin
// UserId는 모든 Context에서 공유
// ✅ ACL 불필요
val userId: UserId = user.id  // 어디서나 동일한 UserId 사용
```

#### 2. 매우 단순한 프로젝트
- Bounded Context가 1~2개
- MSA 전환 계획 없음
- 타입 안전성이 중요하지 않음

**하지만**: 프로젝트는 성장한다. 나중에 리팩토링하는 게 더 힘듦.

---

## 언제 ACL을 사용해야 하나?

### ✅ ACL 사용 권장

1. **Bounded Context가 2개 이상**
   - 각 Context가 독립적으로 진화해야 함

2. **동일한 개념이지만 다른 의미를 가질 때**
   ```kotlin
   // Order Context의 Price: 주문 시점 가격 (불변)
   // Product Context의 Price: 현재 판매 가격 (변경 가능)
   ```

3. **MSA 전환 가능성이 있을 때**
   - ACL이 나중에 API Gateway의 DTO 변환 로직이 됨

4. **타입 안전성이 중요할 때**
   - 금융, 의료 등 도메인

### ❌ ACL 불필요

1. **Shared Kernel (공유 커널)**
   ```kotlin
   // UserId, Money 등 모든 Context가 공유하는 VO
   ```

2. **단순 CRUD 앱**
   - Bounded Context 분리 자체가 과도한 설계

3. **성능이 극도로 중요한 경우**
   - 다만 @JvmInline으로 오버헤드 거의 없음

---

## 핵심 정리

### ACL을 사용하는 이유 3줄 요약

1. **타입 안전성**: String → Value Object로 컴파일 타임 검증
2. **Context 독립성**: 각 Context가 독립적으로 진화 가능
3. **MSA 준비**: ACL → API Gateway 자연스러운 전환

### Before (String 사용)
```kotlin
var lastMessageId: String? = null  // ❌ 타입 불안정, Context 경계 모호
```

### After (ACL 사용)
```kotlin
var lastMessageId: MessageId? = null  // ✅ 타입 안전, ChatRoom Context의 MessageId

// Application Layer에서 변환
val chatMessageId = chatRoomMessageId.toChatMessageId()  // ✅ 명시적 변환
```

---

## 참고 자료

- [DDD Reference - Context Mapping](https://www.domainlanguage.com/ddd/reference/)
- [Martin Fowler - Bounded Context](https://martinfowler.com/bliki/BoundedContext.html)
- 프로젝트: `docs/architecture/CONTEXT_MAP.md`
- 프로젝트: `docs/architecture/BOUNDED_CONTEXTS.md`

---

**작성 동기**: TASK-008 ACL 확장 작업을 하면서 "왜 이렇게 해야 하는지" 명확히 이해하고 싶어서
**학습 포인트**: 코드의 복잡도 증가는 타입 안전성과 유지보수성을 위한 합리적인 투자
