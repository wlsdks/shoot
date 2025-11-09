package com.stark.shoot.adapter.persistence.chatroom

import com.stark.shoot.adapter.out.persistence.postgres.adapter.chatroom.ChatRoomCommandPersistenceAdapter
import com.stark.shoot.adapter.out.persistence.postgres.mapper.ChatRoomMapper
import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomUserRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.UserRepository
import com.stark.shoot.domain.chatroom.type.ChatRoomType
import com.stark.shoot.util.TestEntityFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.hamcrest.Matchers.hasSize

@DataJpaTest
@Import(ChatRoomCommandPersistenceAdapter::class, ChatRoomMapper::class)
@DisplayName("채팅방 저장 어댑터 테스트")
@org.springframework.test.context.ActiveProfiles("test")
class SaveChatRoomPersistenceAdapterTest @Autowired constructor(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatRoomUserRepository: ChatRoomUserRepository,
    private val userRepository: UserRepository,
    private val chatRoomCommandPersistenceAdapter: ChatRoomCommandPersistenceAdapter
) {

    @Test
    @DisplayName("[happy] 새로운 채팅방을 저장하면 참여자 정보까지 함께 저장된다")
    fun saveNewChatRoom() {
        val user1 = userRepository.save(TestEntityFactory.createUser("user1", "u1"))
        val user2 = userRepository.save(TestEntityFactory.createUser("user2", "u2"))

        val domainRoom = TestEntityFactory.createChatRoomDomain(
            participants = mutableSetOf(user1.id, user2.id),
            pinned = mutableSetOf(user1.id),
            type = ChatRoomType.GROUP,
            title = "test"
        )

        val saved = chatRoomCommandPersistenceAdapter.save(domainRoom)

        assertThat(saved.id).isNotNull
        assertThat(chatRoomRepository.count()).isEqualTo(1)
        val participants = chatRoomUserRepository.findByChatRoomId(saved.id!!.value)
        assertThat(participants).hasSize(2)
        val pinned = participants.first { it.user.id == user1.id }
        assertThat(pinned.isPinned).isTrue()
    }
}
