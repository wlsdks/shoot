package com.stark.shoot.domain.service.chatroom

import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.service.ChatRoomParticipantDomainService
import com.stark.shoot.domain.chatroom.type.ChatRoomType
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.chatroom.vo.ChatRoomTitle
import com.stark.shoot.domain.user.vo.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("채팅방 참여자 도메인 서비스 테스트")
class ChatRoomParticipantDomainServiceTest {
    private val service = ChatRoomParticipantDomainService()

    @Test
    fun `참여자 변경 정보를 적용할 수 있다`() {
        val room = ChatRoom(
            id = ChatRoomId.from(1L),
            title = ChatRoomTitle.from("room"),
            type = ChatRoomType.GROUP,
            participants = mutableSetOf(UserId.from(1L), UserId.from(2L)),
        )
        val changes = ChatRoom.ParticipantChanges(
            participantsToAdd = setOf(UserId.from(3L)),
            participantsToRemove = setOf(UserId.from(2L)),
            pinnedStatusChanges = mapOf(UserId.from(1L) to true)
        )

        val updated = service.applyChanges(room, changes) { 0 }

        assertThat(updated.participants).containsExactlyInAnyOrder(UserId.from(1L), UserId.from(3L))
        assertThat(updated.pinnedParticipants).containsExactly(UserId.from(1L))
    }

    @Test
    fun `참여자를 제거하면 삭제 여부를 판단할 수 있다`() {
        val room = ChatRoom(
            id = ChatRoomId.from(1L),
            title = ChatRoomTitle.from("room"),
            type = ChatRoomType.GROUP,
            participants = mutableSetOf(UserId.from(1L), UserId.from(2L))
        )

        val result = service.removeParticipant(room, UserId.from(2L))

        assertThat(result.chatRoom.participants).containsExactly(UserId.from(1L))
        assertThat(result.shouldDeleteRoom).isFalse()
    }

    @Test
    fun `마지막 참여자를 제거하면 방이 삭제 대상이 된다`() {
        val room = ChatRoom(
            id = ChatRoomId.from(1L),
            title = ChatRoomTitle.from("room"),
            type = ChatRoomType.GROUP,
            participants = mutableSetOf(UserId.from(1L))
        )

        val result = service.removeParticipant(room, UserId.from(1L))

        assertThat(result.chatRoom.participants).isEmpty()
        assertThat(result.shouldDeleteRoom).isTrue()
    }

}
