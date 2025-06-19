package com.stark.shoot.domain.service.chatroom

import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.domain.chat.room.ChatRoomType
import com.stark.shoot.domain.service.chatroom.ChatRoomParticipantDomainService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("채팅방 참여자 도메인 서비스 테스트")
class ChatRoomParticipantDomainServiceTest {
    private val service = ChatRoomParticipantDomainService()

    @Test
    fun `참여자 변경 정보를 적용할 수 있다`() {
        val room = ChatRoom(id = 1L, title = "room", type = ChatRoomType.GROUP, participants = mutableSetOf(1L, 2L))
        val changes = ChatRoom.ParticipantChanges(
            participantsToAdd = setOf(3L),
            participantsToRemove = setOf(2L),
            pinnedStatusChanges = mapOf(1L to true)
        )

        val updated = service.applyChanges(room, changes) { 0 }

        assertThat(updated.participants).containsExactlyInAnyOrder(1L, 3L)
        assertThat(updated.pinnedParticipants).containsExactly(1L)
    }
}
