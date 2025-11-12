package com.stark.shoot.domain.service.message

import com.stark.shoot.domain.chat.constants.MessageConstants
import com.stark.shoot.domain.chat.exception.MessageException
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.service.MessageEditDomainService
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.message.EditabilityResult
import com.stark.shoot.domain.chat.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

@DisplayName("메시지 편집 도메인 서비스 테스트")
class MessageEditDomainServiceTest {
    private val service = MessageEditDomainService(MessageConstants())

    private fun baseMessage(createdAt: Instant = Instant.now()) = ChatMessage(
        id = MessageId.from("m1"),
        roomId = ChatRoomId.from(1L),
        senderId = UserId.from(2L),
        content = MessageContent("text", MessageType.TEXT),
        status = MessageStatus.SENT,
        createdAt = createdAt
    )

    @Nested
    inner class CanEditMessage {
        @Test
        @DisplayName("[happy] 삭제된 메시지는 수정할 수 없다")
        fun `삭제된 메시지는 수정할 수 없다`() {
            val msg = baseMessage().copy(content = MessageContent("text", MessageType.TEXT, isDeleted = true))
            val result = service.canEditMessage(msg)
            assertThat(result).isEqualTo(EditabilityResult(false, "삭제된 메시지는 수정할 수 없습니다."))
        }

        @Test
        @DisplayName("[happy] 생성 후 24시간이 지나면 수정할 수 없다")
        fun `생성 후 24시간이 지나면 수정할 수 없다`() {
            val msg = baseMessage(Instant.now().minusSeconds(25 * 3600))
            val result = service.canEditMessage(msg)
            assertThat(result.canEdit).isFalse()
        }

        @Test
        @DisplayName("[happy] 텍스트 메시지는 수정 가능하다")
        fun `텍스트 메시지는 수정 가능하다`() {
            val msg = baseMessage()
            val result = service.canEditMessage(msg)
            assertThat(result.canEdit).isTrue()
        }
    }

    @Test
    @DisplayName("[happy] 메시지를 수정할 수 있다")
    fun `메시지를 수정할 수 있다`() {
        val msg = baseMessage()
        val edited = service.editMessage(msg, "new")
        assertThat(edited.content.text).isEqualTo("new")
        assertThat(edited.content.isEdited).isTrue()
    }

    @Test
    @DisplayName("[bad] 편집 불가능한 메시지를 수정하면 예외가 발생한다")
    fun `편집 불가능한 메시지를 수정하면 예외가 발생한다`() {
        val msg = baseMessage().copy(content = MessageContent("text", MessageType.FILE))
        assertThrows<MessageException.NotEditable> { service.editMessage(msg, "x") }
    }

}
