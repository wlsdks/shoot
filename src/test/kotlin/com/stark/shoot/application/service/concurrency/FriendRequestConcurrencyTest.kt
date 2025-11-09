package com.stark.shoot.application.service.concurrency

import com.stark.shoot.application.port.`in`.user.friend.FriendReceiveUseCase
import com.stark.shoot.application.port.`in`.user.friend.FriendRequestUseCase
import com.stark.shoot.application.port.`in`.user.friend.command.AcceptFriendRequestCommand
import com.stark.shoot.application.port.`in`.user.friend.command.SendFriendRequestCommand
import com.stark.shoot.application.port.out.user.UserCommandPort
import com.stark.shoot.application.port.out.user.UserQueryPort
import com.stark.shoot.application.port.out.user.friend.relate.FriendshipQueryPort
import com.stark.shoot.domain.social.exception.FriendException
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.domain.user.User
import com.stark.shoot.domain.user.vo.Nickname
import com.stark.shoot.domain.user.vo.Username
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

/**
 * 친구 요청 동시성 시나리오 통합 테스트
 *
 * Race Condition과 OptimisticLockException 처리를 검증합니다.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = [
        "spring.data.mongodb.auto-index-creation=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration,org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration,org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration,org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
    ]
)
@ActiveProfiles("test")
@Transactional
@org.springframework.context.annotation.Import(com.stark.shoot.config.TestMongoConfiguration::class)
class FriendRequestConcurrencyTest {

    @Autowired
    private lateinit var friendRequestUseCase: FriendRequestUseCase

    @Autowired
    private lateinit var friendReceiveUseCase: FriendReceiveUseCase

    @Autowired
    private lateinit var userQueryPort: UserQueryPort

    @Autowired
    private lateinit var userCommandPort: UserCommandPort

    @Autowired
    private lateinit var friendshipQueryPort: FriendshipQueryPort

    private lateinit var userA: User
    private lateinit var userB: User
    private lateinit var userC: User

    @BeforeEach
    fun setUp() {
        // 테스트 사용자 생성 및 저장
        userA = userCommandPort.createUser(createUser("userA", "UserA"))
        userB = userCommandPort.createUser(createUser("userB", "UserB"))
        userC = userCommandPort.createUser(createUser("userC", "UserC"))
    }

    @Test
    @DisplayName("시나리오 1: A→B와 B→A 동시 친구 요청 시 하나만 성공하고 나머지는 FriendRequestAlreadyReceived 예외")
    fun `A to B and B to A concurrent friend requests`() {
        // Given
        val executor = ConcurrentTestExecutor(threadCount = 2)

        // When: A→B와 B→A 동시 요청
        val results = executor.executeAll(
            {
                friendRequestUseCase.sendFriendRequest(
                    SendFriendRequestCommand(userA.id!!, userB.id!!)
                )
            },
            {
                friendRequestUseCase.sendFriendRequest(
                    SendFriendRequestCommand(userB.id!!, userA.id!!)
                )
            }
        )

        // Then: 하나는 성공, 하나는 "이미 요청 받음" 에러
        assertThat(results.successes()).hasSize(1)
        assertThat(results.failures()).hasSize(1)

        // 실패한 요청은 FriendRequestAlreadyReceived 예외여야 함
        val failures = results.failuresOfType<FriendException.FriendRequestAlreadyReceived, Unit>()
        assertThat(failures).hasSize(1)
    }

    @Test
    @DisplayName("시나리오 2: 동일한 친구 요청을 동시에 수락/거절 시 하나만 성공")
    fun `concurrent accept and reject of same friend request`() {
        // Given: A가 B에게 친구 요청을 보냄
        friendRequestUseCase.sendFriendRequest(
            SendFriendRequestCommand(userA.id!!, userB.id!!)
        )

        val executor = ConcurrentTestExecutor(threadCount = 2)

        // When: B가 동시에 수락과 거절 시도
        val results = executor.executeAll(
            {
                friendReceiveUseCase.acceptFriendRequest(
                    AcceptFriendRequestCommand(userB.id!!, userA.id!!)
                )
            },
            {
                friendReceiveUseCase.rejectFriendRequest(
                    com.stark.shoot.application.port.`in`.user.friend.command.RejectFriendRequestCommand(
                        userB.id!!, userA.id!!
                    )
                )
            }
        )

        // Then: 하나만 성공, 하나는 실패
        assertThat(results.successes()).hasSize(1)
        assertThat(results.failures()).hasSize(1)
    }

    @Test
    @DisplayName("시나리오 3: 동일한 친구 요청을 3번 동시에 전송 시 1번만 성공")
    fun `concurrent duplicate friend requests`() {
        // Given
        val executor = ConcurrentTestExecutor(threadCount = 3)

        // When: A→B 요청을 3번 동시에 시도
        val results = executor.executeParallel(3) {
            friendRequestUseCase.sendFriendRequest(
                SendFriendRequestCommand(userA.id!!, userB.id!!)
            )
        }

        // Then: 하나만 성공, 나머지는 중복 요청 에러
        assertThat(results.successes()).hasSize(1)
        assertThat(results.failures()).hasSize(2)

        // 실패한 요청은 FriendRequestAlreadySent 예외여야 함
        val failures = results.failuresOfType<FriendException.FriendRequestAlreadySent, Unit>()
        assertThat(failures).hasSize(2)
    }

    @Test
    @DisplayName("OptimisticLockException 재시도 메커니즘 검증: 친구 요청 수락")
    fun `optimistic lock retry mechanism on friend request acceptance`() {
        // Given: A가 B, C에게 친구 요청
        friendRequestUseCase.sendFriendRequest(
            SendFriendRequestCommand(userA.id!!, userB.id!!)
        )
        friendRequestUseCase.sendFriendRequest(
            SendFriendRequestCommand(userA.id!!, userC.id!!)
        )

        val executor = ConcurrentTestExecutor(threadCount = 2)

        // When: B와 C가 동시에 A의 요청 수락 (OptimisticLockException 발생 가능)
        val results = executor.executeAll(
            {
                friendReceiveUseCase.acceptFriendRequest(
                    AcceptFriendRequestCommand(userB.id!!, userA.id!!)
                )
            },
            {
                friendReceiveUseCase.acceptFriendRequest(
                    AcceptFriendRequestCommand(userC.id!!, userA.id!!)
                )
            }
        )

        // Then: 재시도 메커니즘으로 모두 성공해야 함
        assertThat(results.successes()).hasSize(2)
        assertThat(results.failures()).hasSize(0)

        // A는 B, C와 친구가 되어야 함
        val friendsOfA = friendshipQueryPort.findAllFriendships(userA.id!!)
        assertThat(friendsOfA.map { it.friendId }).containsExactlyInAnyOrder(userB.id!!, userC.id!!)
    }

    /**
     * 테스트용 사용자 생성
     */
    private fun createUser(username: String, nickname: String): User {
        return User.create(
            username = username,
            nickname = nickname,
            rawPassword = "password123!",
            passwordEncoder = { "hashed_$it" }
        )
    }
}
