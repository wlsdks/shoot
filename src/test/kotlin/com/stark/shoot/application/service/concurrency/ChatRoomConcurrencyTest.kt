package com.stark.shoot.application.service.concurrency

import com.stark.shoot.application.port.`in`.chatroom.CreateChatRoomUseCase
import com.stark.shoot.application.port.`in`.chatroom.command.CreateDirectChatCommand
import com.stark.shoot.application.port.out.user.UserCommandPort
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.domain.user.User
import com.stark.shoot.domain.user.vo.Nickname
import com.stark.shoot.domain.user.vo.Username
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

/**
 * 채팅방 생성 동시성 시나리오 통합 테스트
 *
 * 동일한 사용자 조합으로 1:1 채팅방을 동시에 생성할 때
 * 중복 생성을 방지하고 하나의 채팅방만 생성되는지 검증합니다.
 *
 * Note: 현재 Spring Context 로딩 복잡도로 인해 비활성화
 * TODO: 단순화된 통합 테스트 또는 단위 테스트로 재작성 필요
 */
@Disabled("Spring Context loading complexity - needs refactoring")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = [
        "spring.data.mongodb.auto-index-creation=false",
        "security.enabled=false",
        "websocket.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration,org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration,org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration,org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration"
    ]
)
@ActiveProfiles("test")
@Transactional
@org.springframework.context.annotation.Import(com.stark.shoot.config.TestMongoConfiguration::class)
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
class ChatRoomConcurrencyTest {

    @Autowired
    private lateinit var createChatRoomUseCase: CreateChatRoomUseCase

    @Autowired
    private lateinit var userCommandPort: UserCommandPort

    private lateinit var userA: User
    private lateinit var userB: User

    @BeforeEach
    fun setUp() {
        // 테스트 사용자 생성 및 저장
        userA = userCommandPort.createUser(createUser("userA", "UserA"))
        userB = userCommandPort.createUser(createUser("userB", "UserB"))
    }

    @Test
    @DisplayName("시나리오 1: 동일한 사용자 조합으로 1:1 채팅방을 동시에 생성 시 하나만 생성됨")
    fun `concurrent creation of same direct chat room`() {
        // Given
        val executor = ConcurrentTestExecutor(threadCount = 3)

        // When: A-B 채팅방을 3번 동시에 생성 시도
        val results = executor.executeParallel(3) {
            createChatRoomUseCase.createDirectChat(
                CreateDirectChatCommand(
                    userId = userA.id!!,
                    friendId = userB.id!!
                )
            )
        }

        // Then: 하나만 성공하고 나머지는 실패하거나, 모두 같은 채팅방 ID 반환
        val successResults = results.successes()

        if (successResults.isNotEmpty()) {
            // 성공한 모든 결과가 같은 채팅방 ID를 가져야 함 (중복 방지)
            val roomIds = successResults.map { it.roomId }.toSet()
            assertThat(roomIds).hasSize(1)
        }
    }

    @Test
    @DisplayName("시나리오 2: A→B와 B→A 방향으로 동시 1:1 채팅방 생성 시 하나의 채팅방만 생성됨")
    fun `concurrent creation of direct chat from both directions`() {
        // Given
        val executor = ConcurrentTestExecutor(threadCount = 2)

        // When: A→B와 B→A 채팅방을 동시에 생성
        val results = executor.executeAll(
            {
                createChatRoomUseCase.createDirectChat(
                    CreateDirectChatCommand(
                        userId = userA.id!!,
                        friendId = userB.id!!
                    )
                )
            },
            {
                createChatRoomUseCase.createDirectChat(
                    CreateDirectChatCommand(
                        userId = userB.id!!,
                        friendId = userA.id!!
                    )
                )
            }
        )

        // Then: 모두 같은 채팅방 ID를 반환해야 함
        val successResults = results.successes()
        assertThat(successResults).isNotEmpty

        val roomIds = successResults.map { it.roomId }.toSet()
        assertThat(roomIds).hasSize(1) // 하나의 채팅방만 존재
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
