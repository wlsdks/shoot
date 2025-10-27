# 분산 락(Distributed Lock) 적용 전략

> **작성일**: 2025-10-27
> **목적**: 친구 요청 및 채팅방 생성의 Race Condition 방지

---

## 📋 목차

1. [왜 분산 락이 필요한가?](#왜-분산-락이-필요한가)
2. [해결하려는 문제](#해결하려는-문제)
3. [분산 락 동작 원리](#분산-락-동작-원리)
4. [적용 사례](#적용-사례)
5. [기술적 구현](#기술적-구현)
6. [성능 고려사항](#성능-고려사항)
7. [대안 분석](#대안-분석)

---

## 왜 분산 락이 필요한가?

### 문제 상황: Race Condition

실시간 채팅 애플리케이션은 **다중 서버 환경**에서 동작합니다. 여러 사용자가 동시에 요청을 보낼 때, 두 요청이 서로 다른 서버에서 처리되면 **Race Condition**이 발생할 수 있습니다.

```
┌─────────────┐                   ┌─────────────┐
│  Server A   │                   │  Server B   │
│             │                   │             │
│  User A →   │───────┐   ┌───────│   ← User B  │
│  친구 요청  │       │   │       │   친구 요청 │
└─────────────┘       │   │       └─────────────┘
                      │   │
                      ▼   ▼
              ┌────────────────┐
              │   PostgreSQL   │
              │                │
              │  ❌ 두 개의     │
              │  PENDING 요청  │
              │  동시 생성!    │
              └────────────────┘
```

### 단일 서버 락의 한계

Java/Kotlin의 `synchronized` 또는 `ReentrantLock`은 **같은 JVM 내에서만** 동작합니다.

```kotlin
// ❌ 다중 서버 환경에서는 작동하지 않음!
@Synchronized
fun sendFriendRequest(...) {
    // Server A의 락과 Server B의 락은 독립적
    // 동시 실행 가능!
}
```

따라서 **Redis 기반 분산 락**이 필요합니다.

---

## 해결하려는 문제

### 1. 친구 요청 중복 생성

#### 문제 시나리오

| 시간 | User A 요청 (Server 1)          | User B 요청 (Server 2)          |
|------|--------------------------------|--------------------------------|
| T1   | `checkIncomingRequest(A→B)` → false | `checkIncomingRequest(B→A)` → false |
| T2   | `saveFriendRequest(A→B)` ✅     | `saveFriendRequest(B→A)` ✅     |
| T3   | **결과**: 두 개의 PENDING 요청 공존 | **문제**: 한쪽 수락 시 다른 요청은 orphan |

#### 영향

- 데이터 정합성 깨짐
- 사용자 혼란 (두 사람 모두 "친구 요청 보냄" 상태)
- 한쪽을 수락해도 다른 요청은 PENDING으로 남음

---

### 2. 1:1 채팅방 중복 생성

#### 문제 시나리오

```
Time | User A (Server 1)                   | User B (Server 2)
-----|-------------------------------------|-------------------------------------
T1   | findDirectChatBetween(A, B) → null  | findDirectChatBetween(B, A) → null
T2   | createDirectChat(A, B) ✅            | createDirectChat(B, A) ✅
T3   | 결과: 같은 두 사용자 간 채팅방이 2개 생성됨
```

#### 영향

- 메시지 분산 (채팅방 1에 메시지, 채팅방 2에 메시지)
- 사용자 혼란 (어느 채팅방을 사용해야 할지 불명확)
- 채팅방 목록에 중복 표시

---

## 분산 락 동작 원리

### Redis SETNX 활용

```redis
# 락 획득 시도
SETNX lock:friend-request:100:200 "server-1-thread-42"
EX 10  # 10초 후 자동 만료

# 반환값:
# 1 → 락 획득 성공
# 0 → 이미 다른 요청이 락 보유 중
```

### 지수 백오프 재시도

```kotlin
var currentRetry = 0
while (!acquired && currentRetry < maxRetries) {
    acquired = redisTemplate.setIfAbsent(lockKey, ownerId, timeout)

    if (!acquired) {
        // 재시도 간격: 100ms → 200ms → 400ms → 800ms → 1000ms (최대)
        val waitTime = minOf(100L * (1 shl currentRetry), 1000L)
        Thread.sleep(waitTime)
        currentRetry++
    }
}
```

### 안전한 락 해제 (Lua 스크립트)

```lua
-- 자신이 획득한 락만 해제 가능
if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
else
    return 0  -- 다른 소유자의 락은 해제 불가
end
```

---

## 적용 사례

### 1. 친구 요청 (FriendRequestService)

#### 락 키 생성 전략

```kotlin
// 두 사용자 ID를 정렬하여 A→B, B→A 요청에 동일한 락 사용
val sortedIds = listOf(currentUserId.value, targetUserId.value).sorted()
val lockKey = "friend-request:${sortedIds[0]}:${sortedIds[1]}"

// 예시:
// User 100 → User 200: "friend-request:100:200"
// User 200 → User 100: "friend-request:100:200" (동일!)
```

#### 동작 흐름

```kotlin
private fun processFriendRequest(currentUserId: UserId, targetUserId: UserId) {
    val lockKey = "friend-request:${sortedIds[0]}:${sortedIds[1]}"

    redisLockManager.withLock(lockKey, currentUserId.value.toString()) {
        // 1. 사용자 존재 확인
        validateUserExistence(currentUserId, targetUserId)

        // 2. 중복 요청 검증 (이제 안전!)
        friendDomainService.validateFriendRequest(
            isFriend = userQueryPort.checkFriendship(...),
            hasOutgoingRequest = userQueryPort.checkOutgoingFriendRequest(...),
            hasIncomingRequest = userQueryPort.checkIncomingFriendRequest(...)
        )

        // 3. 친구 요청 저장
        val request = FriendRequest(senderId = currentUserId, receiverId = targetUserId)
        friendRequestCommandPort.saveFriendRequest(request)

        // 4. 캐시 무효화 및 이벤트 발행
        ...
    }
}
```

#### Race Condition 방지

| 시간 | User A 요청 (Server 1)          | User B 요청 (Server 2)          |
|------|--------------------------------|--------------------------------|
| T1   | 🔒 락 획득: `friend-request:100:200` | 🔒 락 획득 시도 → **대기** |
| T2   | `checkIncomingRequest(A→B)` → false | (락 대기 중...) |
| T3   | `saveFriendRequest(A→B)` ✅     | (락 대기 중...) |
| T4   | 🔓 락 해제                      | 🔒 락 획득 성공! |
| T5   |                                | `checkIncomingRequest(B→A)` → **true** ✅ |
| T6   |                                | ❌ "이미 친구 요청을 받았습니다" 예외 발생 |

✅ **결과**: 한 개의 PENDING 요청만 생성, 중복 방지 성공!

---

### 2. 1:1 채팅방 생성 (CreateChatRoomService)

#### 락 키 생성 전략

```kotlin
val sortedIds = listOf(userId.value, friendId.value).sorted()
val lockKey = "chatroom:direct:${sortedIds[0]}:${sortedIds[1]}"

// 예시:
// User 100 ↔ User 200: "chatroom:direct:100:200"
```

#### 동작 흐름

```kotlin
override fun createDirectChat(command: CreateDirectChatCommand): ChatRoomResponse {
    val lockKey = "chatroom:direct:${sortedIds[0]}:${sortedIds[1]}"

    return redisLockManager.withLock(lockKey, userId.value.toString()) {
        // 1. 사용자 존재 확인
        val friend = userQueryPort.findUserById(friendId) ?: throw ...

        // 2. 기존 채팅방 검색 (이제 안전!)
        val existingRoom = chatRoomDomainService.findDirectChatBetween(...)
        if (existingRoom != null) return@withLock ChatRoomResponse.from(existingRoom)

        // 3. 새 채팅방 생성
        val newChatRoom = ChatRoom.createDirectChat(userId, friendId, friendName)
        val savedRoom = chatRoomCommandPort.save(newChatRoom)

        // 4. 이벤트 발행
        publishChatRoomCreatedEvent(savedRoom)

        ChatRoomResponse.from(savedRoom, userId.value)
    }
}
```

---

## 기술적 구현

### RedisLockManager 주요 메서드

```kotlin
@Component
class RedisLockManager(
    private val redisTemplate: StringRedisTemplate,
    private val properties: RedisLockProperties
) {
    /**
     * 분산 락을 획득하여 작업 실행
     *
     * @param lockKey 락 키 (예: "friend-request:100:200")
     * @param ownerId 락 소유자 ID (스레드 식별용)
     * @param retryCount 최대 재시도 횟수 (기본: 3회)
     * @param autoExtend 작업 실행 중 자동 락 연장 여부
     * @param action 락 획득 후 실행할 작업
     */
    fun <T> withLock(
        lockKey: String,
        ownerId: String,
        retryCount: Int = maxRetries,
        autoExtend: Boolean = false,
        action: () -> T
    ): T
}
```

### 설정 (application.yml)

```yaml
redis:
  lock:
    lock-timeout: 10s          # 락 자동 만료 시간
    lock-wait-timeout: 5s      # 락 획득 대기 시간
    max-retries: 3             # 최대 재시도 횟수
```

---

## 성능 고려사항

### 1. 락 타임아웃

```kotlin
// 기본값: 10초
// 너무 짧으면: 작업 중 락 해제 → 다른 요청 진입 → 데이터 충돌
// 너무 길면: 서버 장애 시 복구 지연
```

**권장**: 작업 예상 시간의 2-3배 (평균 응답 시간 500ms → 타임아웃 2-3초)

### 2. 재시도 전략

```kotlin
// 지수 백오프: 100ms → 200ms → 400ms → 800ms → 1000ms (최대)
// 장점: 락 경합 감소, 공정성 향상
// 단점: 최대 대기 시간 증가 (최대 3초)
```

### 3. 락 범위

| 락 키 패턴 | 범위 | 장점 | 단점 |
|----------|-----|------|------|
| `friend-request:{userId1}:{userId2}` | 특정 두 사용자 | 세밀한 제어, 높은 병렬성 | 키 관리 복잡 |
| `friend-request:{userId}` | 특정 사용자의 모든 요청 | 간단한 구현 | 병렬성 저하 |
| `friend-request:global` | 전체 친구 요청 | 매우 간단 | ❌ 병렬성 최악 |

**선택**: `friend-request:{userId1}:{userId2}` (최적 병렬성)

---

## 대안 분석

### 1. Database Unique Constraint

```sql
-- 방법 1: UNIQUE 제약조건
ALTER TABLE friend_request ADD CONSTRAINT uk_friend_request
UNIQUE (sender_id, receiver_id, status);

-- 장점: DB 레벨 보장, 추가 인프라 불필요
-- 단점:
--  - 예외 처리 필요 (UniqueViolationException)
--  - 트랜잭션 롤백 비용
--  - 사용자 경험 저하 (동시 요청 시 한쪽 실패)
```

**평가**: ✅ 보조 수단으로 사용, 주 메커니즘으로는 부족

### 2. Optimistic Locking

```kotlin
@Entity
class FriendRequest(
    @Version
    var version: Long = 0L
    // ...
)

// 장점: 간단한 구현
// 단점:
//  - 충돌 시 OptimisticLockException 발생
//  - 재시도 로직 필요
//  - 사용자 경험 저하
```

**평가**: ❌ Race Condition 방지에는 부적합

### 3. Pessimistic Locking

```kotlin
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT f FROM FriendRequest f WHERE ...")
fun findForUpdate(...): FriendRequest?

// 장점: DB 레벨 락, 확실한 보장
// 단점:
//  - 데드락 가능성
//  - 성능 저하 (DB 락 경합)
//  - 다중 테이블 작업 시 복잡
```

**평가**: ⚠️ 단일 레코드 수정에는 적합, 복잡한 비즈니스 로직에는 부족

### 4. 분산 락 (Redis) ✅

```kotlin
redisLockManager.withLock(lockKey) {
    // 비즈니스 로직
}

// 장점:
//  - 다중 서버 환경 지원
//  - 유연한 범위 제어
//  - 트랜잭션과 독립적
//  - 재시도 로직 내장
// 단점:
//  - Redis 의존성
//  - 네트워크 지연 (평균 1-2ms)
```

**평가**: ✅ **최적 선택** (다중 서버, 복잡한 비즈니스 로직)

---

## 모니터링 및 알람

### 주요 메트릭

1. **락 획득 실패율**
   ```kotlin
   counter("redis.lock.acquisition.failure", "lockKey" to lockKey)
   ```

2. **락 대기 시간**
   ```kotlin
   timer("redis.lock.wait.time", "lockKey" to lockKey).record(waitTime)
   ```

3. **락 보유 시간**
   ```kotlin
   timer("redis.lock.hold.time", "lockKey" to lockKey).record(holdTime)
   ```

### 알람 조건

- 락 획득 실패율 > 5%: 🚨 Redis 성능 저하 또는 타임아웃 설정 부족
- 평균 대기 시간 > 1초: ⚠️ 락 경합 심화, 재시도 로직 조정 필요
- 락 보유 시간 > 5초: ⚠️ 비즈니스 로직 최적화 필요

---

## 결론

### 언제 분산 락을 사용해야 하는가?

✅ **사용해야 하는 경우**:
- 다중 서버 환경
- 동시 요청으로 인한 중복 생성 방지
- 복잡한 비즈니스 로직 (여러 테이블 조회/수정)
- 트랜잭션 경계를 넘는 작업

❌ **사용하지 않아도 되는 경우**:
- 단일 서버 환경
- 단순 레코드 수정 (Optimistic/Pessimistic Locking 충분)
- 중복이 허용되는 경우 (로그 기록 등)

### 프로젝트 적용 결과

| 항목 | 적용 전 | 적용 후 |
|-----|--------|--------|
| 친구 요청 중복 | ❌ 발생 가능 | ✅ 방지 |
| 채팅방 중복 | ❌ 발생 가능 | ✅ 방지 |
| 데이터 정합성 | ⚠️ 취약 | ✅ 보장 |
| 사용자 경험 | ⚠️ 혼란 | ✅ 일관성 |

---

**참고 자료**:
- [Redis SETNX Documentation](https://redis.io/commands/setnx/)
- [Redlock Algorithm](https://redis.io/docs/manual/patterns/distributed-locks/)
- [Martin Kleppmann - How to do distributed locking](https://martin.kleppmann.com/2016/02/08/how-to-do-distributed-locking.html)

**관련 파일**:
- `src/main/kotlin/com/stark/shoot/infrastructure/config/redis/RedisLockManager.kt`
- `src/main/kotlin/com/stark/shoot/application/service/user/friend/FriendRequestService.kt`
- `src/main/kotlin/com/stark/shoot/application/service/chatroom/CreateChatRoomService.kt`
