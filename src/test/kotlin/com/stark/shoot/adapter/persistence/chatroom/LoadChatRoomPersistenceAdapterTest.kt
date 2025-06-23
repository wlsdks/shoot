package com.stark.shoot.adapter.persistence.chatroom

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

@DataJpaTest
@Import(LoadChatRoomPersistenceAdapter::class, ChatRoomMapper::class)
class LoadChatRoomPersistenceAdapterTest @Autowired constructor(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatRoomUserRepository: ChatRoomUserRepository,
    private val userRepository: UserRepository,
    private val loadChatRoomPersistenceAdapter: LoadChatRoomPersistenceAdapter
) {

    @Test
    @DisplayName("ID로 채팅방을 조회할 수 있다")
    fun findById() {
        val user1 = userRepository.save(TestEntityFactory.createUser("user1", "u1"))
        val user2 = userRepository.save(TestEntityFactory.createUser("user2", "u2"))
        val roomEntity = chatRoomRepository.save(
            TestEntityFactory.createChatRoomEntity("room", ChatRoomType.GROUP)
        )
        chatRoomUserRepository.save(TestEntityFactory.createChatRoomUser(roomEntity, user1, "m1"))
        chatRoomUserRepository.save(TestEntityFactory.createChatRoomUser(roomEntity, user2, "m1", isPinned = true))

        val room = loadChatRoomPersistenceAdapter.findById(roomEntity.id)
        assertThat(room).isNotNull
        assertThat(room!!.participants).containsExactlyInAnyOrder(user1.id, user2.id)
        assertThat(room.pinnedParticipants).contains(user2.id)
    }

    @Test
    @DisplayName("참여자 ID로 채팅방을 조회할 수 있다")
    fun findByParticipantId() {
        val user1 = userRepository.save(TestEntityFactory.createUser("user1", "u1"))
        val user2 = userRepository.save(TestEntityFactory.createUser("user2", "u2"))
        val roomEntity = chatRoomRepository.save(
            TestEntityFactory.createChatRoomEntity("room", ChatRoomType.GROUP)
        )
        chatRoomUserRepository.save(TestEntityFactory.createChatRoomUser(roomEntity, user1, "m1"))
        chatRoomUserRepository.save(TestEntityFactory.createChatRoomUser(roomEntity, user2, "m1"))

        val rooms = loadChatRoomPersistenceAdapter.findByParticipantId(user1.id)
        assertThat(rooms).hasSize(1)
        assertThat(rooms[0].participants).contains(user1.id, user2.id)
    }
}
