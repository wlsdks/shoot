package com.stark.shoot.domain.chat.message

import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.ChatMessageMetadata
import com.stark.shoot.domain.chat.reaction.type.ReactionType
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("채팅 메시지 도메인 테스트")
class ChatMessageTest {

    @Nested
    @DisplayName("메시지 생성 시")
    inner class CreateMessage {

        @Test
        @DisplayName("주어진 정보로 메시지를 생성할 수 있다")
        fun `메시지를 생성할 수 있다`() {
            // given
            val roomId = ChatRoomId.from(1L)
            val senderId = UserId.from(2L)
            val text = "안녕하세요"
            val type = MessageType.TEXT
            val tempId = "temp-123"

            // when
            val message = ChatMessage.create(
                roomId = roomId,
                senderId = senderId,
                text = text,
                type = type,
                tempId = tempId
            )

            // then
            assertThat(message.roomId).isEqualTo(roomId)
            assertThat(message.senderId).isEqualTo(senderId)
            assertThat(message.content.text).isEqualTo(text)
            assertThat(message.content.type).isEqualTo(type)
            assertThat(message.status).isEqualTo(MessageStatus.SENDING)
            assertThat(message.metadata.tempId).isEqualTo(tempId)
            assertThat(message.createdAt).isNotNull()
        }
    }

    @Nested
    @DisplayName("메시지 읽음 처리 시")
    inner class MarkAsRead {

        @Test
        @DisplayName("사용자가 메시지를 읽음 처리할 수 있다")
        fun `사용자가 메시지를 읽음 처리할 수 있다`() {
            // given
            val message = ChatMessage.create(
                roomId = ChatRoomId.from(1L),
                senderId = UserId.from(2L),
                text = "안녕하세요",
                type = MessageType.TEXT
            )
            val userId = UserId.from(3L)

            // when
            val updatedMessage = message.markAsRead(userId)

            // then
            assertThat(updatedMessage.readBy).containsKey(userId)
            assertThat(updatedMessage.readBy[userId]).isTrue()
            assertThat(updatedMessage.metadata.readAt).isNotNull()
        }
    }

    @Nested
    @DisplayName("메시지 고정 상태 변경 시")
    inner class UpdatePinStatus {

        @Test
        @DisplayName("메시지를 고정할 수 있다")
        fun `메시지를 고정할 수 있다`() {
            // given
            val message = ChatMessage.create(
                roomId = ChatRoomId.from(1L),
                senderId = UserId.from(2L),
                text = "안녕하세요",
                type = MessageType.TEXT
            )
            val userId = UserId.from(3L)

            // when
            val pinnedMessage = message.updatePinStatus(true, userId)

            // then
            assertThat(pinnedMessage.isPinned).isTrue()
            assertThat(pinnedMessage.pinnedBy).isEqualTo(userId)
            assertThat(pinnedMessage.pinnedAt).isNotNull()
        }

        @Test
        @DisplayName("고정된 메시지를 해제할 수 있다")
        fun `고정된 메시지를 해제할 수 있다`() {
            // given
            val message = ChatMessage.create(
                roomId = ChatRoomId.from(1L),
                senderId = UserId.from(2L),
                text = "안녕하세요",
                type = MessageType.TEXT
            )
            val userId = UserId.from(3L)
            val pinnedMessage = message.updatePinStatus(true, userId)

            // when
            val unpinnedMessage = pinnedMessage.updatePinStatus(false)

            // then
            assertThat(unpinnedMessage.isPinned).isFalse()
            assertThat(unpinnedMessage.pinnedBy).isNull()
            assertThat(unpinnedMessage.pinnedAt).isNull()
        }
    }

    @Nested
    @DisplayName("채팅방에서 메시지 고정 시")
    inner class PinMessageInRoom {

        @Test
        @DisplayName("채팅방에 고정된 메시지가 없으면 메시지를 고정할 수 있다")
        fun `채팅방에 고정된 메시지가 없으면 메시지를 고정할 수 있다`() {
            // given
            val message = ChatMessage.create(
                roomId = ChatRoomId.from(1L),
                senderId = UserId.from(2L),
                text = "안녕하세요",
                type = MessageType.TEXT
            )
            val userId = UserId.from(3L)

            // when
            val result = message.pinMessageInRoom(userId, null)

            // then
            assertThat(result.pinnedMessage.isPinned).isTrue()
            assertThat(result.pinnedMessage.pinnedBy).isEqualTo(userId)
            assertThat(result.pinnedMessage.pinnedAt).isNotNull()
            assertThat(result.unpinnedMessage).isNull()
        }

        @Test
        @DisplayName("채팅방에 이미 고정된 메시지가 있으면 기존 메시지를 해제하고 새 메시지를 고정할 수 있다")
        fun `채팅방에 이미 고정된 메시지가 있으면 기존 메시지를 해제하고 새 메시지를 고정할 수 있다`() {
            // given
            val existingMessage = ChatMessage.create(
                roomId = ChatRoomId.from(1L),
                senderId = UserId.from(2L),
                text = "기존 메시지",
                type = MessageType.TEXT
            )
            val newMessage = ChatMessage.create(
                roomId = ChatRoomId.from(1L),
                senderId = UserId.from(2L),
                text = "새 메시지",
                type = MessageType.TEXT
            )
            val userId = UserId.from(3L)
            val pinnedExistingMessage = existingMessage.updatePinStatus(true, userId)

            // when
            val result = newMessage.pinMessageInRoom(userId, pinnedExistingMessage)

            // then
            assertThat(result.pinnedMessage.isPinned).isTrue()
            assertThat(result.pinnedMessage.pinnedBy).isEqualTo(userId)
            assertThat(result.pinnedMessage.pinnedAt).isNotNull()
            assertThat(result.unpinnedMessage).isNotNull()
            assertThat(result.unpinnedMessage!!.isPinned).isFalse()
            assertThat(result.unpinnedMessage!!.pinnedBy).isNull()
            assertThat(result.unpinnedMessage!!.pinnedAt).isNull()
        }

        @Test
        @DisplayName("이미 고정된 메시지를 다시 고정하면 변경 없이 그대로 반환한다")
        fun `이미 고정된 메시지를 다시 고정하면 변경 없이 그대로 반환한다`() {
            // given
            val message = ChatMessage.create(
                roomId = ChatRoomId.from(1L),
                senderId = UserId.from(2L),
                text = "안녕하세요",
                type = MessageType.TEXT
            )
            val userId = UserId.from(3L)
            val pinnedMessage = message.updatePinStatus(true, userId)

            // when
            val result = pinnedMessage.pinMessageInRoom(userId, null)

            // then
            assertThat(result.pinnedMessage).isEqualTo(pinnedMessage)
            assertThat(result.unpinnedMessage).isNull()
        }
    }

    @Nested
    @DisplayName("메시지 내용 수정 시")
    inner class EditMessage {

        @Test
        @DisplayName("텍스트 타입 메시지의 내용을 수정할 수 있다")
        fun `텍스트 타입 메시지의 내용을 수정할 수 있다`() {
            // given
            val message = ChatMessage.create(
                roomId = ChatRoomId.from(1L),
                senderId = UserId.from(2L),
                text = "안녕하세요",
                type = MessageType.TEXT
            )
            val newContent = "수정된 내용입니다"

            // when
            val editedMessage = message.editMessage(newContent)

            // then
            assertThat(editedMessage.content.text).isEqualTo(newContent)
            assertThat(editedMessage.content.isEdited).isTrue()
            assertThat(editedMessage.updatedAt).isNotNull()
        }

        @Test
        @DisplayName("빈 내용으로 수정하려고 하면 예외가 발생한다")
        fun `빈 내용으로 수정하려고 하면 예외가 발생한다`() {
            // given
            val message = ChatMessage.create(
                roomId = ChatRoomId.from(1L),
                senderId = UserId.from(2L),
                text = "안녕하세요",
                type = MessageType.TEXT
            )
            val emptyContent = "   "

            // when & then
            assertThatThrownBy { message.editMessage(emptyContent) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("메시지 내용은 비어있을 수 없습니다")
        }

        @Test
        @DisplayName("삭제된 메시지를 수정하려고 하면 예외가 발생한다")
        fun `삭제된 메시지를 수정하려고 하면 예외가 발생한다`() {
            // given
            val message = ChatMessage.create(
                roomId = ChatRoomId.from(1L),
                senderId = UserId.from(2L),
                text = "안녕하세요",
                type = MessageType.TEXT
            )
            val deletedMessage = message.markAsDeleted()
            val newContent = "수정된 내용입니다"

            // when & then
            assertThatThrownBy { deletedMessage.editMessage(newContent) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("삭제된 메시지는 수정할 수 없습니다")
        }

        @Test
        @DisplayName("텍스트 타입이 아닌 메시지를 수정하려고 하면 예외가 발생한다")
        fun `텍스트 타입이 아닌 메시지를 수정하려고 하면 예외가 발생한다`() {
            // given
            val message = ChatMessage.create(
                roomId = ChatRoomId.from(1L),
                senderId = UserId.from(2L),
                text = "파일 URL",
                type = MessageType.FILE
            )
            val newContent = "수정된 내용입니다"

            // when & then
            assertThatThrownBy { message.editMessage(newContent) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("텍스트 타입의 메시지만 수정할 수 있습니다")
        }
    }

    @Nested
    @DisplayName("메시지 삭제 시")
    inner class MarkAsDeleted {

        @Test
        @DisplayName("메시지를 삭제 상태로 변경할 수 있다")
        fun `메시지를 삭제 상태로 변경할 수 있다`() {
            // given
            val message = ChatMessage.create(
                roomId = ChatRoomId.from(1L),
                senderId = UserId.from(2L),
                text = "안녕하세요",
                type = MessageType.TEXT
            )

            // when
            val deletedMessage = message.markAsDeleted()

            // then
            assertThat(deletedMessage.content.isDeleted).isTrue()
            assertThat(deletedMessage.updatedAt).isNotNull()
        }
    }

    @Nested
    @DisplayName("메시지 반응 토글 시")
    inner class ToggleReaction {

        @Test
        @DisplayName("메시지에 반응을 추가할 수 있다")
        fun `메시지에 반응을 추가할 수 있다`() {
            // given
            val message = ChatMessage.create(
                roomId = ChatRoomId.from(1L),
                senderId = UserId.from(2L),
                text = "안녕하세요",
                type = MessageType.TEXT
            )
            val userId = UserId.from(3L)
            val reactionType = ReactionType.LIKE

            // when
            val result = message.toggleReaction(userId, reactionType)

            // then
            assertThat(result.isAdded).isTrue()
            assertThat(result.previousReactionType).isNull()
            assertThat(result.isReplacement).isFalse()
            assertThat(result.message.messageReactions.reactions).containsKey(reactionType.code)
            assertThat(result.message.messageReactions.reactions[reactionType.code]).contains(userId.value)
        }

        @Test
        @DisplayName("이미 추가한 반응과 같은 반응을 선택하면 제거할 수 있다")
        fun `이미 추가한 반응과 같은 반응을 선택하면 제거할 수 있다`() {
            // given
            val message = ChatMessage.create(
                roomId = ChatRoomId.from(1L),
                senderId = UserId.from(2L),
                text = "안녕하세요",
                type = MessageType.TEXT
            )
            val userId = UserId.from(3L)
            val reactionType = ReactionType.LIKE
            val messageWithReaction = message.toggleReaction(userId, reactionType).message

            // when
            val result = messageWithReaction.toggleReaction(userId, reactionType)

            // then
            assertThat(result.isAdded).isFalse()
            assertThat(result.previousReactionType).isNull()
            assertThat(result.isReplacement).isFalse()
            assertThat(result.message.messageReactions.reactions).doesNotContainKey(reactionType.code)
        }

        @Test
        @DisplayName("이미 추가한 반응과 다른 반응을 선택하면 기존 반응을 제거하고 새 반응을 추가할 수 있다")
        fun `이미 추가한 반응과 다른 반응을 선택하면 기존 반응을 제거하고 새 반응을 추가할 수 있다`() {
            // given
            val message = ChatMessage.create(
                roomId = ChatRoomId.from(1L),
                senderId = UserId.from(2L),
                text = "안녕하세요",
                type = MessageType.TEXT
            )
            val userId = UserId.from(3L)
            val oldReactionType = ReactionType.LIKE
            val newReactionType = ReactionType.CURIOUS
            val messageWithReaction = message.toggleReaction(userId, oldReactionType).message

            // when
            val result = messageWithReaction.toggleReaction(userId, newReactionType)

            // then
            assertThat(result.isAdded).isTrue()
            assertThat(result.previousReactionType).isEqualTo(oldReactionType.code)
            assertThat(result.isReplacement).isTrue()
            assertThat(result.message.messageReactions.reactions).doesNotContainKey(oldReactionType.code)
            assertThat(result.message.messageReactions.reactions).containsKey(newReactionType.code)
            assertThat(result.message.messageReactions.reactions[newReactionType.code]).contains(userId.value)
        }
    }

    @Nested
    @DisplayName("URL 미리보기 처리 시")
    inner class UrlPreview {

        @Test
        @DisplayName("URL 미리보기 정보를 설정할 수 있다")
        fun `URL 미리보기 정보를 설정할 수 있다`() {
            // given
            val message = ChatMessage.create(
                roomId = ChatRoomId.from(1L),
                senderId = UserId.from(2L),
                text = "https://example.com",
                type = MessageType.TEXT
            )
            val urlPreview = ChatMessageMetadata.UrlPreview(
                url = "https://example.com",
                title = "Example Domain",
                description = "This domain is for use in examples",
                imageUrl = "https://example.com/image.jpg",
                siteName = "Example"
            )

            // when
            val updatedMessage = message.setUrlPreview(urlPreview)

            // then
            assertThat(updatedMessage.metadata.urlPreview).isEqualTo(urlPreview)
            assertThat(updatedMessage.metadata.needsUrlPreview).isFalse()
            assertThat(updatedMessage.updatedAt).isNotNull()
        }

        @Test
        @DisplayName("URL 미리보기가 필요함을 표시할 수 있다")
        fun `URL 미리보기가 필요함을 표시할 수 있다`() {
            // given
            val message = ChatMessage.create(
                roomId = ChatRoomId.from(1L),
                senderId = UserId.from(2L),
                text = "https://example.com",
                type = MessageType.TEXT
            )
            val url = "https://example.com"

            // when
            val updatedMessage = message.markNeedsUrlPreview(url)

            // then
            assertThat(updatedMessage.metadata.needsUrlPreview).isTrue()
            assertThat(updatedMessage.metadata.previewUrl).isEqualTo(url)
            assertThat(updatedMessage.updatedAt).isNotNull()
        }
    }

    @Nested
    @DisplayName("메시지 만료 설정 시")
    inner class Expiration {

        @Test
        @DisplayName("만료 시간이 지나면 만료로 판단한다")
        fun `만료 시간이 지나면 만료로 판단한다`() {
            val expireAt = Instant.now().minusSeconds(10)
            val message = ChatMessage.create(
                roomId = ChatRoomId.from(1L),
                senderId = UserId.from(2L),
                text = "hi",
                expiresAt = expireAt
            )
            assertThat(message.isExpired()).isTrue()
        }

        @Test
        @DisplayName("미래의 만료 시간은 만료되지 않은 것으로 판단한다")
        fun `미래의 만료 시간은 만료되지 않은 것으로 판단한다`() {
            val expireAt = Instant.now().plusSeconds(60)
            val message = ChatMessage.create(
                roomId = ChatRoomId.from(1L),
                senderId = UserId.from(2L),
                text = "hi",
                expiresAt = expireAt
            )
            assertThat(message.isExpired()).isFalse()
        }
    }
}
