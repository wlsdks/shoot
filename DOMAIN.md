# Shoot - Domain Documentation

> Spring Boot Kotlin 실시간 채팅 애플리케이션 도메인 모델

## 도메인 개요

Shoot은 **Hexagonal Architecture**와 **Domain-Driven Design**을 기반으로 설계된 실시간 채팅 애플리케이션입니다. 도메인은 크게 4개의 컨텍스트로 구분됩니다:

1. **User Context** - 사용자, 친구 관계, 차단 관리
2. **ChatRoom Context** - 채팅방, 참여자 관리
3. **Message Context** - 메시지, 리액션, 예약 메시지
4. **Notification Context** - 알림 생성 및 관리

---

## 1. Core Entities (핵심 엔티티)

### 1.1 User (사용자)

**위치**: `domain/user/User.kt`

**책임**:
- 사용자 계정 정보 관리
- 프로필 정보 업데이트 (닉네임, 프로필 이미지, 배경 이미지, 자기소개)
- 사용자 코드 생성/변경 (친구 추가용)
- 계정 삭제 (소프트 삭제)

**주요 Value Objects**:
- `UserId` - 사용자 고유 ID
- `Username` - 사용자명 (3-20자, 필수)
- `Nickname` - 닉네임 (1-30자, 필수)
- `UserCode` - 친구 추가용 코드 (8자리 대문자+숫자, 예: "A1B2C3D4")
- `ProfileImageUrl` - 프로필 이미지 URL
- `BackgroundImageUrl` - 배경 이미지 URL
- `UserBio` - 자기소개 (최대 500자)

**주요 메서드**:
- `create()` - 사용자 생성 (Factory method, 비밀번호 검증 포함)
- `generateUserCode()` - 유저 코드 재생성
- `changeUserCode()` - 유저 코드를 특정 값으로 변경 (중복 검사 필요)
- `updateProfile()` - 프로필 정보 업데이트
- `markAsDeleted()` - 계정 소프트 삭제

**비즈니스 규칙**:
- 비밀번호는 최소 8자 이상
- 사용자명은 3-20자 사이
- 유저 코드는 중복 불가 (외부 validator로 검증)
- 삭제된 계정은 OFFLINE 상태로 변경

---

### 1.2 ChatRoom (채팅방)

**위치**: `domain/chatroom/ChatRoom.kt`

**책임**:
- 채팅방 생성 (1:1, 그룹)
- 참여자 관리 (추가/제거)
- 즐겨찾기(핀) 상태 관리
- 채팅방 공지사항 관리
- 마지막 메시지 및 활동 시간 추적

**주요 Value Objects**:
- `ChatRoomId` - 채팅방 고유 ID
- `ChatRoomTitle` - 채팅방 제목 (최대 100자)
- `ChatRoomAnnouncement` - 공지사항 (최대 500자)

**주요 메서드**:
- `createDirectChat()` - 1:1 채팅방 생성 (Factory method)
- `addParticipant()` / `removeParticipant()` - 참여자 단건 추가/제거
- `addParticipants()` / `removeParticipants()` - 참여자 다건 추가/제거
- `updateParticipants()` - 참여자 목록 업데이트 (추가/제거 자동 처리)
- `calculateParticipantChanges()` - 참여자 변경 사항 계산 (핀 상태 포함)
- `updateFavoriteStatus()` - 즐겨찾기 상태 업데이트
- `updateAnnouncement()` - 공지사항 업데이트
- `isEmpty()` / `shouldBeDeleted()` - 채팅방 삭제 가능 여부 확인
- `isDirectChatBetween()` - 특정 두 사용자 간 1:1 채팅인지 확인

**비즈니스 규칙**:
- 1:1 채팅방은 정확히 2명의 참여자만 가능
- 자기 자신과 채팅방 생성 불가 (`SelfChatNotAllowed` 예외)
- 최대 핀 채팅방 개수 제한 (사용자별, DomainConstants에서 관리)
- 참여자가 없는 채팅방은 삭제 가능

---

### 1.3 ChatMessage (채팅 메시지)

**위치**: `domain/chat/message/ChatMessage.kt`

**책임**:
- 메시지 생성 및 전송
- 메시지 수정/삭제
- 리액션 추가/제거 (토글 방식)
- 메시지 고정/해제
- 멘션 추출 및 관리
- URL 미리보기 처리
- 읽음 상태 추적

**주요 Value Objects**:
- `MessageId` - 메시지 고유 ID
- `MessageContent` - 메시지 내용 (텍스트, 타입, 편집/삭제 상태, 첨부파일)
- `ChatMessageMetadata` - 메타데이터 (tempId, URL 미리보기, 읽음 시간)
- `MessageReactions` - 리액션 맵 (사용자별 리액션)

**주요 메서드**:
- `create()` - 메시지 생성 (Factory method)
- `editMessage()` - 메시지 수정 (TEXT 타입만 가능)
- `markAsDeleted()` - 메시지 소프트 삭제
- `toggleReaction()` - 리액션 토글 (같은 리액션 선택 시 제거, 다른 리액션 선택 시 교체)
- `pinMessageInRoom()` - 채팅방에서 메시지 고정 (기존 고정 메시지 자동 해제)
- `updatePinStatus()` - 메시지 고정 상태 변경
- `markAsRead()` - 메시지 읽음 처리
- `updateMentions()` - 멘션 추출 및 업데이트
- `setUrlPreview()` / `markNeedsUrlPreview()` - URL 미리보기 처리
- `isExpired()` / `setExpiration()` - 메시지 만료 처리

**비즈니스 규칙**:
- TEXT 타입 메시지만 수정 가능
- 삭제된 메시지는 수정 불가
- 한 채팅방에는 최대 하나의 고정 메시지만 존재 가능
- 사용자는 메시지당 하나의 리액션만 가능 (다른 리액션 선택 시 자동 교체)
- 메시지 내용 최대 길이: 4000자 (DomainConstants)

---

### 1.4 Notification (알림)

**위치**: `domain/notification/Notification.kt`

**책임**:
- 알림 생성 (채팅, 친구 요청 등)
- 읽음 상태 관리
- 소프트 삭제 처리
- 사용자 소유권 검증

**주요 Value Objects**:
- `NotificationId` - 알림 고유 ID
- `NotificationTitle` - 알림 제목 (최대 100자)
- `NotificationMessage` - 알림 메시지 (최대 500자)

**주요 메서드**:
- `fromChatEvent()` - 채팅 이벤트로부터 알림 생성 (Factory method)
- `fromEvent()` - 일반 도메인 이벤트로부터 알림 생성 (Factory method)
- `create()` - 알림 직접 생성 (Factory method)
- `markAsRead()` - 알림 읽음 처리
- `markAsDeleted()` - 알림 소프트 삭제
- `belongsToUser()` - 알림이 특정 사용자에게 속하는지 확인
- `validateOwnership()` - 사용자 소유권 검증 (예외 발생)

**비즈니스 규칙**:
- 알림은 특정 사용자에게만 속함
- 읽음 처리는 한 번만 가능 (중복 처리 방지)
- 소프트 삭제 후에도 데이터는 유지됨

---

### 1.5 FriendRequest (친구 요청)

**위치**: `domain/user/FriendRequest.kt`

**책임**:
- 친구 요청 생성
- 요청 수락/거절/취소

**주요 메서드**:
- `create()` - 친구 요청 생성 (Factory method)
- `accept()` - 요청 수락 (상태: PENDING → ACCEPTED)
- `reject()` - 요청 거절 (상태: PENDING → REJECTED)
- `cancel()` - 요청 취소 (상태: PENDING → CANCELLED)

**비즈니스 규칙**:
- 이미 처리된 요청은 재처리 불가 (`IllegalStateException`)
- 상태 변경 시 응답 시간(`respondedAt`) 자동 기록

---

### 1.6 Friendship (친구 관계)

**위치**: `domain/user/Friendship.kt`

**책임**:
- 두 사용자 간의 친구 관계 표현

**주요 메서드**:
- `create()` - 친구 관계 생성 (Factory method)

**비즈니스 규칙**:
- 양방향 관계 (userId ↔ friendId)
- 최대 친구 수: 1000명 (DomainConstants)

---

### 1.7 BlockedUser (차단된 사용자)

**위치**: `domain/user/BlockedUser.kt`

**책임**:
- 사용자 차단 관계 표현

**주요 메서드**:
- `create()` - 차단 관계 생성 (Factory method)

**비즈니스 규칙**:
- 단방향 차단 (userId → blockedUserId)
- 차단된 사용자는 채팅 불가

---

### 1.8 ScheduledMessage (예약 메시지)

**위치**: `domain/chat/message/ScheduledMessage.kt`

**책임**:
- 예약 메시지 생성 및 관리
- 예약 시간에 메시지 자동 전송

**주요 속성**:
- `scheduledAt` - 예약 전송 시간
- `status` - 예약 메시지 상태 (PENDING, SENT, CANCELLED)

**비즈니스 규칙**:
- 예약 시간은 현재 시간보다 미래여야 함
- PENDING 상태에서만 취소 가능

---

## 2. Value Objects (값 객체)

Value Objects는 불변 객체로, 도메인의 검증 로직을 캡슐화합니다.

### 2.1 User 관련 Value Objects

| Value Object | 검증 규칙 | 예시 |
|-------------|---------|------|
| `Username` | 3-20자, 필수 | "johndoe123" |
| `Nickname` | 1-30자, 필수 | "홍길동" |
| `UserCode` | 4-12자, 영문 대문자+숫자 | "A1B2C3D4" |
| `UserBio` | 최대 500자 | "안녕하세요!" |
| `ProfileImageUrl` | URL 형식 | "https://..." |
| `BackgroundImageUrl` | URL 형식 | "https://..." |

### 2.2 ChatRoom 관련 Value Objects

| Value Object | 검증 규칙 | 예시 |
|-------------|---------|------|
| `ChatRoomTitle` | 최대 100자 | "프로젝트 회의방" |
| `ChatRoomAnnouncement` | 최대 500자 | "매일 오전 10시 미팅" |

### 2.3 Message 관련 Value Objects

| Value Object | 검증 규칙 | 예시 |
|-------------|---------|------|
| `MessageContent` | 최대 4000자 | "안녕하세요" |

### 2.4 Notification 관련 Value Objects

| Value Object | 검증 규칙 | 예시 |
|-------------|---------|------|
| `NotificationTitle` | 최대 100자 | "새 메시지" |
| `NotificationMessage` | 최대 500자 | "홍길동: 안녕하세요" |

---

## 3. Domain Events (도메인 이벤트)

도메인 이벤트는 도메인 내에서 발생한 중요한 사실을 나타냅니다. Event-Driven Architecture의 핵심 요소입니다.

### 3.1 메시지 관련 이벤트

| 이벤트 | 발생 시점 | 수신 대상 |
|-------|---------|----------|
| `MessageSentEvent` | 메시지 전송 시 | WebSocket, Kafka, Notification |
| `MessageReactionEvent` | 리액션 추가/제거 시 | WebSocket, Notification |
| `MessagePinEvent` | 메시지 고정/해제 시 | WebSocket |
| `MessageBulkReadEvent` | 메시지 일괄 읽음 처리 시 | WebSocket |
| `MentionEvent` | 멘션 발생 시 | Notification |

### 3.2 채팅방 관련 이벤트

| 이벤트 | 발생 시점 | 수신 대상 |
|-------|---------|----------|
| `ChatRoomCreatedEvent` | 채팅방 생성 시 | WebSocket, Notification |
| `ChatRoomParticipantChangedEvent` | 참여자 변경 시 | WebSocket, Notification |
| `ChatRoomTitleChangedEvent` | 채팅방 제목 변경 시 | WebSocket |

### 3.3 친구 관련 이벤트

| 이벤트 | 발생 시점 | 수신 대상 |
|-------|---------|----------|
| `FriendAddedEvent` | 친구 추가 시 | Notification |
| `FriendRemovedEvent` | 친구 삭제 시 | Notification |

### 3.4 알림 이벤트

| 이벤트 | 발생 시점 | 수신 대상 |
|-------|---------|----------|
| `NotificationEvent` | 알림 생성 시 | WebSocket, FCM (Push Notification) |

**이벤트 처리 방식**:
- `@TransactionalEventListener` 사용 (트랜잭션 커밋 후 이벤트 발행)
- SpringEventPublisher로 이벤트 발행
- 비동기 처리 가능 (중요하지 않은 작업)

---

## 4. Domain Services (도메인 서비스)

복잡한 비즈니스 로직이나 여러 엔티티에 걸친 로직은 도메인 서비스로 분리합니다.

### 4.1 메시지 도메인 서비스

**위치**: `domain/chat/message/service/`

| 서비스 | 책임 |
|-------|-----|
| `MessageDomainService` | 메시지 생성, 삭제, 유효성 검증 |
| `MessageEditDomainService` | 메시지 수정 권한 검증, 수정 시간 제한 확인 |
| `MessageForwardDomainService` | 메시지 전달 (다른 채팅방으로) |
| `MessagePinDomainService` | 메시지 고정/해제, 최대 고정 개수 확인 |
| `MessageReactionService` | 리액션 토글, 리액션 유효성 검증 |

### 4.2 채팅방 도메인 서비스

**위치**: `domain/chatroom/service/`

| 서비스 | 책임 |
|-------|-----|
| `ChatRoomDomainService` | 채팅방 생성, 삭제, 중복 확인 |
| `ChatRoomParticipantDomainService` | 참여자 추가/제거, 권한 검증 |
| `ChatRoomValidationDomainService` | 채팅방 유효성 검증 (최대 참여자 수 등) |
| `ChatRoomMetadataDomainService` | 채팅방 메타데이터 관리 (마지막 메시지 시간 등) |
| `ChatRoomEventService` | 채팅방 관련 이벤트 발행 |

### 4.3 친구 도메인 서비스

**위치**: `domain/user/service/`

| 서비스 | 책임 |
|-------|-----|
| `FriendDomainService` | 친구 추가/삭제, 친구 요청 처리 |
| `UserBlockDomainService` | 사용자 차단/해제 |
| `FriendGroupDomainService` | 친구 그룹 관리 |

### 4.4 알림 도메인 서비스

**위치**: `domain/notification/service/`

| 서비스 | 책임 |
|-------|-----|
| `NotificationDomainService` | 알림 생성, 읽음 처리, 삭제 |

---

## 5. Domain Constants (도메인 상수)

**위치**: `infrastructure/config/domain/DomainConstants.kt`

비즈니스 규칙에 사용되는 상수값들을 중앙 관리합니다.

```kotlin
chatRoom:
  maxParticipants: 100        # 채팅방 최대 참여자 수
  minGroupParticipants: 2     # 그룹 채팅 최소 참여자 수
  maxPinnedMessages: 5        # 채팅방 최대 고정 메시지 수

message:
  maxContentLength: 4000      # 메시지 최대 길이
  maxAttachmentSize: 52428800 # 첨부파일 최대 크기 (50MB)
  batchSize: 100              # 메시지 배치 처리 크기

friend:
  maxFriendCount: 1000        # 최대 친구 수
  recommendationLimit: 20     # 친구 추천 최대 개수
```

---

## 6. Aggregates & Boundaries (애그리게이트 경계)

### 6.1 User Aggregate

**Root**: `User`

**포함 엔티티**:
- `User` (Root)
- `Friendship` (별도 관리, User ID로 참조)
- `FriendRequest` (별도 관리, User ID로 참조)
- `BlockedUser` (별도 관리, User ID로 참조)
- `FriendGroup` (별도 관리, User ID로 참조)

**경계 규칙**:
- 친구 관계는 별도 애그리게이트로 관리 (참조 일관성)
- User는 UserId로만 다른 엔티티를 참조

### 6.2 ChatRoom Aggregate

**Root**: `ChatRoom`

**포함 엔티티**:
- `ChatRoom` (Root)
- `ChatRoomSettings` (별도 엔티티, 설정 정보)

**경계 규칙**:
- 참여자는 UserId Set으로 관리 (User 엔티티 직접 참조 X)
- 메시지는 별도 애그리게이트 (MessageId로만 참조)

### 6.3 Message Aggregate

**Root**: `ChatMessage`

**포함 엔티티**:
- `ChatMessage` (Root)
- `MessageBookmark` (별도 관리)
- `ScheduledMessage` (별도 애그리게이트)

**경계 규칙**:
- 메시지는 ChatRoomId와 UserId로만 다른 엔티티 참조
- 리액션은 Value Object로 관리 (MessageReactions)

### 6.4 Notification Aggregate

**Root**: `Notification`

**포함 엔티티**:
- `Notification` (Root)
- `NotificationSettings` (별도 엔티티, 사용자별 알림 설정)

**경계 규칙**:
- 알림은 UserId로만 사용자 참조
- sourceId + sourceType으로 원본 이벤트 참조

---

## 7. 도메인 간 통신

도메인 컨텍스트 간 통신은 **Domain Events**를 통해 이루어집니다.

### 7.1 통신 예시

```
User Context → ChatRoom Context
- 친구 추가 → 1:1 채팅방 자동 생성
  FriendAddedEvent → ChatRoomDomainService.createDirectChat()

Message Context → Notification Context
- 메시지 전송 → 알림 생성
  MessageSentEvent → NotificationDomainService.createFromMessageEvent()

ChatRoom Context → Notification Context
- 참여자 추가 → 알림 생성
  ChatRoomParticipantChangedEvent → NotificationDomainService.createFromChatRoomEvent()
```

### 7.2 이벤트 발행 시점

- **트랜잭션 커밋 전**: 중요한 이벤트 (데이터 일관성 필요)
- **트랜잭션 커밋 후**: 부가 기능 (알림, 로깅 등)

---

## 8. 도메인 타입 (Enums)

### 8.1 User 타입

| Enum | 값 |
|------|---|
| `UserStatus` | ONLINE, OFFLINE, AWAY |
| `FriendRequestStatus` | PENDING, ACCEPTED, REJECTED, CANCELLED |

### 8.2 ChatRoom 타입

| Enum | 값 |
|------|---|
| `ChatRoomType` | INDIVIDUAL (1:1), GROUP (그룹) |

### 8.3 Message 타입

| Enum | 값 |
|------|---|
| `MessageType` | TEXT, IMAGE, VIDEO, AUDIO, FILE |
| `MessageStatus` | SENT, DELIVERED, READ, FAILED |
| `ScheduledMessageStatus` | PENDING, SENT, CANCELLED, FAILED |

### 8.4 Notification 타입

| Enum | 값 |
|------|---|
| `NotificationType` | MESSAGE, FRIEND_REQUEST, MENTION, REACTION, SYSTEM |
| `SourceType` | CHAT, FRIEND, SYSTEM |

### 8.5 Reaction 타입

| Enum | 값 |
|------|---|
| `ReactionType` | LIKE (👍), LOVE (❤️), HAHA (😂), WOW (😮), SAD (😢), ANGRY (😡) |

---

## 9. 도메인 불변식 (Invariants)

도메인 불변식은 항상 참이어야 하는 비즈니스 규칙입니다.

### 9.1 User 불변식

- ✅ Username은 3-20자 사이
- ✅ 비밀번호는 최소 8자 이상
- ✅ UserCode는 중복 불가
- ✅ 삭제된 사용자는 OFFLINE 상태

### 9.2 ChatRoom 불변식

- ✅ 1:1 채팅방은 정확히 2명의 참여자만 가능
- ✅ 자기 자신과 채팅방 생성 불가
- ✅ 참여자가 없는 채팅방은 삭제되어야 함
- ✅ 최대 고정 메시지 수는 5개 (DomainConstants)

### 9.3 Message 불변식

- ✅ TEXT 타입 메시지만 수정 가능
- ✅ 삭제된 메시지는 수정 불가
- ✅ 한 채팅방에는 최대 하나의 고정 메시지만 존재
- ✅ 사용자는 메시지당 하나의 리액션만 가능
- ✅ 메시지 내용은 최대 4000자

### 9.4 Notification 불변식

- ✅ 알림은 특정 사용자에게만 속함
- ✅ 읽음 처리는 한 번만 가능

### 9.5 FriendRequest 불변식

- ✅ 이미 처리된 요청은 재처리 불가
- ✅ PENDING 상태에서만 accept/reject/cancel 가능

---

## 10. 도메인 용어 사전 (Ubiquitous Language)

| 용어 | 의미 |
|-----|-----|
| **User** | 사용자 계정 |
| **UserCode** | 친구 추가용 8자리 코드 (예: A1B2C3D4) |
| **Friendship** | 친구 관계 (양방향) |
| **BlockedUser** | 차단된 사용자 (단방향) |
| **ChatRoom** | 채팅방 (1:1 또는 그룹) |
| **Participant** | 채팅방 참여자 |
| **Pinned** | 고정됨 (채팅방 또는 메시지) |
| **Favorite** | 즐겨찾기 (Pinned와 동일) |
| **Announcement** | 채팅방 공지사항 |
| **Message** | 채팅 메시지 |
| **Reaction** | 메시지 리액션 (👍, ❤️ 등) |
| **Mention** | 멘션 (@사용자명) |
| **Thread** | 메시지 스레드 (답장) |
| **Scheduled Message** | 예약 메시지 |
| **Notification** | 알림 |
| **Domain Event** | 도메인 이벤트 (중요한 사실) |
| **Aggregate** | 애그리게이트 (일관성 경계) |
| **Value Object** | 값 객체 (불변 객체) |

---

## 11. 도메인 패턴

### 11.1 Factory Method Pattern

엔티티 생성 시 유효성 검증과 복잡한 로직을 캡슐화합니다.

- `User.create()` - 사용자 생성
- `ChatRoom.createDirectChat()` - 1:1 채팅방 생성
- `ChatMessage.create()` - 메시지 생성
- `Notification.fromChatEvent()` - 채팅 이벤트로부터 알림 생성

### 11.2 Domain Event Pattern

도메인 내에서 발생한 중요한 사실을 이벤트로 발행하여 느슨한 결합을 유지합니다.

- `MessageSentEvent` → Notification 생성, WebSocket 전송
- `FriendAddedEvent` → 1:1 채팅방 자동 생성

### 11.3 Value Object Pattern

검증 로직을 값 객체에 캡슐화하여 도메인 규칙을 강제합니다.

- `Username.from()` - 3-20자 검증
- `UserCode.generate()` - 8자리 코드 자동 생성

### 11.4 Domain Service Pattern

여러 엔티티에 걸친 복잡한 로직을 도메인 서비스로 분리합니다.

- `MessageEditDomainService` - 메시지 수정 권한 및 시간 제한 확인
- `ChatRoomParticipantDomainService` - 참여자 추가/제거 로직

---

*Last updated: 2025-10-24*
