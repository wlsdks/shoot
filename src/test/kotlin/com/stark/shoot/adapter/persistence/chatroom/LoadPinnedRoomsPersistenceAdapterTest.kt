package com.stark.shoot.adapter.persistence.chatroom

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.out.persistence.postgres.adapter.chatroom.ChatRoomQueryPersistenceAdapter
import com.stark.shoot.adapter.out.persistence.postgres.mapper.ChatRoomMapper
import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomUserRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.UserRepository
import com.stark.shoot.domain.chatroom.type.ChatRoomType
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.util.TestEntityFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.hamcrest.Matchers.hasSize

@DataJpaTest
@Import(LoadPinnedRoomsPersistenceAdapterTest.TestConfig::class, ChatRoomQueryPersistenceAdapter::class, ChatRoomMapper::class)
@DisplayName("고정된 채팅방 조회 어댑터 테스트")
@org.springframework.test.context.ActiveProfiles("test")
class LoadPinnedRoomsPersistenceAdapterTest @Autowired constructor(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatRoomUserRepository: ChatRoomUserRepository,
    private val userRepository: UserRepository,
    private val chatRoomQueryPersistenceAdapter: ChatRoomQueryPersistenceAdapter
) {

    @Test
    @DisplayName("[happy] 사용자가 고정한 채팅방을 조회할 수 있다")
    fun findPinnedRooms() {
        val user1 = userRepository.save(TestEntityFactory.createUser("user1", "u1"))
        val user2 = userRepository.save(TestEntityFactory.createUser("user2", "u2"))

        val roomEntity = chatRoomRepository.save(
            TestEntityFactory.createChatRoomEntity("room", ChatRoomType.GROUP)
        )
        chatRoomUserRepository.save(TestEntityFactory.createChatRoomUser(roomEntity, user1, "m1", isPinned = true))
        chatRoomUserRepository.save(TestEntityFactory.createChatRoomUser(roomEntity, user2, "m1"))

        val rooms = chatRoomQueryPersistenceAdapter.findByUserId(UserId.from(user1.id))
        assertThat(rooms).hasSize(1)
        // DDD 개선: pinnedParticipants는 ChatRoomFavorite Aggregate에서 관리
    }

    @TestConfiguration
    class TestConfig {
        @Bean
        fun objectMapper(): ObjectMapper = ObjectMapper()
    }
}
