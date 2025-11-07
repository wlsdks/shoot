# DDD 개선 작업 TODO

> 생성일: 2025-11-08
> 현재 DDD 성숙도: 7.7/10 (B+)
> 목표 DDD 성숙도: 8.5/10 (A+)

---

## 📊 작업 현황

```
전체 진행률: [▰░░░░░░░░░] 1/15 (6.7%)

Critical:  [▰▰▰▰▰░░░░░] 1/2  (50%)
High:      [░░░░░░░░░░] 0/4
Medium:    [░░░░░░░░░░] 0/5
Low:       [░░░░░░░░░░] 0/4
```

---

## 🔴 Critical Priority (1-2주 내 완료 필수)

### ✅ TASK-001: 24시간 메시지 수정 제한 구현 ✅ **완료**
- **우선순위**: 🔴 Critical
- **예상 시간**: 2시간 → **실제: 1.5시간**
- **담당자**: Claude
- **완료일**: 2025-11-08
- **커밋**: `8d6bad0d`

#### 문제점
- CLAUDE.md에 "수정 시간 제한: 24시간" 명시되어 있으나 실제 코드에 없음
- 비즈니스 규칙 위반

#### 작업 파일
- `shoot/src/main/kotlin/com/stark/shoot/domain/chat/message/ChatMessage.kt` ✅
- `shoot/src/main/kotlin/com/stark/shoot/domain/chat/exception/MessageException.kt` ✅ (기존 파일에 추가)
- `shoot/src/test/kotlin/com/stark/shoot/domain/chat/message/ChatMessageTest.kt` ✅

#### 체크리스트
- [x] `ChatMessage.kt:143` - `editMessage()` 메서드 수정 ✅
  - [x] `validateEditTimeLimit()` 메서드 추가 ✅
  - [x] 24시간 검증 로직 구현 ✅
- [x] `MessageException.EditTimeExpired` 예외 클래스 추가 ✅
- [x] 단위 테스트 작성 (총 7개) ✅
  - [x] 23시간 59분: 수정 성공 케이스 ✅
  - [x] 24시간 경과: 수정 실패 케이스 ✅
  - [x] 25시간 경과: 수정 실패 케이스 ✅
  - [x] 기존 테스트 MessageException으로 업데이트 ✅
- [x] 메인 소스 컴파일 성공 확인 ✅
- [x] 커밋 완료 ✅

#### 예상 코드
```kotlin
// ChatMessage.kt
fun editMessage(newContent: String) {
    validateEditTimeLimit()  // 추가
    validateContentNotEmpty(newContent)
    validateMessageNotDeleted()
    validateMessageType()

    this.content = this.content.copy(
        text = newContent,
        isEdited = true
    )
    this.updatedAt = Instant.now()
}

private fun validateEditTimeLimit() {
    val messageAge = Duration.between(
        this.createdAt ?: Instant.now(),
        Instant.now()
    )
    if (messageAge.toHours() > 24) {
        throw MessageEditTimeExpiredException(
            "메시지는 생성 후 24시간 이내에만 수정할 수 있습니다."
        )
    }
}
```

---

### ✅ TASK-002: Saga 보상 실패 알림 메커니즘
- **우선순위**: 🔴 Critical
- **예상 시간**: 1일
- **담당자**: [할당 필요]
- **마감일**: 2025-11-20

#### 문제점
- Saga 보상 트랜잭션 실패 시 로그만 남기고 끝남
- 운영자가 문제를 인지할 수 없음

#### 작업 파일
- `shoot/src/main/kotlin/com/stark/shoot/domain/saga/SagaOrchestrator.kt`
- `shoot/src/main/kotlin/com/stark/shoot/application/port/out/DeadLetterQueuePort.kt` (신규)
- `shoot/src/main/kotlin/com/stark/shoot/application/port/out/AlertNotificationPort.kt` (신규)
- `shoot/src/main/kotlin/com/stark/shoot/adapter/out/kafka/DeadLetterQueueKafkaAdapter.kt` (신규)
- `shoot/src/main/kotlin/com/stark/shoot/adapter/out/notification/SlackAlertAdapter.kt` (신규)

#### 체크리스트
- [ ] Port 인터페이스 생성
  - [ ] `DeadLetterQueuePort` - DLQ 발행
  - [ ] `AlertNotificationPort` - Slack 알림
- [ ] `SagaOrchestrator.compensate()` 메서드 수정
  - [ ] 보상 실패 시 DLQ 발행
  - [ ] Slack 알림 전송
- [ ] Kafka Adapter 구현
  - [ ] Topic: `dead-letter-queue`
  - [ ] 재처리 가능하도록 메타데이터 포함
- [ ] Slack Webhook Adapter 구현
  - [ ] Webhook URL 설정 (application.yml)
  - [ ] 알림 템플릿 작성
- [ ] 테스트 코드 작성
- [ ] 운영 문서 작성 (DLQ 재처리 가이드)

#### 예상 코드
```kotlin
// SagaOrchestrator.kt
private fun compensate(executedSteps: List<SagaStep<T>>, context: T) {
    var allCompensationsSucceeded = true

    executedSteps.reversed().forEach { step ->
        try {
            val success = step.compensate(context)
            if (!success) {
                allCompensationsSucceeded = false
            }
        } catch (e: Exception) {
            logger.error(e) { "Compensation threw exception" }
            allCompensationsSucceeded = false
        }
    }

    // 추가: 보상 실패 시 알림
    if (!allCompensationsSucceeded && context is MessageSagaContext) {
        handleCompensationFailure(context, executedSteps)
    }
}

private fun handleCompensationFailure(
    context: MessageSagaContext,
    executedSteps: List<SagaStep<MessageSagaContext>>
) {
    // DLQ 발행
    deadLetterQueuePort?.publish(
        DeadLetterMessage(
            sagaId = context.sagaId,
            failedSteps = executedSteps.map { it.stepName() },
            errorDetails = context.error?.message,
            payload = context,
            requiresManualIntervention = true,
            timestamp = Instant.now()
        )
    )

    // Slack 알림
    alertNotificationPort?.sendCriticalAlert(
        channel = "#ops-critical",
        title = "🚨 Saga Compensation Failed",
        message = """
            Saga ID: ${context.sagaId}
            Failed Steps: ${executedSteps.joinToString { it.stepName() }}
            Error: ${context.error?.message}
            Requires manual intervention
        """.trimIndent()
    )
}
```

---

## 🟡 High Priority (1-2개월 내 완료)

### ✅ TASK-003: 친구 요청 수락 Saga Pattern 적용
- **우선순위**: 🟡 High
- **예상 시간**: 1주
- **담당자**: [할당 필요]
- **마감일**: 2025-12-15

#### 문제점
- `FriendReceiveService.acceptFriendRequest()`에서 여러 Aggregate 수정
- DDD 원칙 위반: "하나의 트랜잭션에서 하나의 Aggregate만 수정"

#### 작업 파일
- `shoot/src/main/kotlin/com/stark/shoot/application/service/saga/friend/FriendRequestSagaOrchestrator.kt` (신규)
- `shoot/src/main/kotlin/com/stark/shoot/application/service/saga/friend/FriendRequestSagaContext.kt` (신규)
- `shoot/src/main/kotlin/com/stark/shoot/application/service/saga/friend/steps/AcceptFriendRequestStep.kt` (신규)
- `shoot/src/main/kotlin/com/stark/shoot/application/service/saga/friend/steps/CreateFriendshipsStep.kt` (신규)
- `shoot/src/main/kotlin/com/stark/shoot/application/service/saga/friend/steps/PublishFriendEventsStep.kt` (신규)
- `shoot/src/main/kotlin/com/stark/shoot/application/service/user/friend/FriendReceiveService.kt` (수정)

#### 체크리스트
- [ ] Saga Context 클래스 생성
  - [ ] `FriendRequestSagaContext`
  - [ ] 보상용 스냅샷 필드 추가
- [ ] Saga Steps 구현
  - [ ] Step 1: `AcceptFriendRequestStep` (@Transactional)
    - [ ] FriendRequest.accept() 실행
    - [ ] 보상: FriendRequest 상태 PENDING으로 복원
  - [ ] Step 2: `CreateFriendshipsStep` (@Transactional)
    - [ ] 2개의 Friendship 생성
    - [ ] 보상: Friendship 삭제
  - [ ] Step 3: `PublishFriendEventsStep`
    - [ ] FriendAddedEvent 발행 (2개)
    - [ ] 보상: (이벤트는 보상 안 함)
- [ ] `FriendRequestSagaOrchestrator` 구현
  - [ ] OptimisticLockException 재시도 (최대 3회)
  - [ ] Exponential Backoff
- [ ] `FriendReceiveService` 리팩토링
  - [ ] `@Transactional` 제거
  - [ ] Saga Orchestrator 사용
- [ ] 테스트 코드 작성
  - [ ] 정상 시나리오
  - [ ] Step 2 실패 → Step 1 보상
  - [ ] OptimisticLockException 재시도
- [ ] 기존 테스트 수정 (통합 테스트)

#### 설계 구조
```
FriendReceiveService (No @Transactional)
    ↓
FriendRequestSagaOrchestrator
    ↓
SagaOrchestrator
    ├─ Step 1: AcceptFriendRequestStep (@Transactional)
    ├─ Step 2: CreateFriendshipsStep (@Transactional)
    └─ Step 3: PublishFriendEventsStep
```

---

### ✅ TASK-004: FriendRequest Rich Model로 개선
- **우선순위**: 🟡 High
- **예상 시간**: 3일
- **담당자**: [할당 필요]
- **마감일**: 2025-12-20

#### 문제점
- `FriendRequest`가 Anemic Model (빈약한 도메인 모델)
- 비즈니스 로직이 `FriendDomainService`로 유출

#### 작업 파일
- `shoot/src/main/kotlin/com/stark/shoot/domain/social/FriendRequest.kt` (수정)
- `shoot/src/main/kotlin/com/stark/shoot/domain/social/FriendshipPair.kt` (신규)
- `shoot/src/main/kotlin/com/stark/shoot/domain/social/service/FriendDomainService.kt` (수정)
- `shoot/src/test/kotlin/com/stark/shoot/domain/social/FriendRequestTest.kt`

#### 체크리스트
- [ ] `FriendshipPair` Value Object 생성
  - [ ] 2개의 Friendship + 2개의 Event 포함
- [ ] `FriendRequest.accept()` 메서드 개선
  - [ ] Friendship 생성 로직 내재화
  - [ ] FriendAddedEvent 생성 로직 내재화
  - [ ] `FriendshipPair` 반환
- [ ] `FriendDomainService.processFriendAccept()` 제거 또는 단순화
  - [ ] 비즈니스 로직을 `FriendRequest`로 이동
  - [ ] 검증 로직만 남김
- [ ] 단위 테스트 작성
  - [ ] `accept()` 메서드 테스트
  - [ ] PENDING → ACCEPTED 상태 전이
  - [ ] Friendship 생성 검증
  - [ ] Event 생성 검증
- [ ] 통합 테스트 수정

#### 예상 코드
```kotlin
// FriendRequest.kt
fun accept(): FriendshipPair {
    if (status != FriendRequestStatus.PENDING) {
        throw IllegalStateException("이미 처리된 친구 요청입니다: $status")
    }

    status = FriendRequestStatus.ACCEPTED
    respondedAt = Instant.now()

    // 비즈니스 로직 내재화
    return FriendshipPair(
        friendship1 = Friendship.create(receiverId, senderId),
        friendship2 = Friendship.create(senderId, receiverId),
        events = listOf(
            FriendAddedEvent.create(receiverId, senderId),
            FriendAddedEvent.create(senderId, receiverId)
        )
    )
}

// FriendshipPair.kt
data class FriendshipPair(
    val friendship1: Friendship,
    val friendship2: Friendship,
    val events: List<FriendAddedEvent>
)
```

---

### ✅ TASK-005: 동시성 시나리오 통합 테스트
- **우선순위**: 🟡 High
- **예상 시간**: 3일
- **담당자**: [할당 필요]
- **마감일**: 2025-12-25

#### 목적
- 동시성 제어가 실제로 작동하는지 검증
- Race Condition 방지 확인

#### 작업 파일
- `shoot/src/test/kotlin/com/stark/shoot/application/service/concurrency/FriendRequestConcurrencyTest.kt` (신규)
- `shoot/src/test/kotlin/com/stark/shoot/application/service/concurrency/ChatRoomConcurrencyTest.kt` (신규)
- `shoot/src/test/kotlin/com/stark/shoot/application/service/concurrency/MessageEditConcurrencyTest.kt` (신규)

#### 체크리스트
- [ ] 친구 요청 동시성 테스트
  - [ ] 시나리오 1: A→B, B→A 동시 요청
  - [ ] 시나리오 2: 친구 요청 동시 수락/거절
  - [ ] 시나리오 3: 동일 요청 중복 전송
- [ ] 채팅방 생성 동시성 테스트
  - [ ] 시나리오 1: 동시 1:1 채팅방 생성
  - [ ] 시나리오 2: 동일 그룹 채팅방 중복 생성
- [ ] 메시지 수정 동시성 테스트
  - [ ] 시나리오 1: 동일 메시지 동시 수정
  - [ ] 시나리오 2: 메시지 수정 중 삭제
- [ ] OptimisticLockException 재시도 테스트
- [ ] Distributed Lock timeout 테스트
- [ ] 테스트 유틸리티 작성
  - [ ] `ConcurrentTestExecutor` - 동시 실행 헬퍼
  - [ ] `RedisLockTestHelper` - 락 상태 확인

#### 테스트 예시
```kotlin
@SpringBootTest
@Transactional
class FriendRequestConcurrencyTest {

    @Test
    fun `A→B와 B→A 동시 친구 요청 시 하나만 성공`() {
        // Given
        val userA = createUser("A")
        val userB = createUser("B")

        // When: 동시에 2개 요청
        val executor = ConcurrentTestExecutor(threadCount = 2)
        val results = executor.executeAll(
            { sendFriendRequest(userA.id, userB.id) },
            { sendFriendRequest(userB.id, userA.id) }
        )

        // Then: 하나는 성공, 하나는 "이미 요청 받음" 에러
        assertThat(results.filter { it.isSuccess }).hasSize(1)
        assertThat(results.filter { it.isFailure }).hasSize(1)
    }
}
```

---

### ✅ TASK-006: Context Map 다이어그램 문서화
- **우선순위**: 🟡 High
- **예상 시간**: 2일
- **담당자**: [할당 필요]
- **마감일**: 2025-12-30

#### 목적
- Bounded Context 간 관계를 시각화
- 팀원들의 이해도 향상
- MSA 전환 시 참고 자료

#### 작업 파일
- `docs/architecture/CONTEXT_MAP.md` (신규)
- `docs/architecture/diagrams/context-map.mermaid` (신규)
- `docs/architecture/BOUNDED_CONTEXTS.md` (신규)

#### 체크리스트
- [ ] Context Map 다이어그램 작성 (Mermaid)
  - [ ] 5개 Context 표시
  - [ ] Context 간 관계 표시 (Conformist, ACL, Event)
  - [ ] Shared Kernel 표시 (UserId)
- [ ] 각 Context 상세 문서
  - [ ] User Context
  - [ ] Social Context
  - [ ] ChatRoom Context
  - [ ] Chat Context
  - [ ] Notification Context
- [ ] Context 간 통신 패턴 문서화
  - [ ] 동기 통신 (Port 인터페이스)
  - [ ] 비동기 통신 (Domain Event)
- [ ] ACL 목록 정리
  - [ ] 현재: ChatRoomIdConverter
  - [ ] 추가 필요: 기타 VO 변환
- [ ] Ubiquitous Language 용어집

---

## 🟢 Medium Priority (3-6개월 내 완료)

### ✅ TASK-007: User Aggregate에 RefreshToken 통합
- **우선순위**: 🟢 Medium
- **예상 시간**: 1주
- **담당자**: [할당 필요]
- **마감일**: 2026-02-15

#### 문제점
- RefreshToken이 별도 Aggregate로 분리되어 있음
- User와 RefreshToken의 생명주기가 밀접하게 연결되어 있음

#### 작업 파일
- `shoot/src/main/kotlin/com/stark/shoot/domain/user/User.kt` (수정)
- `shoot/src/main/kotlin/com/stark/shoot/domain/user/RefreshToken.kt` (수정 - Entity로 변경)
- `shoot/src/main/kotlin/com/stark/shoot/adapter/out/persistence/postgres/entity/UserEntity.kt` (수정)
- 관련 Service, Port, Adapter 수정

#### 체크리스트
- [ ] `User` Aggregate 수정
  - [ ] `refreshTokens: MutableList<RefreshToken>` 필드 추가
  - [ ] `rotateRefreshToken()` 메서드 추가
  - [ ] `revokeRefreshToken()` 메서드 추가
  - [ ] `validateRefreshToken()` 메서드 추가
- [ ] `RefreshToken` 엔티티 변경
  - [ ] Aggregate Root → Entity로 변경
  - [ ] `@Embeddable` 또는 `@OneToMany` 관계 설정
- [ ] JPA 매핑 수정
  - [ ] `UserEntity`와 `RefreshTokenEntity` 관계 설정
  - [ ] Cascade 옵션 설정
- [ ] 관련 Service 리팩토링
  - [ ] `RefreshTokenService` → `User` Aggregate 메서드 사용
- [ ] 마이그레이션 스크립트 작성 (DB 변경 없음, 로직만 변경)
- [ ] 테스트 코드 수정

---

### ✅ TASK-008: ACL 확장 (모든 VO 변환)
- **우선순위**: 🟢 Medium
- **예상 시간**: 3일
- **담당자**: [할당 필요]
- **마감일**: 2026-02-20

#### 목적
- Context 간 타입 독립성 강화
- 명시적인 Context 경계 설정

#### 작업 파일
- `shoot/src/main/kotlin/com/stark/shoot/application/acl/UserIdConverter.kt` (신규)
- `shoot/src/main/kotlin/com/stark/shoot/application/acl/MessageIdConverter.kt` (신규)
- `shoot/src/main/kotlin/com/stark/shoot/application/acl/ChatRoomIdConverter.kt` (기존, 유지)

#### 체크리스트
- [ ] 현재 ACL 패턴 분석
  - [ ] `ChatRoomIdConverter` 분석
- [ ] 추가 필요한 Converter 식별
  - [ ] `UserIdConverter` (필요시)
  - [ ] `MessageIdConverter` (필요시)
  - [ ] 기타 VO Converter
- [ ] Converter 인터페이스 표준화
  - [ ] `ContextConverter<S, T>` 인터페이스
- [ ] Extension function 추가
  - [ ] `UserId.toSocial()`, `UserId.toChat()` 등
- [ ] 사용처 리팩토링
  - [ ] Event Listener에서 사용
  - [ ] Application Service에서 사용
- [ ] 문서 업데이트
  - [ ] ACL 패턴 가이드 작성

---

### ✅ TASK-009: N+1 쿼리 제거 (배치 쿼리 확대)
- **우선순위**: 🟢 Medium
- **예상 시간**: 5일
- **담당자**: [할당 필요]
- **마감일**: 2026-03-01

#### 목적
- 성능 최적화
- 데이터베이스 부하 감소

#### 작업 파일
- 각 Port 인터페이스에 배치 조회 메서드 추가
- 각 Adapter에 배치 쿼리 구현

#### 체크리스트
- [ ] N+1 쿼리 포인트 식별
  - [ ] 채팅방 목록 조회 시 참여자 정보
  - [ ] 메시지 목록 조회 시 발신자 정보
  - [ ] 친구 목록 조회 시 사용자 정보
  - [ ] 알림 목록 조회 시 관련 엔티티 정보
- [ ] 배치 조회 메서드 추가
  - [ ] `UserQueryPort.findAllByIds(ids: List<UserId>)`
  - [ ] `ChatRoomQueryPort.findAllByIds(ids: Set<ChatRoomId>)`
  - [ ] `MessageQueryPort.findAllByIds(ids: Set<MessageId>)`
- [ ] Spring Data JPA `@EntityGraph` 활용
  - [ ] 필요한 곳에 적용
- [ ] Query DSL 적용 고려
  - [ ] 복잡한 쿼리 최적화
- [ ] 성능 테스트
  - [ ] Before/After 쿼리 수 비교
  - [ ] 응답 시간 비교

---

### ✅ TASK-010: MSA API 계약 정의 (OpenAPI Spec)
- **우선순위**: 🟢 Medium
- **예상 시간**: 1주
- **담당자**: [할당 필요]
- **마감일**: 2026-03-15

#### 목적
- MSA 전환 준비
- 서비스 간 인터페이스 명확화

#### 작업 파일
- `docs/api/user-service-api.yaml` (신규)
- `docs/api/chat-service-api.yaml` (신규)
- `docs/api/notification-service-api.yaml` (신규)
- `docs/api/events-schema.yaml` (신규)

#### 체크리스트
- [ ] User Service API 정의
  - [ ] REST API: 사용자 CRUD, 친구 관리
  - [ ] gRPC API: 내부 서비스용
- [ ] Chat Service API 정의
  - [ ] REST API: 채팅방, 메시지 CRUD
  - [ ] WebSocket API: 실시간 메시지
  - [ ] gRPC API: 내부 서비스용
- [ ] Notification Service API 정의
  - [ ] REST API: 알림 조회, 읽음 처리
  - [ ] gRPC API: 내부 알림 발송
- [ ] Event Schema 정의
  - [ ] Kafka Topic 목록
  - [ ] Event 메시지 포맷 (JSON Schema)
  - [ ] Event 버전 관리 정책
- [ ] Swagger UI 설정
  - [ ] Spring Doc 설정
  - [ ] API 문서 자동 생성

---

### ✅ TASK-011: Event Versioning 구현
- **우선순위**: 🟢 Medium
- **예상 시간**: 3일
- **담당자**: [할당 필요]
- **마감일**: 2026-03-20

#### 목적
- MSA 환경에서 이벤트 스키마 진화 대비
- 하위 호환성 보장

#### 작업 파일
- 모든 Domain Event 클래스 (version 필드 추가)
- `shoot/src/main/kotlin/com/stark/shoot/domain/shared/event/EventVersion.kt` (신규)

#### 체크리스트
- [ ] Event 버전 관리 정책 수립
  - [ ] Semantic Versioning (1.0, 1.1, 2.0)
  - [ ] Breaking Change 정의
- [ ] 모든 Event에 version 필드 추가
  - [ ] `MessageSentEvent` (이미 있음)
  - [ ] `FriendAddedEvent`
  - [ ] `ChatRoomCreatedEvent`
  - [ ] 기타 모든 Event
- [ ] Event Upcasting 메커니즘 구현
  - [ ] 구버전 이벤트를 신버전으로 변환
- [ ] Event Consumer에서 버전 체크
  - [ ] 지원하지 않는 버전 로깅
- [ ] 문서 작성
  - [ ] Event Version 가이드
  - [ ] Breaking Change 시 마이그레이션 절차

---

## 🔵 Low Priority (6개월 이후)

### ✅ TASK-012: CQRS 패턴 확대 적용
- **우선순위**: 🔵 Low
- **예상 시간**: 2주
- **담당자**: [할당 필요]
- **마감일**: 2026-06-01

#### 목적
- 읽기/쓰기 성능 최적화
- 복잡한 조회 쿼리 분리

#### 체크리스트
- [ ] Read Model 식별
  - [ ] 채팅방 목록 (정렬, 필터링, 검색)
  - [ ] 메시지 목록 (페이지네이션, 검색)
  - [ ] 친구 목록 (온라인 상태, 검색)
- [ ] Read Model 전용 DB 구성 (선택)
  - [ ] Redis 또는 Elasticsearch
- [ ] Materialized View 생성
- [ ] 이벤트 핸들러로 Read Model 업데이트

---

### ✅ TASK-013: Event Sourcing 적용 (메시지 이력)
- **우선순위**: 🔵 Low
- **예상 시간**: 3주
- **담당자**: [할당 필요]
- **마감일**: 2026-07-01

#### 목적
- 메시지 이력 완전 추적
- 감사 로그 (Audit Log)
- 시간 여행 디버깅

#### 체크리스트
- [ ] Event Store 설계
  - [ ] MongoDB 또는 전용 DB
- [ ] Message Aggregate Event Sourcing 적용
  - [ ] MessageCreatedEvent
  - [ ] MessageEditedEvent
  - [ ] MessageDeletedEvent
  - [ ] ReactionAddedEvent
- [ ] Snapshot 전략 수립
- [ ] Replay 메커니즘 구현

---

### ✅ TASK-014: Kubernetes 배포 구성
- **우선순위**: 🔵 Low
- **예상 시간**: 2주
- **담당자**: [할당 필요]
- **마감일**: 2026-08-01

#### 체크리스트
- [ ] Dockerfile 작성 (각 서비스)
- [ ] Kubernetes Manifest 작성
  - [ ] Deployment, Service, Ingress
  - [ ] ConfigMap, Secret
- [ ] Helm Chart 작성
- [ ] CI/CD 파이프라인 구성
  - [ ] GitHub Actions 또는 GitLab CI
- [ ] 모니터링 스택 구축
  - [ ] Prometheus, Grafana
  - [ ] ELK Stack (로깅)

---

### ✅ TASK-015: 분산 추적 (Distributed Tracing)
- **우선순위**: 🔵 Low
- **예상 시간**: 1주
- **담당자**: [할당 필요]
- **마감일**: 2026-08-15

#### 체크리스트
- [ ] Jaeger 또는 Zipkin 설정
- [ ] Spring Cloud Sleuth 적용
- [ ] Trace ID 전파 검증
- [ ] 대시보드 구성

---

## 📈 마일스톤

### Milestone 1: Critical Issues 해결 (2025-11-20)
- [x] 분석 완료
- [ ] TASK-001: 24시간 수정 제한
- [ ] TASK-002: Saga 보상 알림

### Milestone 2: DDD 원칙 준수 (2025-12-31)
- [ ] TASK-003: 친구 요청 Saga
- [ ] TASK-004: FriendRequest Rich Model
- [ ] TASK-005: 동시성 테스트
- [ ] TASK-006: Context Map 문서화

### Milestone 3: MSA 준비 (2026-03-31)
- [ ] TASK-007 ~ TASK-011: 인프라 준비
- [ ] DDD 성숙도 8.5/10 달성

### Milestone 4: MSA 전환 (2026-06-30)
- [ ] Notification Service 분리
- [ ] User Service 분리
- [ ] Chat Service 분리

### Milestone 5: 고급 패턴 적용 (2026-12-31)
- [ ] TASK-012 ~ TASK-015
- [ ] DDD 성숙도 9.0/10 달성

---

## 🎯 목표 성숙도 로드맵

```
현재 (2025-11-08):     7.7/10 (B+)
    ↓
Milestone 1 완료:      7.9/10 (B+)
    ↓
Milestone 2 완료:      8.5/10 (A+)
    ↓
Milestone 3 완료:      8.7/10 (A+)
    ↓
Milestone 4 완료:      9.0/10 (S)
    ↓
Milestone 5 완료:      9.5/10 (S+)
```

---

## 📝 작업 규칙

### 브랜치 전략
```
main (protected)
  ├─ develop
  │   ├─ feature/TASK-001-message-edit-time-limit
  │   ├─ feature/TASK-002-saga-compensation-alert
  │   └─ feature/TASK-003-friend-request-saga
  └─ release/v1.0
```

### 커밋 메시지 규칙
```
[TASK-001] Add 24-hour edit time limit to ChatMessage

- Add validateEditTimeLimit() method
- Create MessageEditTimeExpiredException
- Add unit tests for time limit validation

Resolves #TASK-001
```

### PR 체크리스트
- [ ] 단위 테스트 작성 및 통과
- [ ] 통합 테스트 작성 및 통과
- [ ] 코드 리뷰 승인
- [ ] Sonar 분석 통과
- [ ] 문서 업데이트

---

## 🔗 관련 문서

- [DDD 분석 보고서](./DDD_ANALYSIS_REPORT.md) (생성 예정)
- [Context Map](./docs/architecture/CONTEXT_MAP.md) (TASK-006)
- [MSA 전환 계획](./docs/architecture/MSA_MIGRATION_PLAN.md) (생성 예정)
- [API 계약서](./docs/api/) (TASK-010)

---

## 📞 담당자 할당

| 작업 영역 | 담당자 | 작업 개수 |
|----------|--------|----------|
| Domain Model | [미할당] | 4개 (TASK-001, 004, 007, 008) |
| Application Service | [미할당] | 2개 (TASK-003, 009) |
| Infrastructure | [미할당] | 2개 (TASK-002, 014) |
| Testing | [미할당] | 1개 (TASK-005) |
| Documentation | [미할당] | 4개 (TASK-006, 010, 011, 015) |
| Advanced | [미할당] | 2개 (TASK-012, 013) |

---

**생성일**: 2025-11-08
**마지막 업데이트**: 2025-11-08
**다음 검토일**: 2025-11-15
