package com.stark.shoot.adapter.persistence.chatroom

import com.stark.shoot.adapter.out.persistence.postgres.adapter.chatroom.ReadStatusPersistenceAdapter
import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomUserRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.UserRepository
import com.stark.shoot.util.TestEntityFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import

@DataJpaTest
@Import(ReadStatusPersistenceAdapter::class)
class ReadStatusPersistenceAdapterTest @Autowired constructor(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatRoomUserRepository: ChatRoomUserRepository,
    private val userRepository: UserRepository,
    private val readStatusPersistenceAdapter: ReadStatusPersistenceAdapter
) {

    @Test
    @DisplayName("마지막 읽은 메시지 ID를 업데이트할 수 있다")
    fun updateLastReadMessageId() {
        val user = userRepository.save(TestEntityFactory.createUser("user", "u1"))
        val room = chatRoomRepository.save(TestEntityFactory.createChatRoomEntity("room"))
        val cru = chatRoomUserRepository.save(TestEntityFactory.createChatRoomUser(room, user))

        readStatusPersistenceAdapter.updateLastReadMessageId(room.id, user.id, "m1")
        val updated = chatRoomUserRepository.findById(cru.id).get()
        assertThat(updated.lastReadMessageId).isEqualTo("m1")
    }
}
