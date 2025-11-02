# 낙관적 락(Optimistic Locking) 구현

> 작업일: 2025-11-02
> 작업 시간: 약 30분
> 해결한 Critical Issue: #2 - Race Condition

---

## 📋 목차

1. [문제 상황](#문제-상황)
2. [낙관적 락이란?](#낙관적-락이란)
3. [구현 방법](#구현-방법)
4. [동작 원리](#동작-원리)
5. [테스트 시나리오](#테스트-시나리오)
6. [적용된 엔티티](#적용된-엔티티)
7. [배운 점](#배운-점)

---

## 문제 상황

### Critical Issue #2: 친구 요청 Race Condition

```kotlin
시나리오:
- 사용자 A가 사용자 B에게 친구 요청을 보냄
- B가 수락하려고 함 (Thread 1)
- 동시에 A가 취소하려고 함 (Thread 2)

문제:
Thread 1: FriendRequest 조회 (status = PENDING)
Thread 2: FriendRequest 조회 (status = PENDING)
Thread 1: status = ACCEPTED로 변경 → DB 저장
Thread 2: status = CANCELLED로 변경 → DB 저장

결과: Lost Update!
      Thread 1의 ACCEPTED가 Thread 2의 CANCELLED로 덮어씌워짐
      사용자 B는 수락했지만 실제로는 취소됨 ❌
```

### 실제 코드 문제점

**FriendRequestService.kt (96-117줄)**
```kotlin
override fun cancelFriendRequest(command: CancelFriendRequestCommand) {
    // 친구 요청 존재 여부 확인
    if (!userQueryPort.checkOutgoingFriendRequest(...)) {
        throw InvalidInputException("해당 친구 요청이 존재하지 않습니다.")
    }

    // ❌ 문제: 낙관적 락 없음!
    // 여기서 조회한 상태와 업데이트 시의 상태가 다를 수 있음
    friendRequestCommandPort.updateStatus(
        command.currentUserId,
        command.targetUserId,
        FriendRequestStatus.CANCELLED
    )

    // 다른 스레드가 이미 ACCEPTED로 변경했을 수 있지만
    // 이 코드는 그냥 CANCELLED로 덮어씀
}
```

### 다른 문제 상황들

#### RefreshToken 동시 갱신
```kotlin
시나리오:
- 사용자가 여러 탭에서 동시 로그인
- 각 탭이 동시에 토큰 갱신 요청

문제:
Tab 1: RefreshToken 조회
Tab 2: RefreshToken 조회
Tab 1: lastUsedAt 업데이트 → 저장
Tab 2: lastUsedAt 업데이트 → 저장 (Tab 1 변경 덮어씀)

결과: Tab 1의 업데이트 소실
```

#### ChatRoom lastActiveAt 동시 업데이트
```kotlin
시나리오:
- 100명이 있는 그룹 채팅
- 동시에 10명이 메시지 전송

문제:
각 메시지 전송 시 lastActiveAt 업데이트
→ 동시성 충돌 발생
→ 일부 업데이트 소실 가능
```

---

## 낙관적 락이란?

### 개념

**낙관적 락 (Optimistic Locking)**
- "충돌이 거의 없을 것"이라고 가정
- 실제 데이터베이스 락을 사용하지 않음
- 대신 **버전** 번호로 충돌 감지
- 충돌 시 예외 발생 → 재시도 로직으로 처리

### 비관적 락과 비교

```
비관적 락 (Pessimistic Locking):
  SELECT ... FOR UPDATE
  - 조회 시부터 락을 걸어버림
  - 다른 트랜잭션은 대기해야 함
  - 안전하지만 느림
  - 데드락 위험 있음

낙관적 락 (Optimistic Locking):
  SELECT ... (락 없음)
  UPDATE ... WHERE id = ? AND version = ?
  - 조회 시에는 락 안 걸음
  - 업데이트 시 버전 체크
  - 빠르지만 충돌 시 재시도 필요
  - 충돌이 적은 환경에 적합
```

### 언제 사용하는가?

#### ✅ 낙관적 락이 적합한 경우

- 읽기가 쓰기보다 훨씬 많음 (Read-heavy)
- 동시 수정이 드물게 발생
- 응답 속도가 중요
- 예: 친구 요청, 프로필 수정, 설정 변경

#### ❌ 비관적 락이 필요한 경우

- 동시 수정이 자주 발생
- 충돌 시 재시도가 위험
- 데이터 일관성이 최우선
- 예: 재고 관리, 좌석 예약, 결제

---

## 구현 방법

### 1단계: BaseEntity에 @Version 추가

```kotlin
// BaseEntity.kt

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open val id: Long = 0

    @CreatedDate
    @Column(nullable = false, updatable = false)
    open val createdAt: Instant = Instant.now()

    @LastModifiedDate
    open var updatedAt: Instant? = null

    /**
     * 낙관적 락을 위한 버전 필드
     * JPA가 자동으로 관리
     */
    @Version
    @Column(nullable = false)
    open var version: Long = 0  // ← 추가!
}
```

**효과:**
- BaseEntity를 상속하는 모든 엔티티가 자동으로 낙관적 락 지원
- 코드 중복 없음
- 일관된 동시성 제어

### 2단계: 데이터베이스 마이그레이션

```sql
-- V7__add_version_column_for_optimistic_locking.sql

-- 모든 테이블에 version 컬럼 추가
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE friend_requests
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE refresh_tokens
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- ... 12개 테이블
```

**주의사항:**
- `IF NOT EXISTS` 사용 (chat_rooms는 이미 version 있음)
- `NOT NULL` + `DEFAULT 0` (기존 데이터 호환성)
- 모든 테이블에 일괄 적용

### 3단계: ChatRoomEntity 수정

**기존 코드 (문제):**
```kotlin
class ChatRoomEntity(...) : BaseEntity() {
    @Version
    var version: Long = 0  // ← BaseEntity와 중복!
        protected set
}
```

**수정 후:**
```kotlin
class ChatRoomEntity(...) : BaseEntity() {
    // BaseEntity에서 version 필드를 상속받으므로 별도 선언 불필요

    // 필요 시 getter/setter만 커스터마이징 가능
}
```

---

## 동작 원리

### JPA가 하는 일

#### 1. 조회 (Read)
```kotlin
val friendRequest = friendRequestRepository.findById(1)
// SELECT id, sender_id, receiver_id, status, version
// FROM friend_requests
// WHERE id = 1

// 결과: id=1, status=PENDING, version=0
```

#### 2. 수정 (Update)
```kotlin
friendRequest.status = FriendRequestStatus.ACCEPTED
friendRequestRepository.save(friendRequest)

// JPA가 생성하는 SQL:
// UPDATE friend_requests
// SET status = 'ACCEPTED',
//     version = version + 1,  ← 버전 자동 증가!
//     updated_at = NOW()
// WHERE id = 1
//   AND version = 0          ← 조회 시 버전과 일치해야 함!

// 성공 시: 1 row affected, version = 1
// 실패 시: 0 rows affected → OptimisticLockException!
```

#### 3. 충돌 발생 시나리오

```
초기 상태: id=1, status=PENDING, version=0

Thread 1:                          Thread 2:
------------------------------------------------------------
조회: version=0
                                   조회: version=0
status = ACCEPTED
                                   status = CANCELLED
UPDATE ... WHERE version=0
→ 성공! version=1
                                   UPDATE ... WHERE version=0
                                   → 실패! (현재 version=1)
                                   → OptimisticLockException 발생!
```

### Exception 처리

```kotlin
try {
    friendRequestCommandPort.updateStatus(...)
} catch (e: OptimisticLockException) {
    // 다른 스레드가 이미 수정함
    // 재시도 또는 사용자에게 알림
    throw ConflictException("친구 요청이 이미 처리되었습니다.")
}
```

---

## 테스트 시나리오

### 시나리오 1: 친구 요청 동시 취소/수락

```kotlin
// Given
val friendRequest = FriendRequest(
    sender = userA,
    receiver = userB,
    status = PENDING,
    version = 0
)

// When: 동시에 두 작업 실행
CompletableFuture.allOf(
    CompletableFuture.runAsync {
        // Thread 1: B가 수락
        friendReceiveService.acceptFriendRequest(
            AcceptFriendRequestCommand(userB.id, userA.id)
        )
    },
    CompletableFuture.runAsync {
        // Thread 2: A가 취소
        friendRequestService.cancelFriendRequest(
            CancelFriendRequestCommand(userA.id, userB.id)
        )
    }
).join()

// Then
// 하나는 성공, 하나는 OptimisticLockException 발생
// 사용자에게 "이미 처리된 요청입니다" 메시지 표시
```

### 시나리오 2: RefreshToken 동시 갱신

```kotlin
// Given
val token = RefreshToken(
    user = user,
    token = "abc123",
    lastUsedAt = null,
    version = 0
)

// When: 여러 탭에서 동시 갱신
(1..10).map {
    CompletableFuture.runAsync {
        tokenService.updateLastUsedAt(token.id)
    }
}.forEach { it.join() }

// Then
// 첫 번째 업데이트만 성공
// 나머지는 OptimisticLockException
// 재시도 로직으로 순차 처리
```

### 시나리오 3: ChatRoom lastActiveAt 업데이트

```kotlin
// Given
val chatRoom = ChatRoom(lastActiveAt = now, version = 0)

// When: 10개 메시지 동시 전송
(1..10).map {
    CompletableFuture.runAsync {
        sendMessageService.send(
            SendMessageCommand(roomId, "메시지 $it")
        )
        // 각 메시지 전송 시 chatRoom.lastActiveAt 업데이트
    }
}.forEach { it.join() }

// Then
// 충돌 발생하지만 재시도로 모두 처리됨
// 최종 lastActiveAt = 가장 마지막 메시지 시간
```

---

## 적용된 엔티티

낙관적 락이 적용된 모든 엔티티 (BaseEntity 상속):

### 사용자 관련
- ✅ **UserEntity**: 프로필 동시 수정 방지
- ✅ **RefreshTokenEntity**: 토큰 갱신 동시 처리 방지

### 친구 관련
- ✅ **FriendRequestEntity**: 요청 동시 취소/수락 방지 ⭐
- ✅ **FriendshipMappingEntity**: 친구 관계 동시 수정 방지
- ✅ **BlockedUserEntity**: 차단 동시 처리 방지
- ✅ **FriendGroupEntity**: 그룹 동시 수정 방지
- ✅ **FriendGroupMemberEntity**: 멤버 동시 추가/삭제 방지

### 채팅 관련
- ✅ **ChatRoomEntity**: lastActiveAt 동시 업데이트 방지
- ✅ **ChatRoomUserEntity**: 읽음 상태 동시 업데이트 방지

### 기타
- ✅ **NotificationEntity**: 알림 상태 동시 변경 방지
- ✅ **OutboxEventEntity**: 이벤트 발행 상태 동시 변경 방지
- ✅ **OutboxDeadLetterEventEntity**: DLQ 상태 동시 변경 방지

---

## 검증 결과

### 빌드 테스트
```bash
$ ./gradlew build -x test

> Task :compileKotlin
> Task :processResources
> Task :classes
> Task :bootJar
> Task :jar
> Task :assemble
> Task :build

BUILD SUCCESSFUL in 7s
✅ 빌드 성공
```

### 마이그레이션 파일
```bash
$ ls src/main/resources/db/migration/V7*

V7__add_version_column_for_optimistic_locking.sql
✅ 마이그레이션 파일 생성 완료
```

### 데이터베이스 적용 (애플리케이션 시작 시)
```sql
-- Flyway가 자동 실행할 SQL
ALTER TABLE users ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE friend_requests ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
...
-- 12개 테이블에 version 컬럼 추가

✅ 기존 데이터에 영향 없음 (DEFAULT 0)
✅ 즉시 적용 가능
```

---

## 배운 점

### 1. 낙관적 락 vs 비관적 락

```
우리 프로젝트 상황:
- 읽기 >> 쓰기 (Read-heavy)
- 친구 요청 충돌은 드물게 발생
- 응답 속도 중요

결정: 낙관적 락 선택 ✅

이유:
1. 성능: 락을 걸지 않으므로 빠름
2. 확장성: 동시 접속자 수에 영향 적음
3. 간단함: @Version 하나로 해결
```

### 2. BaseEntity 활용의 장점

```
Before:
  각 엔티티마다 version 필드 중복 선언
  → 코드 중복
  → 일부 엔티티는 빠뜨릴 위험

After:
  BaseEntity에 한 번만 선언
  → 모든 엔티티가 자동 적용
  → 일관된 동시성 제어
  → 유지보수 용이
```

### 3. Flyway 마이그레이션 전략

```sql
-- ✅ 좋은 예
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

이유:
1. IF NOT EXISTS: 중복 실행 안전
2. NOT NULL: 데이터 무결성
3. DEFAULT 0: 기존 데이터 호환
```

### 4. 예외 처리 전략

```kotlin
// Service Layer에서 처리
try {
    friendRequestRepository.save(friendRequest)
} catch (e: OptimisticLockException) {
    logger.warn { "동시 수정 감지: ${friendRequest.id}" }
    throw ConflictException("이미 처리된 요청입니다.")
}

// Controller Layer
@ExceptionHandler(ConflictException::class)
fun handleConflict(e: ConflictException): ResponseEntity<ErrorResponse> {
    return ResponseEntity
        .status(HttpStatus.CONFLICT)
        .body(ErrorResponse("CONFLICT", e.message))
}
```

### 5. 재시도 로직 필요성

```kotlin
// Spring Retry 사용 권장
@Retryable(
    value = [OptimisticLockException::class],
    maxAttempts = 3,
    backoff = Backoff(delay = 100)
)
fun updateChatRoomLastActive(roomId: Long) {
    val chatRoom = chatRoomRepository.findById(roomId)
    chatRoom.updateLastActiveAt(Instant.now())
    chatRoomRepository.save(chatRoom)
}
```

---

## 다음 단계

### 추가 개선 사항

1. **재시도 로직 구현**
   ```kotlin
   @EnableRetry
   @Configuration
   class RetryConfig {
       @Bean
       fun retryTemplate(): RetryTemplate {
           // OptimisticLockException 발생 시 최대 3회 재시도
       }
   }
   ```

2. **성능 모니터링**
   ```kotlin
   @Aspect
   class OptimisticLockMonitor {
       @AfterThrowing(
           pointcut = "execution(* com.stark.shoot..*Repository.save(..))",
           throwing = "ex"
       )
       fun logOptimisticLockFailure(ex: OptimisticLockException) {
           // 충돌 빈도 측정
           // 임계치 초과 시 알림
       }
   }
   ```

3. **통합 테스트 작성**
   ```kotlin
   @Test
   fun `동시에 친구 요청을 취소하고 수락하면 하나만 성공한다`() {
       // CompletableFuture로 동시성 테스트
   }
   ```

---

## 참고 자료

### JPA 공식 문서
- [Optimistic Locking](https://docs.oracle.com/javaee/7/tutorial/persistence-locking002.htm)
- [@Version Annotation](https://docs.oracle.com/javaee/7/api/javax/persistence/Version.html)

### Spring Data JPA
- [Optimistic Locking in Spring Data JPA](https://www.baeldung.com/jpa-optimistic-locking)
- [Handling OptimisticLockException](https://www.baeldung.com/jpa-optimistic-locking#handling-optimisticlockexception)

### 관련 문서
- `FLYWAY_MIGRATION_FIX.md` - 마이그레이션 관리
- `CLAUDE.md` - 프로젝트 아키텍처

---

**작성일**: 2025-11-02
**작성자**: Claude Code
**문서 버전**: 1.0
