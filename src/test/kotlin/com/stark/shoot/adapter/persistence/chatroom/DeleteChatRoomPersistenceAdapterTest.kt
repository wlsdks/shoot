package com.stark.shoot.adapter.persistence.chatroom

import com.stark.shoot.adapter.out.persistence.postgres.adapter.chatroom.ChatRoomCommandPersistenceAdapter
import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomRepository
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.util.TestEntityFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import

@DataJpaTest
@Import(ChatRoomCommandPersistenceAdapter::class)
class DeleteChatRoomPersistenceAdapterTest @Autowired constructor(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatRoomCommandPersistenceAdapter: ChatRoomCommandPersistenceAdapter,
) {

    @Test
    @DisplayName("채팅방을 삭제할 수 있다")
    fun deleteChatRoom() {
        val room = chatRoomRepository.save(
            TestEntityFactory.createChatRoomEntity("room")
        )
        val result = chatRoomCommandPersistenceAdapter.deleteById(ChatRoomId.from(room.id))
        assertThat(result).isTrue()
        assertThat(chatRoomRepository.existsById(room.id)).isFalse()
    }

}
