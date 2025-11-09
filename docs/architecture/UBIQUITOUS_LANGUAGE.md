# Ubiquitous Language 용어집

> Shoot 애플리케이션의 도메인 용어 사전

## 목차
- [공통 용어](#공통-용어)
- [User Context](#user-context)
- [Social Context](#social-context)
- [ChatRoom Context](#chatroom-context)
- [Chat Context](#chat-context)
- [Notification Context](#notification-context)
- [아키텍처 패턴](#아키텍처-패턴)

---

## 공통 용어

### UserId
**타입**: Value Object (Shared Kernel)

모든 Bounded Context에서 사용자를 식별하는 고유 식별자. Long 타입을 감싸는 Value Object로 타입 안정성을 제공합니다.

**사용 예시**:
```kotlin
val userId = UserId(1L)
```

### Aggregate
**타입**: DDD 패턴

일관성 경계를 가지는 도메인 객체의 집합. 하나의 Aggregate Root를 가지며, 외부에서는 Root를 통해서만 접근합니다.

**예시**: User, ChatRoom, ChatMessage, FriendRequest

### Entity
**타입**: DDD 패턴

고유 식별자를 가지는 도메인 객체. 속성이 변경되어도 식별자가 같으면 동일한 객체입니다.

**예시**: ChatRoomSettings, RefreshToken

### Value Object
**타입**: DDD 패턴

식별자 없이 속성만으로 구분되는 불변 객체. 값이 같으면 동일한 객체입니다.

**예시**: UserId, Username, Password, Nickname, MessageContent, FriendshipPair

---

## User Context

### User
**타입**: Aggregate Root

애플리케이션의 사용자. 인증, 권한 관리, 프로필 정보를 담당합니다.

**주요 속성**:
- `id`: UserId
- `username`: Username (3-20자)
- `password`: Password (암호화 저장)
- `nickname`: Nickname (1-30자)
- `email`: Email
- `userCode`: UserCode (8자리 고유 코드)
- `role`: Role (ROLE_USER, ROLE_ADMIN)

### Username
**타입**: Value Object

사용자의 로그인 아이디. 3-20자 제한.

**비즈니스 규칙**:
- 최소 3자, 최대 20자
- 중복 불가 (유니크 제약)

### Password
**타입**: Value Object

사용자의 비밀번호. BCrypt로 암호화하여 저장합니다.

**비즈니스 규칙**:
- 최소 8자
- 평문 저장 금지 (BCrypt 암호화 필수)

### Nickname
**타입**: Value Object

사용자의 표시 이름. 1-30자 제한.

**비즈니스 규칙**:
- 최소 1자, 최대 30자

### UserCode
**타입**: Value Object

사용자를 식별하는 8자리 고유 코드. 대문자와 숫자 조합으로 자동 생성됩니다.

**비즈니스 규칙**:
- 정확히 8자
- 대문자 + 숫자 조합
- 중복 불가 (시스템이 자동 생성 시 중복 검사)

**예시**: `A1B2C3D4`

### Role
**타입**: Enum

사용자의 권한 역할.

**값**:
- `ROLE_USER`: 일반 사용자
- `ROLE_ADMIN`: 관리자

### RefreshToken
**타입**: Aggregate (향후 User Aggregate 내부 Entity로 통합 예정)

JWT 갱신을 위한 토큰.

**주요 속성**:
- `token`: UUID
- `expiresAt`: 만료 시간
- `isRevoked`: 취소 여부

**비즈니스 규칙**:
- 최대 동시 로그인 세션: 5개

---

## Social Context

### Friendship
**타입**: Aggregate Root

두 사용자 간의 친구 관계. 양방향 관계를 위해 2개의 Friendship이 생성됩니다.

**주요 속성**:
- `userId`: UserId (친구 관계의 주체)
- `friendId`: UserId (친구)
- `createdAt`: 친구 추가 시간

**비즈니스 규칙**:
- 최대 친구 수: 1,000명
- 친구 관계는 양방향 (A-B 친구 관계 = Friendship 2개)

### FriendRequest
**타입**: Aggregate Root

친구 요청.

**주요 속성**:
- `senderId`: UserId (요청 발신자)
- `receiverId`: UserId (요청 수신자)
- `status`: FriendRequestStatus
- `createdAt`: 요청 생성 시간
- `respondedAt`: 응답 시간

**상태 전이**:
```
PENDING → ACCEPTED (수락)
        → REJECTED (거절)
        → CANCELLED (취소)
```

**비즈니스 규칙**:
- 자기 자신에게 요청 불가
- 이미 친구인 경우 요청 불가
- PENDING 상태에서만 수락/거절 가능
- 중복 요청 불가

### FriendRequestStatus
**타입**: Enum

친구 요청의 상태.

**값**:
- `PENDING`: 대기 중
- `ACCEPTED`: 수락됨
- `REJECTED`: 거절됨
- `CANCELLED`: 취소됨

### FriendshipPair
**타입**: Value Object

친구 요청 수락 시 생성되는 양방향 Friendship과 이벤트를 담는 객체. Rich Model 패턴의 핵심입니다.

**주요 속성**:
- `friendship1`: Friendship (A→B)
- `friendship2`: Friendship (B→A)
- `events`: List<FriendAddedEvent> (2개)

**비즈니스 규칙**:
- 정확히 2개의 Friendship 포함
- 정확히 2개의 FriendAddedEvent 포함

**사용처**: `FriendRequest.accept()` 메서드의 반환 타입

---

## ChatRoom Context

### ChatRoom
**타입**: Aggregate Root

채팅방.

**주요 속성**:
- `id`: ChatRoomId
- `type`: ChatRoomType
- `participants`: List<UserId>
- `lastMessageId`: MessageId? (마지막 메시지 ID)
- `lastActiveAt`: 마지막 활동 시간
- `isActive`: 활성화 여부

**비즈니스 규칙**:
- 1:1 채팅: 정확히 2명
- 그룹 채팅: 2~100명
- 자기 자신과 채팅방 생성 불가
- 참여자 없으면 자동 삭제
- 동일 사용자 조합의 1:1 채팅방 중복 생성 방지

### ChatRoomType
**타입**: Enum

채팅방의 유형.

**값**:
- `DIRECT`: 1:1 채팅 (정확히 2명)
- `GROUP`: 그룹 채팅 (2~100명)

### ChatRoomSettings
**타입**: Entity (ChatRoom의 내부 Entity)

채팅방별 설정.

**주요 속성**:
- `isNotificationEnabled`: 알림 활성화 (기본: true)
- `retentionDays`: 메시지 보존 기간 (기본: null = 무기한)
- `isEncrypted`: 암호화 여부 (기본: false)
- `customSettings`: Map<String, Any> (커스텀 설정)

---

## Chat Context

### ChatMessage
**타입**: Aggregate Root

채팅 메시지.

**주요 속성**:
- `id`: MessageId
- `roomId`: ChatRoomId (ACL 변환)
- `senderId`: UserId
- `content`: MessageContent
- `mentions`: List<UserId> (멘션된 사용자)
- `readBy`: Map<UserId, Boolean> (읽음 여부)
- `reactions`: Map<UserId, ReactionType> (리액션)
- `createdAt`: 생성 시간
- `updatedAt`: 수정 시간

**비즈니스 규칙**:
- 최대 메시지 길이: 4,000자
- 최대 첨부파일 크기: 50MB
- 수정 시간 제한: 24시간 (생성 후 24시간 이후 수정 불가)
- TEXT 타입만 수정 가능
- 삭제된 메시지는 수정 불가
- 사용자당 메시지별 1개 리액션

### MessageContent
**타입**: Value Object

메시지의 내용.

**주요 속성**:
- `type`: MessageType
- `text`: String?
- `attachments`: List<String>
- `isEdited`: Boolean
- `isDeleted`: Boolean

### MessageType
**타입**: Enum

메시지의 유형.

**값**:
- `TEXT`: 텍스트 메시지
- `IMAGE`: 이미지
- `VIDEO`: 동영상
- `FILE`: 파일

### Reaction
**타입**: Value Object

메시지에 대한 리액션.

**타입**:
- `LIKE`: 좋아요
- `LOVE`: 사랑해요
- `HAHA`: 웃겨요
- `WOW`: 놀라워요
- `SAD`: 슬퍼요
- `ANGRY`: 화나요

**비즈니스 규칙**:
- 사용자당 메시지별 1개 리액션
- 다른 리액션 선택 시 기존 리액션 교체

---

## Notification Context

### Notification
**타입**: Aggregate Root

알림.

**주요 속성**:
- `id`: NotificationId
- `userId`: UserId (수신자)
- `type`: NotificationType
- `content`: JSON (알림 내용)
- `isRead`: Boolean (읽음 여부, 기본: false)
- `createdAt`: 생성 시간

**비즈니스 규칙**:
- 알림은 이벤트 발생 시 자동 생성
- 읽은 알림은 30일 후 자동 삭제
- 사용자당 최대 저장 알림: 1,000개

### NotificationType
**타입**: Enum

알림의 유형.

**값**:
- `FRIEND_REQUEST`: 친구 요청
- `FRIEND_ACCEPTED`: 친구 수락
- `MESSAGE`: 새 메시지
- `MENTION`: 멘션
- `CHAT_ROOM_CREATED`: 채팅방 생성

---

## 아키텍처 패턴

### Saga Pattern
**타입**: 분산 트랜잭션 패턴

여러 Aggregate를 수정하는 비즈니스 프로세스를 관리하는 패턴. 각 Step은 독립적인 트랜잭션으로 실행되며, 실패 시 보상 트랜잭션(Compensation)을 통해 롤백합니다.

**구성 요소**:
- **SagaContext**: Saga 실행 컨텍스트 (상태, 스냅샷)
- **SagaStep**: 개별 실행 단계
- **SagaOrchestrator**: Saga 실행 조정자

**예시**:
- `FriendRequestSaga`: 친구 요청 수락 (3 Steps)
  - Step 1: FriendRequest 상태 변경
  - Step 2: Friendship 2개 생성
  - Step 3: FriendAddedEvent 2개 발행

- `MessageSaga`: 메시지 전송 (3 Steps)
  - Step 1: MongoDB에 메시지 저장
  - Step 2: PostgreSQL ChatRoom 메타데이터 업데이트
  - Step 3: MessageSentEvent 발행

**재시도 정책**:
- OptimisticLockException 발생 시 재시도
- 백오프: 0ms, 10ms, 100ms
- 최대 3회 재시도

### Transactional Outbox
**타입**: 이벤트 발행 패턴

도메인 이벤트를 데이터베이스 트랜잭션 내에서 Outbox 테이블에 저장한 후, 별도 프로세스가 Kafka로 발행하는 패턴. At-Least-Once 전송을 보장합니다.

**구성 요소**:
- **Outbox**: 발행 대기 중인 이벤트 저장 테이블
- **OutboxPublisher**: 주기적으로 Outbox를 폴링하여 Kafka 발행
- **OutboxCleaner**: 발행 완료된 이벤트 정리

**이벤트 상태**:
- `PENDING`: 발행 대기
- `PUBLISHED`: 발행 완료
- `FAILED`: 발행 실패

### ACL (Anti-Corruption Layer)
**타입**: DDD 패턴

Context 간 타입 변환을 담당하는 계층. 하위 Context가 상위 Context의 모델에 오염되지 않도록 보호합니다.

**예시**:
- `ChatRoomIdConverter`: ChatRoom Context의 ChatRoomId → Chat Context의 ChatRoomId

**위치**: `application/acl/`

### Conformist
**타입**: Context 관계 패턴

하위 Context가 상위 Context의 모델을 그대로 수용하는 관계.

**예시**:
- Social Context → User Context (UserId, 사용자 정보)
- Chat Context → User Context (UserId, 발신자 정보)
- Notification Context → User Context (UserId, 수신자 정보)

### Domain Event
**타입**: 이벤트 기반 통신

도메인에서 발생한 중요한 사건을 나타내는 불변 객체. Context 간 비동기 통신에 사용됩니다.

**발행 이벤트**:
- `FriendAddedEvent` (Social → Notification)
- `FriendRequestRejectedEvent` (Social → Notification)
- `ChatRoomCreatedEvent` (ChatRoom → Notification)
- `MessageSentEvent` (Chat → Notification)
- `MentionEvent` (Chat → Notification)

**발행 방식**:
- `@TransactionalEventListener(phase = AFTER_COMMIT)`
- Transactional Outbox 패턴

### Shared Kernel
**타입**: Context 관계 패턴

여러 Context가 공유하는 핵심 도메인 모델.

**예시**:
- `UserId`: 모든 Context에서 사용하는 공유 Value Object

**위치**: `domain/shared/`

**주의사항**:
- Shared Kernel 변경 시 모든 참여 Context 영향
- 신중하게 확장/수정 필요

### DLQ (Dead Letter Queue)
**타입**: 에러 처리 패턴

Saga 실행 실패 또는 보상 트랜잭션 실패 시, 실패한 작업을 별도 큐에 저장하여 운영팀이 수동으로 처리할 수 있도록 합니다.

**구성 요소**:
- Kafka Topic: `dead-letter-queue`
- Slack Alerting: 치명적 실패 시 Slack 알림

**발행 시점**:
- Saga 보상 트랜잭션 실패
- 재시도 횟수 초과

---

## 참고 문서
- [Context Map](./CONTEXT_MAP.md)
- [Bounded Contexts 상세](./BOUNDED_CONTEXTS.md)
- [DDD 개선 작업 TODO](../../DDD_IMPROVEMENT_TODO.md)
