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
        val chatRoomUser = chatRoomUserRepository.save(TestEntityFactory.createChatRoomUser(room, user, "m1"))

        readStatusPersistenceAdapter.updateLastReadMessageId(room.id, user.id, "m1")
        val updated = chatRoomUserRepository.findById(chatRoomUser.id).get()
        assertThat(updated.lastReadMessageId).isEqualTo("m1")
    }

    @Test
    @DisplayName("긴 메시지 ID(24자 초과)도 업데이트할 수 있다")
    fun updateLongLastReadMessageId() {
        val user = userRepository.save(TestEntityFactory.createUser("user", "u1"))
        val room = chatRoomRepository.save(TestEntityFactory.createChatRoomEntity("room"))
        // 초기 짧은 메시지 ID 설정
        val initialMessageId = "short_id"
        val chatRoomUser = chatRoomUserRepository.save(TestEntityFactory.createChatRoomUser(room, user, initialMessageId))

        // 24자를 초과하는 긴 메시지 ID
        val longMessageId = "abcdef1234567890abcdef1234567890"  // 32자

        readStatusPersistenceAdapter.updateLastReadMessageId(room.id, user.id, longMessageId)

        // 직접 쿼리를 통해 현재 값을 확인
        val currentMessageId = chatRoomUserRepository.findLastReadMessageId(room.id, user.id)
        assertThat(currentMessageId).isEqualTo(longMessageId)
    }

}
