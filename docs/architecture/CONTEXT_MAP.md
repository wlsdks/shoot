# Context Map

> Shoot 애플리케이션의 Bounded Context 간 관계도

## 개요

Shoot는 5개의 명확하게 분리된 Bounded Context로 구성되어 있습니다. 각 Context는 독립적인 도메인 모델을 가지고 있으며, 명시적인 관계 패턴을 통해 서로 통신합니다.

## Context Map 다이어그램

자세한 다이어그램은 [`diagrams/context-map.mermaid`](./diagrams/context-map.mermaid)를 참조하세요.

## Bounded Contexts

### 1. User Context (핵심 도메인)
- **책임**: 사용자 인증, 권한 관리, 프로필 관리
- **Aggregates**: User, RefreshToken
- **핵심 비즈니스**: 회원 가입, 로그인, 토큰 갱신

### 2. Social Context (지원 도메인)
- **책임**: 친구 관계 관리, 친구 요청 처리
- **Aggregates**: Friendship, FriendRequest
- **핵심 비즈니스**: 친구 추가/삭제, 친구 요청 수락/거절

### 3. ChatRoom Context (핵심 도메인)
- **책임**: 채팅방 생성 및 관리, 참여자 관리
- **Aggregates**: ChatRoom
- **Entities**: ChatRoomSettings
- **핵심 비즈니스**: 1:1/그룹 채팅방 생성, 참여자 추가/제거

### 4. Chat Context (핵심 도메인)
- **책임**: 메시지 송수신, 메시지 수정/삭제, 리액션
- **Aggregates**: ChatMessage
- **핵심 비즈니스**: 메시지 전송, 읽음 처리, 리액션 추가

### 5. Notification Context (지원 도메인)
- **책임**: 알림 생성 및 전송
- **Aggregates**: Notification
- **핵심 비즈니스**: 이벤트 기반 알림 생성, 알림 읽음 처리

## Context 간 관계 패턴

### Shared Kernel (공유 커널)
- **대상**: `UserId` Value Object
- **참여 Context**: User, Social, ChatRoom, Chat, Notification
- **설명**: 모든 Context에서 사용자 식별에 사용하는 공유 타입
- **위치**: `domain/shared/UserId.kt`

### Conformist (순응자)
하위 Context가 상위 Context의 모델을 그대로 수용합니다.

| 하위 Context | 상위 Context | 설명 |
|-------------|-------------|------|
| Social | User | Social은 User의 모델을 그대로 사용 |
| Chat | User | Chat은 User의 모델을 그대로 사용 |
| Notification | User | Notification은 User의 모델을 그대로 사용 |

### Anti-Corruption Layer (ACL)
Context 간 타입 변환을 통해 독립성을 유지합니다.

| Context A | Context B | ACL | 설명 |
|-----------|-----------|-----|------|
| ChatRoom | Chat | `ChatRoomIdConverter` | ChatRoom의 ID를 Chat Context용 ID로 변환 |

**위치**: `application/acl/ChatRoomIdConverter.kt`

### Domain Event (도메인 이벤트)
비동기 통신을 통해 느슨한 결합을 유지합니다.

| 발행 Context | 이벤트 | 구독 Context | 설명 |
|-------------|--------|-------------|------|
| Social | `FriendAddedEvent` | Notification | 친구 추가 알림 |
| ChatRoom | `ChatRoomCreatedEvent` | Notification | 채팅방 생성 알림 |
| Chat | `MessageSentEvent` | Notification | 새 메시지 알림 |
| Chat | `MentionEvent` | Notification | 멘션 알림 |

## 통신 패턴

### 동기 통신 (Synchronous)
- **방식**: Port & Adapter 패턴
- **사용처**: 동일 트랜잭션 내에서 데이터 조회/수정이 필요한 경우
- **예시**:
  - `FriendRequestService`가 `UserQueryPort`를 통해 사용자 존재 확인
  - `ChatMessageService`가 `ChatRoomQueryPort`를 통해 채팅방 정보 조회

### 비동기 통신 (Asynchronous)
- **방식**: Domain Event + `@TransactionalEventListener`
- **사용처**: Context 간 느슨한 결합이 필요한 경우
- **예시**:
  - `FriendAddedEvent` 발행 → `NotificationEventListener`가 알림 생성
  - `MessageSentEvent` 발행 → `NotificationEventListener`가 알림 생성

## MSA 전환 전략

현재 모놀리식 아키텍처지만, Context 간 명확한 경계로 인해 MSA 전환이 용이합니다.

### Phase 1: Context 분리 강화
1. ACL 확장 (모든 VO 변환)
2. Context 간 직접 의존성 제거
3. 모든 통신을 Port 인터페이스로 추상화

### Phase 2: 이벤트 기반 통신 강화
1. Domain Event를 Kafka 메시지로 발행
2. Transactional Outbox 패턴 적용 (완료)
3. Event Sourcing 도입 (선택)

### Phase 3: 물리적 분리
1. User Context → 독립 서비스
2. Social Context → 독립 서비스
3. ChatRoom + Chat Context → 독립 서비스
4. Notification Context → 독립 서비스

## 참고 문서
- [Bounded Contexts 상세](./BOUNDED_CONTEXTS.md)
- [Ubiquitous Language 용어집](./UBIQUITOUS_LANGUAGE.md)
- [DDD 개선 작업 TODO](../../DDD_IMPROVEMENT_TODO.md)
