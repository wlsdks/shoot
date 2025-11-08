# Bounded Contexts 상세 문서

> 각 Bounded Context의 책임, 도메인 모델, 비즈니스 규칙

## 1. User Context

### 책임
- 사용자 인증 및 권한 관리
- 사용자 프로필 관리
- RefreshToken 관리

### 도메인 모델

#### Aggregates
- **User**
  - Username (3-20자)
  - Password (최소 8자, 암호화 저장)
  - Nickname (1-30자)
  - Email
  - UserCode (8자리, 고유)
  - Role (ROLE_USER, ROLE_ADMIN)

- **RefreshToken** (향후 User Aggregate 내부 Entity로 통합 예정)
  - Token (UUID)
  - ExpiresAt
  - IsRevoked

### 비즈니스 규칙
- UserCode는 대문자+숫자 조합으로 자동 생성, 중복 불가
- Password는 BCrypt로 암호화
- 최대 동시 로그인 세션: 5개

### 외부 의존성
- 없음 (최상위 Context)

---

## 2. Social Context

### 책임
- 친구 관계 관리
- 친구 요청/수락/거절/취소
- 친구 추천 (BFS 알고리즘)

### 도메인 모델

#### Aggregates
- **Friendship**
  - userId (UserId)
  - friendId (UserId)
  - createdAt

- **FriendRequest**
  - senderId (UserId)
  - receiverId (UserId)
  - status (PENDING, ACCEPTED, REJECTED, CANCELLED)
  - createdAt, respondedAt

#### Value Objects
- **FriendshipPair** (Rich Model 개선)
  - friendship1, friendship2
  - events (2개의 FriendAddedEvent)

### 비즈니스 규칙
- 최대 친구 수: 1,000명
- 자기 자신에게 친구 요청 불가
- 이미 친구인 경우 요청 불가
- PENDING 상태에서만 수락/거절 가능
- 친구 관계는 양방향 (Friendship 2개 생성)

### Saga Pattern
- **FriendRequestSaga**
  - Step 1: FriendRequest 상태 변경 (PENDING → ACCEPTED)
  - Step 2: Friendship 2개 생성 (양방향)
  - Step 3: Outbox에 FriendAddedEvent 2개 저장
  - OptimisticLockException 재시도 지원

### 외부 의존성
- **User Context**: Conformist
  - 사용자 존재 여부 확인 (UserQueryPort)

### 발행 이벤트
- `FriendAddedEvent` → Notification Context

---

## 3. ChatRoom Context

### 책임
- 채팅방 생성 및 관리
- 참여자 관리
- 채팅방 설정 관리

### 도메인 모델

#### Aggregates
- **ChatRoom**
  - id (ChatRoomId)
  - type (DIRECT, GROUP)
  - participants (UserId 목록)
  - lastMessageId
  - lastActiveAt
  - isActive

#### Entities
- **ChatRoomSettings**
  - isNotificationEnabled (기본: true)
  - retentionDays (기본: null = 무기한)
  - isEncrypted (기본: false)
  - customSettings (Map)

### 비즈니스 규칙
- 1:1 채팅: 정확히 2명
- 그룹 채팅: 2~100명
- 자기 자신과 채팅방 생성 불가
- 참여자 없으면 자동 삭제
- 동일 사용자 조합의 1:1 채팅방 중복 생성 방지

### 외부 의존성
- **User Context**: Conformist
  - 참여자 정보 조회 (UserQueryPort)

### ACL 제공
- **ChatRoomIdConverter**
  - ChatRoom Context의 ChatRoomId를 Chat Context용 ChatRoomId로 변환

### 발행 이벤트
- `ChatRoomCreatedEvent` → Notification Context

---

## 4. Chat Context

### 책임
- 메시지 송수신
- 메시지 수정/삭제
- 메시지 읽음 처리
- 리액션 관리

### 도메인 모델

#### Aggregates
- **ChatMessage**
  - id (MessageId)
  - roomId (ChatRoomId, ACL 변환)
  - senderId (UserId)
  - content (MessageContent VO)
    - type (TEXT, IMAGE, VIDEO, FILE)
    - text
    - attachments
    - isEdited
    - isDeleted
  - mentions (UserId 목록)
  - readBy (Map<UserId, Boolean>)
  - reactions (Map<UserId, ReactionType>)
  - createdAt, updatedAt

#### Value Objects
- **MessageContent**
  - type, text, attachments, isEdited, isDeleted

- **Reaction**
  - type (LIKE, LOVE, HAHA, WOW, SAD, ANGRY)

### 비즈니스 규칙
- 최대 메시지 길이: 4,000자
- 최대 첨부파일 크기: 50MB
- **수정 시간 제한: 24시간** (생성 후 24시간 이후 수정 불가)
- TEXT 타입만 수정 가능
- 삭제된 메시지는 수정 불가
- 사용자당 메시지별 1개 리액션 (변경 가능)

### Saga Pattern
- **MessageSaga**
  - Step 1: MongoDB에 메시지 저장
  - Step 2: PostgreSQL ChatRoom 메타데이터 업데이트
  - Step 3: Outbox에 MessageSentEvent 저장
  - OptimisticLockException 재시도 지원

### 외부 의존성
- **User Context**: Conformist
  - 발신자 정보 조회 (UserQueryPort)

- **ChatRoom Context**: ACL (ChatRoomIdConverter)
  - 채팅방 정보 조회 (ChatRoomQueryPort)

### 발행 이벤트
- `MessageSentEvent` → Notification Context
- `MentionEvent` → Notification Context

---

## 5. Notification Context

### 책임
- 도메인 이벤트 기반 알림 생성
- 알림 전송 (WebSocket, FCM)
- 알림 읽음 처리

### 도메인 모델

#### Aggregates
- **Notification**
  - id (NotificationId)
  - userId (UserId)
  - type (FRIEND_REQUEST, MESSAGE, MENTION, etc.)
  - content (JSON)
  - isRead (기본: false)
  - createdAt

### 비즈니스 규칙
- 알림은 이벤트 발생 시 자동 생성
- 읽은 알림은 30일 후 자동 삭제
- 사용자당 최대 저장 알림: 1,000개

### 구독 이벤트
- `FriendAddedEvent` (Social Context)
- `FriendRequestRejectedEvent` (Social Context)
- `ChatRoomCreatedEvent` (ChatRoom Context)
- `MessageSentEvent` (Chat Context)
- `MentionEvent` (Chat Context)

### 외부 의존성
- **User Context**: Conformist
  - 수신자 정보 조회 (UserQueryPort)

---

## Shared Kernel

### UserId
- **타입**: Value Object
- **위치**: `domain/shared/UserId.kt`
- **참여 Context**: User, Social, ChatRoom, Chat, Notification
- **설명**: 모든 Context에서 사용자를 식별하기 위한 공유 타입

### 설계 결정
- **왜 Shared Kernel인가?**
  - UserId는 모든 Context의 핵심 개념
  - 각 Context마다 별도 UserId를 만들면 불필요한 복잡도 증가
  - 타입 안정성을 위해 Long 대신 Value Object 사용

- **향후 MSA 전환 시**
  - Shared Kernel은 별도 라이브러리로 분리
  - 각 서비스가 의존성으로 포함

---

## Context 간 데이터 흐름 예시

### 친구 요청 수락 플로우
```
1. User A가 User B의 친구 요청 수락
   └─> Social Context: FriendRequestSaga 실행
       ├─> Step 1: FriendRequest 상태 변경 (PENDING → ACCEPTED)
       ├─> Step 2: Friendship 2개 생성 (A↔B 양방향)
       └─> Step 3: FriendAddedEvent 2개 발행
           └─> Notification Context: 알림 2개 생성 (A, B에게)
```

### 메시지 전송 플로우
```
1. User A가 ChatRoom에 메시지 전송
   └─> Chat Context: MessageSaga 실행
       ├─> Step 1: MongoDB에 메시지 저장
       ├─> Step 2: PostgreSQL ChatRoom 메타데이터 업데이트
       └─> Step 3: MessageSentEvent 발행
           ├─> Notification Context: 참여자들에게 알림 생성
           └─> WebSocket: 실시간 메시지 전송
```

---

## 참고 문서
- [Context Map](./CONTEXT_MAP.md)
- [Ubiquitous Language 용어집](./UBIQUITOUS_LANGUAGE.md)
