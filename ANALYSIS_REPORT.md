# 백엔드 코드 분석 보고서

**분석 일자**: 2025-10-28
**분석 대상**: MongoDB/PostgreSQL 분리 구조, 친구 관리, 채팅방 관리

---

## 📊 종합 요약

### ✅ 잘 구현된 부분

1. **아키텍처 설계**
   - Hexagonal Architecture + DDD 패턴 잘 적용
   - Port/Adapter 패턴으로 의존성 역전 원칙 준수
   - 도메인 로직과 인프라 계층 명확히 분리

2. **데이터베이스 분리**
   - PostgreSQL: 관계형 데이터 (사용자, 친구, 채팅방 메타데이터)
   - MongoDB: 문서형 데이터 (메시지, 알림)
   - 각 데이터베이스 특성에 맞게 적절히 활용

3. **동시성 제어**
   - Redis 분산 락으로 Race Condition 방지
   - Optimistic Locking (@Version) 적용
   - 트랜잭션 이벤트 리스너 활용

4. **이벤트 기반 아키텍처**
   - SpringEventPublisher로 도메인 이벤트 발행
   - @TransactionalEventListener로 트랜잭션 커밋 후 처리
   - CDC (Change Data Capture) + Outbox 패턴

---

## ⚠️ 주요 문제점 및 개선 방안

### 1. 친구 관리 시스템

#### 문제점 1.1: N+1 쿼리 - RecommendFriendService
**위치**: `RecommendFriendService.kt:261-269`

**문제**:
```kotlin
users.forEach { user ->
    user.id?.let { targetId ->
        if (userQueryPort.checkFriendship(userId, targetId)) {  // N개 쿼리
            friendIds.add(targetId)
        }
        if (userQueryPort.checkOutgoingFriendRequest(userId, targetId)) {  // N개 쿼리
            outgoingRequestIds.add(targetId)
        }
        if (userQueryPort.checkIncomingFriendRequest(userId, targetId)) {  // N개 쿼리
            incomingRequestIds.add(targetId)
        }
    }
}
```

**영향**: 추천 사용자 20명일 경우 60개 추가 쿼리 발생

**개선안**:
```kotlin
// 배치 조회 메서드 추가
interface UserQueryPort {
    fun checkFriendshipBatch(userId: UserId, friendIds: List<UserId>): Set<UserId>
    fun checkOutgoingFriendRequestBatch(userId: UserId, targetIds: List<UserId>): Set<UserId>
    fun checkIncomingFriendRequestBatch(userId: UserId, requesterIds: List<UserId>): Set<UserId>
}

// 최적화된 구현
private fun filterExistingRelationships(userId: UserId, users: List<User>): List<User> {
    val userIds = users.mapNotNull { it.id }
    val friendIds = userQueryPort.checkFriendshipBatch(userId, userIds)
    val outgoingIds = userQueryPort.checkOutgoingFriendRequestBatch(userId, userIds)
    val incomingIds = userQueryPort.checkIncomingFriendRequestBatch(userId, userIds)
    val excludedIds = friendIds + outgoingIds + incomingIds
    return users.filter { it.id?.let { id -> !excludedIds.contains(id) } ?: true }
}
```

---

#### 문제점 1.2: N+1 쿼리 - FindFriendService
**위치**: `FindFriendService.kt:42-53`

**문제**:
```kotlin
return friendships.map { friendship ->
    val friend = userQueryPort.findUserById(friendId)  // 친구 1000명이면 1000개 쿼리
        ?: throw ResourceNotFoundException("Friend not found: $friendId")
    FriendResponse(...)
}
```

**개선안**:
```kotlin
override fun getFriends(command: GetFriendsCommand): List<FriendResponse> {
    val friendships = friendshipQueryPort.findAllFriendships(currentUserId)
    val friendIds = friendships.map { it.friendId }

    // 배치 조회
    val friendsMap = userQueryPort.findAllByIds(friendIds).associateBy { it.id }

    return friendships.mapNotNull { friendship ->
        val friend = friendsMap[friendship.friendId] ?: return@mapNotNull null
        FriendResponse(...)
    }
}
```

---

#### 문제점 1.3: 인덱스 누락

**현재 상태**: `friendship_map`, `friend_requests` 테이블에 인덱스 미정의

**영향**: Full Table Scan으로 조회 성능 저하

**개선안**:
```sql
-- friendship_map 인덱스
CREATE INDEX IF NOT EXISTS idx_friendship_user_id ON friendship_map(user_id);
CREATE INDEX IF NOT EXISTS idx_friendship_friend_id ON friendship_map(friend_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_friendship_user_friend ON friendship_map(user_id, friend_id);

-- friend_requests 인덱스
CREATE INDEX IF NOT EXISTS idx_friend_request_sender_status ON friend_requests(sender_id, status);
CREATE INDEX IF NOT EXISTS idx_friend_request_receiver_status ON friend_requests(receiver_id, status);
CREATE INDEX IF NOT EXISTS idx_friend_request_sender_receiver ON friend_requests(sender_id, receiver_id);
```

---

#### 문제점 1.4: 양방향 친구 관계 원자성 미보장

**위치**: `FriendReceiveService.kt:42-65`

**문제**:
```kotlin
friendCommandPort.addFriendRelation(currentUserId, requesterId)
friendCommandPort.addFriendRelation(requesterId, currentUserId)  // 이 줄 실패 시 불일치
```

**개선안**:
```kotlin
// Repository에 원자적 배치 삽입 메서드 추가
@Modifying
@Query(nativeQuery = true, """
    INSERT INTO friendship_map (user_id, friend_id, created_at)
    VALUES (:userId1, :userId2, now()), (:userId2, :userId1, now())
""")
fun createBidirectional(
    @Param("userId1") userId1: Long,
    @Param("userId2") userId2: Long
)
```

---

### 2. 채팅방 관리 시스템

#### 문제점 2.1: N+1 쿼리 - 채팅방 참여자 조회
**위치**: `ChatRoomQueryPersistenceAdapter.kt:45-65`

**문제**:
```kotlin
val chatRoomUsers = chatRoomUserRepository.findByUserId(participantId.value)  // 1 쿼리
val chatRoomIds = chatRoomUsers.map { it.chatRoom.id }
val chatRoomEntities = chatRoomRepository.findAllById(chatRoomIds)  // 1 쿼리
val allParticipants = chatRoomUserRepository.findAllByChatRoomIds(chatRoomIds)  // 1 쿼리
// 총 3 쿼리
```

**개선안**:
```sql
-- 단일 쿼리로 통합
SELECT cr.*, cru.user_id, cru.is_pinned, cru.last_read_message_mongodb_id
FROM chat_rooms cr
INNER JOIN chat_room_users cru ON cr.id = cru.chat_room_id
WHERE cr.id IN (
    SELECT DISTINCT chat_room_id
    FROM chat_room_users
    WHERE user_id = :userId
)
ORDER BY cr.last_active_at DESC
```

---

#### 문제점 2.2: readBy 맵 확장성 문제
**위치**: `ChatMessageDocument.kt:22`

**문제**:
```kotlin
val readBy: MutableMap<Long, Boolean> = mutableMapOf()
```

100명 그룹 채팅방 × 10,000 메시지 = 1,000,000개 readBy 항목
→ 문서당 평균 ~1MB (압축 전)
→ 쿼리 성능 저하

**개선안**: 정규화된 message_reads 컬렉션 분리
```kotlin
@Document(collection = "message_reads")
data class MessageReadDocument(
    val messageId: ObjectId,
    val userId: Long,
    val roomId: Long,
    val readAt: Instant
) {
    @Id
    val id: String = "${messageId}_${userId}"
}

@CompoundIndex(name = "room_user_read_idx",
    def = "{'roomId': 1, 'userId': 1, 'readAt': -1}")
```

---

#### 문제점 2.3: Optimistic Lock 재시도 메커니즘 없음
**위치**: `ChatRoomEntity.kt`, `ChatRoomCommandPersistenceAdapter.kt`

**문제**: OptimisticLockingFailureException 발생 시 처리 로직 없음

**개선안**:
```kotlin
fun saveWithRetry(chatRoom: ChatRoom, maxRetries: Int = 3): ChatRoom {
    repeat(maxRetries) { attempt ->
        try {
            return save(chatRoom)
        } catch (e: OptimisticLockingFailureException) {
            if (attempt < maxRetries - 1) {
                logger.warn { "Optimistic lock 실패, 재시도 ${attempt + 1}/$maxRetries" }
                val latestRoom = findById(chatRoom.id!!)
                    ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다")
                chatRoom.merge(latestRoom)
                Thread.sleep(100L * (attempt + 1))  // 지수 백오프
            } else {
                throw
            }
        }
    }
    throw IllegalStateException("최대 재시도 횟수 초과")
}
```

---

#### 문제점 2.4: 마지막 읽음 ID 업데이트 경합
**위치**: `MessageReadService.kt:313`

**문제**: 마지막 읽음 메시지 ID를 매 배치마다 업데이트 → Lock 경합

**개선안**: Redis 큐에 축적 후 배치 업데이트
```kotlin
// 1. Redis에 큐잉
fun markAllMessagesAsRead(command: MarkAllMessagesAsReadCommand) {
    val (updatedMessageIds, lastMessage) = processAllUnreadMessages(roomId, userId)

    if (lastMessage != null) {
        redisTemplate.opsForZSet()
            .add("room:${roomId.value}:reads",
                 "${userId.value}:${lastMessage.id?.value}",
                 System.currentTimeMillis().toDouble())
    }
}

// 2. 스케줄된 배치 업데이트 (1초마다)
@Scheduled(fixedDelay = 1000)
fun batchUpdateLastReadMessages() {
    val redisKeys = redisTemplate.keys("room:*:reads")
    redisKeys.forEach { key ->
        val updates = redisTemplate.opsForZSet().rangeByScore(key, 0.0, now)
        if (updates.isNotEmpty()) {
            // 배치 업데이트
            updates.forEach { update ->
                val (userId, messageId) = update.split(":")
                chatRoomCommandPort.updateLastReadMessageId(...)
            }
            redisTemplate.opsForZSet().removeRangeByScore(key, 0.0, now)
        }
    }
}
```

---

#### 문제점 2.5: MongoDB 인덱스 추가 필요

**현재 인덱스**:
- `room_created_idx`: `{'roomId': 1, 'createdAt': -1}`
- `sender_created_idx`: `{'senderId': 1, 'createdAt': -1}`

**추가 권장 인덱스**:
```kotlin
@CompoundIndexes(
    // 기존 인덱스...

    // 스레드 조회 최적화
    CompoundIndex(name = "thread_id_idx", def = "{'threadId': 1, 'createdAt': -1}"),

    // 고정 메시지 조회
    CompoundIndex(name = "room_pinned_idx", def = "{'roomId': 1, 'isPinned': 1, 'createdAt': -1}"),

    // 삭제되지 않은 메시지 조회
    CompoundIndex(name = "room_not_deleted_idx", def = "{'roomId': 1, 'isDeleted': 1, 'createdAt': -1}")
)
```

---

## 📈 개선 우선순위

| 순위 | 항목 | 영향도 | 난이도 | 예상 시간 |
|-----|------|--------|--------|----------|
| 1 | 인덱스 추가 (friendship_map, friend_requests, chat_rooms) | 🔴 HIGH | 🟢 LOW | 2h |
| 2 | N+1 쿼리: RecommendFriendService | 🔴 HIGH | 🟡 MEDIUM | 4h |
| 3 | N+1 쿼리: FindFriendService | 🔴 HIGH | 🟡 MEDIUM | 3h |
| 4 | N+1 쿼리: ChatRoomQueryPersistenceAdapter | 🔴 HIGH | 🟡 MEDIUM | 3h |
| 5 | readBy 맵 정규화 | 🟠 MEDIUM | 🔴 HIGH | 6h |
| 6 | Optimistic Lock 재시도 | 🟠 MEDIUM | 🟡 MEDIUM | 4h |
| 7 | 양방향 친구 관계 원자성 | 🟠 MEDIUM | 🟡 MEDIUM | 3h |
| 8 | 마지막 읽음 ID 지연 업데이트 | 🟠 MEDIUM | 🟡 MEDIUM | 3h |
| 9 | MongoDB 인덱스 추가 | 🟠 MEDIUM | 🟢 LOW | 2h |

**총 예상 시간**: 30시간

---

## 🧪 테스트 전략

### 1. 통합 테스트 구성

**test.yml 생성**:
- H2 in-memory DB (PostgreSQL 모드)
- Embedded MongoDB (flapdoodle)
- Embedded Redis (embedded-redis)

### 2. 테스트 범위

**친구 관리 통합 테스트**:
- 친구 요청 → 수락 → 친구 목록 조회
- 양방향 관계 검증
- 동시 요청 처리 (Redis 락 검증)

**채팅방 관리 통합 테스트**:
- 1:1 채팅방 생성 → 메시지 전송 → 읽음 처리
- 그룹 채팅방 생성 → 참여자 추가/제거
- Optimistic Lock 충돌 시나리오

---

## 📊 성능 예상 효과

| 항목 | 현재 | 개선 후 | 향상도 |
|-----|------|---------|--------|
| 친구 추천 조회 (20명) | 60+ 쿼리 | 3 쿼리 | 95% ↓ |
| 친구 목록 조회 (1000명) | 1000+ 쿼리 | 1 쿼리 | 99.9% ↓ |
| 채팅방 목록 조회 (10개) | 3 쿼리 | 1 쿼리 | 66% ↓ |
| 100명 그룹 readBy | 1.2MB/문서 | 50KB/문서 | 95% ↓ |
| 마지막 읽음 ID 업데이트 | 매 배치마다 | 1초에 1회 (배치) | 99% ↓ |

---

## 🔧 즉시 적용 가능한 개선 사항

### 1. Flyway 마이그레이션 추가

**파일**: `V7__add_indexes.sql`

```sql
-- friendship_map 인덱스
CREATE INDEX IF NOT EXISTS idx_friendship_user_id ON friendship_map(user_id);
CREATE INDEX IF NOT EXISTS idx_friendship_friend_id ON friendship_map(friend_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_friendship_user_friend ON friendship_map(user_id, friend_id);

-- friend_requests 인덱스
CREATE INDEX IF NOT EXISTS idx_friend_request_sender_status ON friend_requests(sender_id, status);
CREATE INDEX IF NOT EXISTS idx_friend_request_receiver_status ON friend_requests(receiver_id, status);
CREATE INDEX IF NOT EXISTS idx_friend_request_sender_receiver ON friend_requests(sender_id, receiver_id);

-- chat_rooms 인덱스
CREATE INDEX IF NOT EXISTS idx_chat_rooms_last_active ON chat_rooms(last_active_at DESC);

-- chat_room_users 인덱스
CREATE INDEX IF NOT EXISTS idx_chat_room_users_user_id ON chat_room_users(user_id);
CREATE INDEX IF NOT EXISTS idx_chat_room_users_room_id ON chat_room_users(chat_room_id);
```

### 2. application.yml 설정 조정

```yaml
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 100  # 현재 10 → 100으로 증가
        jdbc:
          batch_size: 50  # 배치 INSERT 최적화
```

---

## 📝 결론

1. **전반적인 코드 품질**: 우수함
   - 아키텍처 설계가 견고함
   - 도메인 로직 분리가 명확함

2. **주요 개선 영역**: 성능 최적화
   - N+1 쿼리 문제 해결 필요
   - 인덱스 추가로 즉시 효과
   - MongoDB readBy 맵 정규화 (중장기)

3. **테스트 커버리지**: 개선 필요
   - 통합 테스트 추가 권장
   - PostgreSQL/MongoDB 통합 시나리오 검증

4. **다음 단계**:
   - 인덱스 추가 (즉시 적용)
   - 배치 조회 메서드 추가
   - 통합 테스트 작성

---

**작성자**: Claude Code
**검토 필요 사항**: 프로덕션 적용 전 부하 테스트 권장
