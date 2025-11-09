package com.stark.shoot.domain.service.chatroom

import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.service.ChatRoomMetadataDomainService
import com.stark.shoot.domain.chatroom.type.ChatRoomType
import com.stark.shoot.domain.chatroom.vo.ChatRoomId as ChatRoomIdService
import com.stark.shoot.domain.chatroom.vo.MessageId as MessageIdService
import com.stark.shoot.domain.chatroom.vo.ChatRoomTitle
import com.stark.shoot.domain.shared.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("채팅방 메타데이터 도메인 서비스 테스트")
class ChatRoomMetadataDomainServiceTest {
    private val service = ChatRoomMetadataDomainService()

    @Test
    @DisplayName("[happy] 새 메시지로 채팅방 메타데이터를 업데이트할 수 있다")
    fun `새 메시지로 채팅방 메타데이터를 업데이트할 수 있다`() {
        val room = ChatRoom(
            id = ChatRoomIdService.from(1L),
            title = ChatRoomTitle.from("room"),
            type = ChatRoomType.GROUP,
            participants = mutableSetOf(UserId.from(1L))
        )
        val originalLastActiveAt = room.lastActiveAt
        val messageId = MessageIdService.from("m1")
        val createdAt = Instant.now()

        val updated = service.updateChatRoomWithNewMessage(room, messageId, createdAt)

        assertThat(updated.lastMessageId?.value).isEqualTo("m1")
        assertThat(updated.lastActiveAt).isAfter(originalLastActiveAt)
    }

}
