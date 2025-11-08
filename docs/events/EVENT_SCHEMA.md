# Event Schema 정의

## 개요

Shoot 애플리케이션의 이벤트 기반 아키텍처를 위한 Event Schema 정의입니다.

**주요 특징**:
- **이벤트 스키마 버저닝**: Semantic Versioning (MAJOR.MINOR.PATCH)
- **MSA 준비**: 서비스 간 독립적인 이벤트 통신
- **Kafka 기반**: 이벤트 브로커로 Apache Kafka 사용
- **트랜잭션 보장**: `@TransactionalEventListener` 사용

## 이벤트 스키마 버전 관리

### Semantic Versioning 규칙

- **MAJOR (1.x.x)**: 호환되지 않는 변경 (Breaking Changes)
  - 필수 필드 제거
  - 필드 타입 변경
  - 필드명 변경

- **MINOR (x.1.x)**: 하위 호환 가능한 추가 (Backward-Compatible)
  - 새 선택적 필드 추가
  - 새 enum 값 추가

- **PATCH (x.x.1)**: 버그 수정
  - 문서 수정
  - 검증 로직 수정

### 현재 버전

모든 이벤트의 현재 버전은 `1.0.0`입니다.

## 공통 필드

모든 도메인 이벤트는 다음 공통 필드를 포함합니다:

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `version` | String | ✅ | 이벤트 스키마 버전 (예: "1.0.0") |
| `occurredOn` | Long | ✅ | 이벤트 발생 시각 (Unix timestamp 밀리초) |

## Kafka 토픽 전략

### 토픽 명명 규칙

```
{domain}.{entity}.{event-type}
```

**예시**:
- `chat.message.sent`
- `chat.room.created`
- `user.friend.added`

### 파티셔닝 전략

- **Message 이벤트**: `roomId` 기준 파티셔닝 → 메시지 순서 보장
- **ChatRoom 이벤트**: `roomId` 기준 파티셔닝
- **Friend 이벤트**: `userId` 기준 파티셔닝
- **User 이벤트**: `userId` 기준 파티셔닝
- **Notification 이벤트**: `userId` 기준 파티셔닝

## 이벤트 카탈로그

### 1. Message 이벤트

#### 1.1 MessageSentEvent

**토픽**: `chat.message.sent`

**스키마 버전**: `1.0.0`

**설명**: 새 메시지가 전송되었을 때 발행

**파티션 키**: `roomId`

**JSON Schema**:
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "MessageSentEvent",
  "type": "object",
  "required": ["version", "messageId", "roomId", "senderId", "content", "type", "createdAt", "occurredOn"],
  "properties": {
    "version": {
      "type": "string",
      "pattern": "^\\d+\\.\\d+\\.\\d+$",
      "description": "Event schema version",
      "example": "1.0.0"
    },
    "messageId": {
      "type": "string",
      "description": "메시지 ID (MongoDB ObjectId)",
      "example": "msg_1234567890"
    },
    "roomId": {
      "type": "integer",
      "format": "int64",
      "description": "채팅방 ID",
      "example": 101
    },
    "senderId": {
      "type": "integer",
      "format": "int64",
      "description": "발신자 ID",
      "example": 1
    },
    "content": {
      "type": "string",
      "maxLength": 4000,
      "description": "메시지 내용",
      "example": "안녕하세요!"
    },
    "type": {
      "type": "string",
      "enum": ["TEXT", "IMAGE", "VIDEO", "AUDIO", "FILE", "SYSTEM"],
      "description": "메시지 타입"
    },
    "mentions": {
      "type": "array",
      "items": {
        "type": "integer",
        "format": "int64"
      },
      "description": "멘션된 사용자 ID 목록",
      "default": []
    },
    "createdAt": {
      "type": "string",
      "format": "date-time",
      "description": "메시지 생성 시각"
    },
    "occurredOn": {
      "type": "integer",
      "format": "int64",
      "description": "이벤트 발생 시각 (Unix timestamp 밀리초)"
    }
  }
}
```

**Consumers**:
- Notification Service: 알림 생성
- Analytics Service: 메시지 통계

---

#### 1.2 MessageEditedEvent

**토픽**: `chat.message.edited`

**스키마 버전**: `1.0.0`

**설명**: 메시지가 수정되었을 때 발행

**파티션 키**: `roomId`

**JSON Schema**:
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "MessageEditedEvent",
  "type": "object",
  "required": ["version", "messageId", "roomId", "userId", "oldContent", "newContent", "editedAt", "occurredOn"],
  "properties": {
    "version": {
      "type": "string",
      "pattern": "^\\d+\\.\\d+\\.\\d+$",
      "example": "1.0.0"
    },
    "messageId": {
      "type": "string",
      "description": "메시지 ID"
    },
    "roomId": {
      "type": "integer",
      "format": "int64",
      "description": "채팅방 ID"
    },
    "userId": {
      "type": "integer",
      "format": "int64",
      "description": "수정한 사용자 ID"
    },
    "oldContent": {
      "type": "string",
      "description": "이전 메시지 내용"
    },
    "newContent": {
      "type": "string",
      "description": "새 메시지 내용"
    },
    "editedAt": {
      "type": "string",
      "format": "date-time",
      "description": "수정 시각"
    },
    "occurredOn": {
      "type": "integer",
      "format": "int64"
    }
  }
}
```

**Consumers**:
- Notification Service: 수정 알림
- Audit Service: 감사 로그

---

#### 1.3 MessageDeletedEvent

**토픽**: `chat.message.deleted`

**스키마 버전**: `1.0.0`

**설명**: 메시지가 삭제되었을 때 발행

**파티션 키**: `roomId`

**Consumers**:
- Audit Service: 삭제 감사 로그

---

#### 1.4 MessageReactionEvent

**토픽**: `chat.message.reaction`

**스키마 버전**: `1.0.0`

**설명**: 메시지에 리액션이 추가/제거되었을 때 발행

**파티션 키**: `roomId`

**JSON Schema**:
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "MessageReactionEvent",
  "type": "object",
  "required": ["version", "messageId", "roomId", "userId", "reactionType", "action", "occurredOn"],
  "properties": {
    "version": {
      "type": "string",
      "pattern": "^\\d+\\.\\d+\\.\\d+$",
      "example": "1.0.0"
    },
    "messageId": {
      "type": "string",
      "description": "메시지 ID"
    },
    "roomId": {
      "type": "integer",
      "format": "int64",
      "description": "채팅방 ID"
    },
    "userId": {
      "type": "integer",
      "format": "int64",
      "description": "리액션을 추가/제거한 사용자 ID"
    },
    "reactionType": {
      "type": "string",
      "enum": ["like", "sad", "dislike", "angry", "curious", "surprised"],
      "description": "리액션 타입"
    },
    "action": {
      "type": "string",
      "enum": ["ADDED", "REMOVED"],
      "description": "리액션 추가 또는 제거"
    },
    "occurredOn": {
      "type": "integer",
      "format": "int64"
    }
  }
}
```

**Consumers**:
- Notification Service: 리액션 알림 (메시지 작성자에게)

---

#### 1.5 MessagePinEvent

**토픽**: `chat.message.pin`

**스키마 버전**: `1.0.0`

**설명**: 메시지가 고정/고정 해제되었을 때 발행

**파티션 키**: `roomId`

**Consumers**:
- Notification Service: 고정 알림

---

#### 1.6 MentionEvent

**토픽**: `chat.message.mention`

**스키마 버전**: `1.0.0`

**설명**: 사용자가 메시지에서 멘션되었을 때 발행

**파티션 키**: `roomId`

**JSON Schema**:
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "MentionEvent",
  "type": "object",
  "required": ["version", "messageId", "roomId", "senderId", "mentionedUserIds", "occurredOn"],
  "properties": {
    "version": {
      "type": "string",
      "pattern": "^\\d+\\.\\d+\\.\\d+$",
      "example": "1.0.0"
    },
    "messageId": {
      "type": "string",
      "description": "메시지 ID"
    },
    "roomId": {
      "type": "integer",
      "format": "int64",
      "description": "채팅방 ID"
    },
    "senderId": {
      "type": "integer",
      "format": "int64",
      "description": "발신자 ID"
    },
    "mentionedUserIds": {
      "type": "array",
      "items": {
        "type": "integer",
        "format": "int64"
      },
      "description": "멘션된 사용자 ID 목록"
    },
    "occurredOn": {
      "type": "integer",
      "format": "int64"
    }
  }
}
```

**Consumers**:
- Notification Service: 멘션 알림 생성

---

### 2. ChatRoom 이벤트

#### 2.1 ChatRoomCreatedEvent

**토픽**: `chat.room.created`

**스키마 버전**: `1.0.0`

**설명**: 새 채팅방이 생성되었을 때 발행

**파티션 키**: `roomId`

**JSON Schema**:
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "ChatRoomCreatedEvent",
  "type": "object",
  "required": ["version", "roomId", "userId", "occurredOn"],
  "properties": {
    "version": {
      "type": "string",
      "pattern": "^\\d+\\.\\d+\\.\\d+$",
      "example": "1.0.0"
    },
    "roomId": {
      "type": "integer",
      "format": "int64",
      "description": "생성된 채팅방 ID"
    },
    "userId": {
      "type": "integer",
      "format": "int64",
      "description": "채팅방을 생성한 사용자 ID"
    },
    "occurredOn": {
      "type": "integer",
      "format": "int64"
    }
  }
}
```

**Consumers**:
- Analytics Service: 채팅방 생성 통계

---

#### 2.2 ChatRoomParticipantChangedEvent

**토픽**: `chat.room.participant.changed`

**스키마 버전**: `1.0.0`

**설명**: 채팅방 참여자가 추가/제거되었을 때 발행

**파티션 키**: `roomId`

**JSON Schema**:
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "ChatRoomParticipantChangedEvent",
  "type": "object",
  "required": ["version", "roomId", "participantsAdded", "participantsRemoved", "changedBy", "occurredOn"],
  "properties": {
    "version": {
      "type": "string",
      "pattern": "^\\d+\\.\\d+\\.\\d+$",
      "example": "1.0.0"
    },
    "roomId": {
      "type": "integer",
      "format": "int64",
      "description": "채팅방 ID"
    },
    "participantsAdded": {
      "type": "array",
      "items": {
        "type": "integer",
        "format": "int64"
      },
      "description": "추가된 참여자 ID 목록"
    },
    "participantsRemoved": {
      "type": "array",
      "items": {
        "type": "integer",
        "format": "int64"
      },
      "description": "제거된 참여자 ID 목록"
    },
    "changedBy": {
      "type": "integer",
      "format": "int64",
      "description": "변경을 수행한 사용자 ID"
    },
    "occurredOn": {
      "type": "integer",
      "format": "int64"
    }
  }
}
```

**Consumers**:
- Notification Service: 참여자 추가/제거 알림

---

#### 2.3 ChatRoomTitleChangedEvent

**토픽**: `chat.room.title.changed`

**스키마 버전**: `1.0.0`

**설명**: 채팅방 제목이 변경되었을 때 발행

**파티션 키**: `roomId`

**Consumers**:
- Notification Service: 제목 변경 알림

---

### 3. Friend 이벤트

#### 3.1 FriendAddedEvent

**토픽**: `user.friend.added`

**스키마 버전**: `1.0.0`

**설명**: 친구가 추가되었을 때 발행 (양방향)

**파티션 키**: `userId`

**JSON Schema**:
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "FriendAddedEvent",
  "type": "object",
  "required": ["version", "userId", "friendId", "occurredOn"],
  "properties": {
    "version": {
      "type": "string",
      "pattern": "^\\d+\\.\\d+\\.\\d+$",
      "example": "1.0.0"
    },
    "userId": {
      "type": "integer",
      "format": "int64",
      "description": "사용자 ID"
    },
    "friendId": {
      "type": "integer",
      "format": "int64",
      "description": "친구 ID"
    },
    "occurredOn": {
      "type": "integer",
      "format": "int64"
    }
  }
}
```

**Consumers**:
- Notification Service: 친구 수락 알림
- Analytics Service: 친구 관계 통계

---

#### 3.2 FriendRequestSentEvent

**토픽**: `user.friend.request.sent`

**스키마 버전**: `1.0.0`

**설명**: 친구 요청이 전송되었을 때 발행

**파티션 키**: `toUserId`

**Consumers**:
- Notification Service: 친구 요청 알림 생성

---

#### 3.3 FriendRemovedEvent

**토픽**: `user.friend.removed`

**스키마 버전**: `1.0.0`

**설명**: 친구가 삭제되었을 때 발행

**파티션 키**: `userId`

**Consumers**:
- Analytics Service: 친구 삭제 통계

---

### 4. User 이벤트

#### 4.1 UserCreatedEvent

**토픽**: `user.created`

**스키마 버전**: `1.0.0`

**설명**: 새 사용자가 생성되었을 때 발행

**파티션 키**: `userId`

**JSON Schema**:
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "UserCreatedEvent",
  "type": "object",
  "required": ["version", "userId", "username", "occurredOn"],
  "properties": {
    "version": {
      "type": "string",
      "pattern": "^\\d+\\.\\d+\\.\\d+$",
      "example": "1.0.0"
    },
    "userId": {
      "type": "integer",
      "format": "int64",
      "description": "생성된 사용자 ID"
    },
    "username": {
      "type": "string",
      "description": "사용자 이름"
    },
    "occurredOn": {
      "type": "integer",
      "format": "int64"
    }
  }
}
```

**Consumers**:
- Analytics Service: 사용자 가입 통계
- Email Service: 환영 이메일 전송

---

#### 4.2 UserDeletedEvent

**토픽**: `user.deleted`

**스키마 버전**: `1.0.0`

**설명**: 사용자가 삭제되었을 때 발행

**파티션 키**: `userId`

**Consumers**:
- Chat Service: 채팅방 퇴장 처리
- Friend Service: 친구 관계 정리

---

### 5. Notification 이벤트

#### 5.1 NotificationEvent

**토픽**: `notification.created`

**스키마 버전**: `1.0.0`

**설명**: 새 알림이 생성되었을 때 발행

**파티션 키**: `recipients[0]` (첫 번째 수신자 ID)

**JSON Schema**:
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "NotificationEvent",
  "type": "object",
  "required": ["version", "type", "title", "message", "sourceId", "sourceType", "recipients", "occurredOn"],
  "properties": {
    "version": {
      "type": "string",
      "pattern": "^\\d+\\.\\d+\\.\\d+$",
      "example": "1.0.0"
    },
    "id": {
      "type": "string",
      "description": "알림 ID (선택적)"
    },
    "timestamp": {
      "type": "string",
      "format": "date-time",
      "description": "알림 생성 시각"
    },
    "type": {
      "type": "string",
      "enum": ["NEW_MESSAGE", "MENTION", "REACTION", "PIN", "FRIEND_REQUEST", "FRIEND_ACCEPTED", "FRIEND_REJECTED", "FRIEND_REMOVED", "SYSTEM_ANNOUNCEMENT", "SYSTEM_MAINTENANCE", "OTHER"],
      "description": "알림 타입"
    },
    "title": {
      "type": "string",
      "description": "알림 제목"
    },
    "message": {
      "type": "string",
      "description": "알림 내용"
    },
    "sourceId": {
      "type": "string",
      "description": "소스 ID (채팅방 ID, 사용자 ID 등)"
    },
    "sourceType": {
      "type": "string",
      "enum": ["CHAT", "CHAT_ROOM", "USER", "FRIEND", "SYSTEM", "OTHER"],
      "description": "소스 타입"
    },
    "metadata": {
      "type": "object",
      "additionalProperties": true,
      "description": "추가 메타데이터",
      "default": {}
    },
    "recipients": {
      "type": "array",
      "items": {
        "type": "integer",
        "format": "int64"
      },
      "description": "수신자 ID 목록"
    },
    "occurredOn": {
      "type": "integer",
      "format": "int64"
    }
  }
}
```

**Consumers**:
- Push Notification Service: 푸시 알림 전송
- Email Service: 이메일 알림 전송
- WebSocket Service: 실시간 알림 전송

---

## Kafka 토픽 설정

### 토픽 구성 예시

```yaml
# Message 토픽
chat.message.sent:
  partitions: 10
  replication-factor: 3
  retention.ms: 604800000  # 7 days
  cleanup.policy: delete

chat.message.edited:
  partitions: 10
  replication-factor: 3
  retention.ms: 604800000
  cleanup.policy: delete

# ChatRoom 토픽
chat.room.created:
  partitions: 5
  replication-factor: 3
  retention.ms: 2592000000  # 30 days
  cleanup.policy: delete

chat.room.participant.changed:
  partitions: 10
  replication-factor: 3
  retention.ms: 2592000000
  cleanup.policy: delete

# Friend 토픽
user.friend.added:
  partitions: 5
  replication-factor: 3
  retention.ms: 2592000000
  cleanup.policy: delete

user.friend.request.sent:
  partitions: 5
  replication-factor: 3
  retention.ms: 2592000000
  cleanup.policy: delete

# User 토픽
user.created:
  partitions: 3
  replication-factor: 3
  retention.ms: -1  # Infinite (compacted)
  cleanup.policy: compact

# Notification 토픽
notification.created:
  partitions: 10
  replication-factor: 3
  retention.ms: 604800000  # 7 days
  cleanup.policy: delete
```

## Consumer Groups

### Notification Service
- **Group ID**: `notification-service`
- **Subscribed Topics**:
  - `chat.message.sent`
  - `chat.message.mention`
  - `chat.message.reaction`
  - `user.friend.request.sent`
  - `user.friend.added`

### Analytics Service
- **Group ID**: `analytics-service`
- **Subscribed Topics**:
  - `chat.message.sent`
  - `chat.room.created`
  - `user.created`
  - `user.friend.added`

### Audit Service
- **Group ID**: `audit-service`
- **Subscribed Topics**:
  - `chat.message.edited`
  - `chat.message.deleted`
  - `user.deleted`

## 이벤트 발행 가이드

### Spring Boot에서 이벤트 발행

```kotlin
@Service
class MessageService(
    private val eventPublisher: ApplicationEventPublisher
) {
    fun sendMessage(command: SendMessageCommand) {
        // 1. 메시지 저장
        val message = messageRepository.save(...)

        // 2. 이벤트 발행 (트랜잭션 커밋 후 자동 발행)
        eventPublisher.publishEvent(
            MessageSentEvent(
                messageId = message.id!!,
                roomId = message.roomId,
                senderId = message.senderId,
                content = message.content.text,
                type = message.content.type,
                mentions = message.mentions,
                createdAt = message.createdAt!!
            )
        )
    }
}
```

### 이벤트 리스너

```kotlin
@ApplicationEventListener
class MessageEventListener(
    private val kafkaTemplate: KafkaTemplate<String, String>
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleMessageSent(event: MessageSentEvent) {
        // Kafka로 이벤트 전송
        kafkaTemplate.send(
            "chat.message.sent",
            event.roomId.value.toString(),  // Partition key
            objectMapper.writeValueAsString(event)
        )
    }
}
```

## 버전 관리 전략

### 하위 호환성 유지

1. **선택적 필드 추가** (MINOR 버전 업)
   ```kotlin
   // v1.0.0
   data class MessageSentEvent(
       val messageId: MessageId,
       val content: String
   )

   // v1.1.0 - 선택적 필드 추가
   data class MessageSentEvent(
       val messageId: MessageId,
       val content: String,
       val attachments: List<Attachment> = emptyList()  // 새 필드
   )
   ```

2. **Breaking Change** (MAJOR 버전 업)
   ```kotlin
   // v1.0.0
   data class MessageSentEvent(
       val messageId: String  // String 타입
   )

   // v2.0.0 - 타입 변경 (Breaking)
   data class MessageSentEvent(
       val messageId: MessageId  // Value Object로 변경
   )
   ```

### 스키마 레지스트리

향후 MSA 전환 시 Confluent Schema Registry 또는 Avro 스키마 사용 권장.

---

**Last Updated**: 2025-01-08

**참고 문서**:
- `/docs/api/*.yaml`: REST API 명세
- `/CLAUDE.md`: 개발 가이드
- `/DOMAIN.md`: 도메인 모델 문서
