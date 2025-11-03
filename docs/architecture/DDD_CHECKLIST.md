# DDD 패턴 체크리스트

> Domain-Driven Design 패턴 준수 여부 검증

**작성일:** 2025-11-02
**프로젝트:** Shoot 실시간 채팅 애플리케이션

---

## 📋 DDD 전략적 설계 (Strategic Design)

### Bounded Context

| 항목 | 상태 | 평가 | 비고 |
|------|------|------|------|
| Context 식별 | ✅ | **Good** | 6개 Context 식별 |
| Context 명명 | ✅ | **Good** | 명확한 이름 (Identity, Social, Messaging 등) |
| Context 경계 | ⚠️ | **Needs Improvement** | Message ↔ ChatRoom 경계 모호 |
| Context 독립성 | ⚠️ | **Needs Improvement** | Saga로 인한 강한 결합 |
| Ubiquitous Language | ✅ | **Good** | 도메인 용어 일관성 있음 |

**점수:** 4/5 (80%)

---

### Context Map

| 항목 | 상태 | 평가 | 비고 |
|------|------|------|------|
| Context 간 관계 정의 | ✅ | **Excellent** | 명확히 정의됨 |
| 통합 패턴 선택 | ⚠️ | **Needs Improvement** | Conformist 패턴 과다 사용 |
| Publisher-Subscriber | ✅ | **Excellent** | 이벤트 기반 통합 우수 |
| Shared Kernel | ✅ | **Good** | UserId, DomainEvent 적절 |
| Anti-Corruption Layer | ⚠️ | **Fair** | Notification Context만 사용 |
| Open Host Service | ❌ | **Not Used** | REST API만 제공 |
| Conformist | ⚠️ | **Over-used** | Message → ChatRoom 강한 결합 |

**점수:** 5/7 (71%)

---

### Core Domain 식별

| 항목 | 상태 | 평가 | 비고 |
|------|------|------|------|
| Core Domain 정의 | ✅ | **Good** | Messaging, ChatRoom |
| Supporting Domain | ✅ | **Good** | Social, Identity |
| Generic Subdomain | ✅ | **Good** | Notification |
| 투자 우선순위 | ⚠️ | **Needs Improvement** | Core Domain에 Saga 복잡도 과다 |

**점수:** 3/4 (75%)

---

## 🏗️ DDD 전술적 설계 (Tactical Design)

### Aggregate

| 항목 | 상태 | 평가 | 비고 |
|------|------|------|------|
| Aggregate 식별 | ✅ | **Excellent** | 명확한 Aggregate Root |
| Aggregate 크기 | ✅ | **Good** | 적절한 크기 유지 |
| Aggregate 불변식 | ✅ | **Good** | 도메인 규칙 보호 |
| Aggregate 독립성 | ⚠️ | **Needs Improvement** | ChatRoom ↔ Message 의존 |
| ID 참조 | ✅ | **Excellent** | Aggregate 간 ID로 참조 |
| 트랜잭션 경계 | ⚠️ | **Needs Improvement** | Saga로 인한 복잡도 |

**점수:** 5/6 (83%)

**Aggregate 목록:**

| Context | Aggregate Root | Entity | Value Object |
|---------|---------------|--------|--------------|
| Identity | User | - | UserId, Username, Nickname, UserCode |
| Identity | RefreshToken | - | - |
| Social | FriendRequest | - | FriendRequestId, FriendRequestStatus |
| Social | Friendship | - | - |
| Social | BlockedUser | - | - |
| Social | FriendGroup | - | - |
| Messaging | ChatMessage | - | MessageId, MessageContent, MessageReactions |
| Messaging | MessageBookmark | - | - |
| ChatRoom | ChatRoom | ChatRoomSettings | ChatRoomId, ChatRoomTitle |
| Notification | Notification | - | NotificationId, NotificationTitle |
| Notification | NotificationSettings | - | - |

**총 11개 Aggregate Root** ✅

---

### Entity

| 항목 | 상태 | 평가 | 비고 |
|------|------|------|------|
| Entity 식별 | ✅ | **Good** | ID로 식별 가능 |
| Entity 불변성 | ✅ | **Good** | val/var 적절히 사용 |
| Entity 생명주기 | ✅ | **Good** | 팩토리 메서드 제공 |
| Entity 책임 | ✅ | **Good** | 도메인 로직 포함 |

**점수:** 4/4 (100%)

---

### Value Object

| 항목 | 상태 | 평가 | 비고 |
|------|------|------|------|
| Value Object 식별 | ✅ | **Good** | UserId, MessageContent 등 |
| Value Object 불변성 | ✅ | **Excellent** | data class + val |
| Value Object 동등성 | ✅ | **Excellent** | 값 기반 비교 (data class) |
| Value Object 검증 | ⚠️ | **Needs Improvement** | init 블록 미흡 |
| Primitive Obsession 회피 | ⚠️ | **Fair** | UserId가 단순 Long wrapper |

**점수:** 3/5 (60%)

**개선 필요:**
```kotlin
// 현재
@JvmInline
value class UserId(val value: Long)  // 단순 wrapper

// 개선
@JvmInline
value class UserId(val value: Long) {
    init {
        require(value > 0) { "UserId must be positive" }
    }

    companion object {
        fun from(value: Long): UserId {
            require(value > 0) { "UserId must be positive" }
            return UserId(value)
        }
    }
}
```

---

### Domain Service

| 항목 | 상태 | 평가 | 비고 |
|------|------|------|------|
| Domain Service 식별 | ✅ | **Good** | FriendDomainService 등 |
| Stateless | ✅ | **Excellent** | 상태 없는 서비스 |
| 도메인 로직 집중 | ✅ | **Good** | 비즈니스 규칙 포함 |
| 명명 규칙 | ✅ | **Good** | *DomainService 접미사 |

**점수:** 4/4 (100%)

**Domain Service 목록:**
- `FriendDomainService` (친구 요청 검증)
- `MessageEditDomainService` (메시지 수정 검증)
- `ChatRoomDomainService` (채팅방 검증)
- `ChatRoomMetadataDomainService` (메타데이터 업데이트)
- `ChatRoomParticipantDomainService` (참여자 관리)
- `ChatRoomValidationDomainService` (채팅방 유효성)
- `ChatRoomEventService` (채팅방 이벤트)
- `NotificationDomainService` (알림 생성)

---

### Domain Event

| 항목 | 상태 | 평가 | 비고 |
|------|------|------|------|
| Event 식별 | ✅ | **Excellent** | 19개 이벤트 식별 |
| Event 명명 | ✅ | **Excellent** | 과거형 명명 (UserCreatedEvent) |
| Event 불변성 | ✅ | **Excellent** | data class + val |
| Event 시간 정보 | ✅ | **Good** | occurredAt/createdAt 포함 |
| Event 발행 시점 | ✅ | **Good** | @TransactionalEventListener |
| Event 구독 분리 | ✅ | **Excellent** | Context별 리스너 분리 |

**점수:** 6/6 (100%)

**Domain Event 목록:**
- UserCreatedEvent, UserDeletedEvent
- FriendRequestSentEvent, FriendAddedEvent, FriendRemovedEvent
- ChatRoomCreatedEvent, ChatRoomParticipantChangedEvent
- MessageSentEvent, MessageEditedEvent, MessageDeletedEvent
- MessageReactionEvent, MentionEvent
- NotificationEvent

---

### Repository

| 항목 | 상태 | 평가 | 비고 |
|------|------|------|------|
| Repository 패턴 | ✅ | **Excellent** | Port/Adapter로 구현 |
| Aggregate당 Repository | ✅ | **Good** | 1:1 매핑 |
| Collection 추상화 | ✅ | **Good** | findAll, save 등 |
| 명명 규칙 | ✅ | **Excellent** | QueryPort, CommandPort |
| 쿼리 최적화 | ✅ | **Good** | 별도 QueryPort |

**점수:** 5/5 (100%)

---

### Factory

| 항목 | 상태 | 평가 | 비고 |
|------|------|------|------|
| Factory 패턴 사용 | ✅ | **Good** | companion object create() |
| 복잡한 생성 로직 | ✅ | **Good** | User.create() 등 |
| 유효성 검증 | ⚠️ | **Fair** | 일부만 검증 |

**점수:** 2/3 (67%)

**개선 필요:**
```kotlin
// 현재
data class User(...) {
    companion object {
        fun create(username: String, ...): User {
            return User(...)  // 일부 검증만
        }
    }
}

// 개선
data class User(...) {
    init {
        // 모든 불변식 검증
        require(username.value.length in 3..20)
        require(nickname.value.length in 1..30)
    }

    companion object {
        fun create(...): User {
            // 팩토리 메서드도 검증
            validateUsername(username)
            validateNickname(nickname)
            return User(...)
        }
    }
}
```

---

### Specification

| 항목 | 상태 | 평가 | 비고 |
|------|------|------|------|
| Specification 패턴 | ❌ | **Not Used** | 비즈니스 규칙이 서비스에 산재 |

**점수:** 0/1 (0%)

**개선 제안:**
```kotlin
// 현재: 비즈니스 규칙이 서비스에 산재
class FriendRequestService {
    fun sendFriendRequest(...) {
        if (currentUserId == targetUserId) throw Exception()
        if (isFriend) throw Exception()
        if (hasOutgoingRequest) throw Exception()
    }
}

// 개선: Specification 패턴
interface FriendRequestSpecification {
    fun isSatisfiedBy(currentUserId: UserId, targetUserId: UserId): Boolean
    fun whyNotSatisfied(): String
}

class NotSelfSpecification : FriendRequestSpecification {
    override fun isSatisfiedBy(...) = currentUserId != targetUserId
    override fun whyNotSatisfied() = "자기 자신에게 친구 요청 불가"
}

class NotAlreadyFriendsSpecification : FriendRequestSpecification {
    override fun isSatisfiedBy(...) = !friendRepository.areFriends(...)
    override fun whyNotSatisfied() = "이미 친구 관계"
}
```

---

## 🏛️ 아키텍처 패턴

### Hexagonal Architecture (Ports & Adapters)

| 항목 | 상태 | 평가 | 비고 |
|------|------|------|------|
| Port 인터페이스 정의 | ✅ | **Excellent** | port/in, port/out 명확 |
| Adapter 구현 분리 | ✅ | **Excellent** | adapter/in, adapter/out |
| 의존성 방향 | ✅ | **Excellent** | 도메인이 중심 |
| Port 명명 규칙 | ✅ | **Excellent** | UseCase, QueryPort, CommandPort |

**점수:** 4/4 (100%)

---

### CQRS (Command Query Responsibility Segregation)

| 항목 | 상태 | 평가 | 비고 |
|------|------|------|------|
| Command/Query 분리 | ✅ | **Good** | CommandPort, QueryPort |
| Read Model 최적화 | ⚠️ | **Fair** | 일부만 분리 |
| Event Sourcing | ❌ | **Not Used** | - |

**점수:** 1/3 (33%)

---

### Event-Driven Architecture

| 항목 | 상태 | 평가 | 비고 |
|------|------|------|------|
| Domain Event 사용 | ✅ | **Excellent** | 19개 이벤트 |
| Event 발행 | ✅ | **Excellent** | @TransactionalEventListener |
| Event 구독 | ✅ | **Excellent** | Context별 리스너 |
| Event Store | ⚠️ | **Partial** | Outbox 패턴 사용 |
| Event Replay | ❌ | **Not Implemented** | - |

**점수:** 3/5 (60%)

---

### Saga Pattern

| 항목 | 상태 | 평가 | 비고 |
|------|------|------|------|
| Saga 구현 | ✅ | **Good** | MessageSagaOrchestrator |
| Orchestration | ✅ | **Good** | 중앙 집중형 |
| 보상 트랜잭션 | ⚠️ | **Needs Improvement** | OptimisticLock 문제 해결됨 |
| 멱등성 보장 | ✅ | **Good** | message.copy() 사용 |
| Saga 복잡도 | ⚠️ | **Too Complex** | 이벤트 기반 전환 권장 |

**점수:** 3/5 (60%)

---

## 📊 전체 점수

| 카테고리 | 점수 | 평가 |
|---------|------|------|
| **전략적 설계** | 12/16 (75%) | **Good** |
| **전술적 설계** | 23/28 (82%) | **Good** |
| **아키텍처 패턴** | 11/17 (65%) | **Fair** |
| **전체** | **46/61 (75%)** | **Good** |

---

## ⚠️ 개선 우선순위

### 🔴 Priority 1: Messaging ↔ ChatRoom 결합 제거

**현재 문제:**
- Saga 패턴으로 강한 결합
- 복잡한 트랜잭션 경계
- 보상 로직 복잡

**개선 방법:**
- Saga 제거
- Publisher-Subscriber 패턴으로 전환
- 결과적 일관성 수용

**예상 효과:**
- 전체 점수: 75% → 85%

---

### 🟠 Priority 2: Value Object 강화

**현재 문제:**
- UserId, MessageId 등이 단순 wrapper
- init 블록 검증 미흡
- Primitive Obsession

**개선 방법:**
```kotlin
@JvmInline
value class UserId(val value: Long) {
    init {
        require(value > 0) { "UserId must be positive" }
    }

    fun tostring(): String = value.toString()

    companion object {
        fun from(value: Long): UserId = UserId(value)
        fun fromString(value: String): UserId = UserId(value.toLong())
    }
}
```

**예상 효과:**
- Value Object 점수: 60% → 90%

---

### 🟡 Priority 3: Specification 패턴 도입

**현재 문제:**
- 비즈니스 규칙이 서비스에 산재
- 재사용 어려움
- 테스트 어려움

**개선 방법:**
- Specification 인터페이스 정의
- 비즈니스 규칙을 Specification으로 추출
- and(), or(), not() 조합 지원

**예상 효과:**
- 비즈니스 로직 재사용성 향상
- 테스트 용이성 증가

---

### 🟢 Priority 4: Read Model 최적화

**현재 문제:**
- CQRS 일부만 적용
- Read Model 최적화 미흡

**개선 방법:**
- 조회 전용 DTO 분리
- 복잡한 쿼리는 별도 Read Model
- 캐싱 전략 강화

---

## ✅ 잘 구현된 부분

### 1. Hexagonal Architecture

✅ **Excellent 구현:**
```
application/
├── port/
│   ├── in/       # Use Cases
│   └── out/      # Repository, Event
└── service/      # Application Services

adapter/
├── in/           # Controllers, WebSocket
└── out/          # JPA, MongoDB, Kafka

domain/           # 핵심 비즈니스 로직
```

### 2. Domain Event

✅ **19개 명확한 이벤트:**
- 과거형 명명 (UserCreatedEvent)
- 불변성 (data class + val)
- @TransactionalEventListener로 안전한 발행

### 3. Repository 패턴

✅ **Port/Adapter로 완벽 구현:**
- QueryPort / CommandPort 분리
- Aggregate당 1개 Repository
- 테스트 용이

### 4. Redis 분산 락

✅ **동시성 제어 우수:**
- 친구 요청 중복 방지
- 채팅방 중복 생성 방지
- Lua 스크립트로 안전한 락 해제

### 5. OptimisticLock + Retry

✅ **충돌 처리 우수:**
- JPA @Version 필드
- @Retryable로 자동 재시도
- 지수 백오프

---

## 📝 결론

### 현재 상태:
- ✅ **DDD 전략적 설계: 75%** (Good)
- ✅ **DDD 전술적 설계: 82%** (Good)
- ⚠️ **아키텍처 패턴: 65%** (Fair)
- 🎯 **전체: 75%** (Good)

### 강점:
1. **Hexagonal Architecture 완벽 구현**
2. **Domain Event 우수**
3. **Repository 패턴 우수**
4. **동시성 제어 우수**

### 약점:
1. **Messaging ↔ ChatRoom 강한 결합** (Saga)
2. **Value Object 약함** (Primitive Obsession)
3. **Specification 패턴 미사용**
4. **CQRS 일부만 적용**

### 개선 후 예상:
- **Saga 제거:** 75% → 80%
- **Value Object 강화:** 80% → 83%
- **Specification 도입:** 83% → 87%
- 🎯 **목표: 85%+**

---

**작성자:** Claude Code
**검토 날짜:** 2025-11-02
**다음 단계:** Priority 1 개선 작업 (Saga → Event-driven)
