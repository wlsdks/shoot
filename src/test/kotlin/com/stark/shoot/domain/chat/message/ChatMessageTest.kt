package com.stark.shoot.domain.chat.message

import com.stark.shoot.domain.chat.exception.MessageException
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.ChatMessageMetadata
import com.stark.shoot.domain.chat.reaction.type.ReactionType
import com.stark.shoot.domain.chat.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

@DisplayName("채팅 메시지 도메인 테스트")
class ChatMessageTest {

    @Nested
    @DisplayName("메시지 생성 시")
    inner class CreateMessage {

        @Test
        @DisplayName("[happy] 주어진 정보로 메시지를 생성할 수 있다")
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
            assertThat(message.status).isEqualTo(MessageStatus.SENT)
            assertThat(message.metadata.tempId).isEqualTo(tempId)
            assertThat(message.createdAt).isNotNull()
        }
    }

    // TODO: 메시지 읽음 표시 기능은 별도의 MessageReadReceipt Aggregate로 분리되었습니다.
    // 아래 테스트는 MessageReadReceipt Aggregate 테스트로 재작성 필요
    /*
    @Nested
    @DisplayName("메시지 읽음 처리 시")
    inner class MarkAsRead {

        @Test
        @DisplayName("[happy] 사용자가 메시지를 읽음 처리할 수 있다")
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
            message.markAsRead(userId)

            // then
            assertThat(message.readBy).containsKey(userId)
            assertThat(message.readBy[userId]).isTrue()
            assertThat(message.metadata.readAt).isNotNull()
        }
    }
    */

    // TODO: 메시지 고정 기능은 별도의 MessagePin Aggregate로 분리되었습니다.
    // 아래 테스트들은 MessagePin Aggregate 테스트로 재작성 필요
    /*
    @Nested
    @DisplayName("메시지 고정 상태 변경 시")
    inner class UpdatePinStatus {

        @Test
        @DisplayName("[happy] 메시지를 고정할 수 있다")
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
            message.updatePinStatus(true, userId)

            // then
            assertThat(message.isPinned).isTrue()
            assertThat(message.pinnedBy).isEqualTo(userId)
            assertThat(message.pinnedAt).isNotNull()
        }

        @Test
        @DisplayName("[happy] 고정된 메시지를 해제할 수 있다")
        fun `고정된 메시지를 해제할 수 있다`() {
            // given
            val message = ChatMessage.create(
                roomId = ChatRoomId.from(1L),
                senderId = UserId.from(2L),
                text = "안녕하세요",
                type = MessageType.TEXT
            )
            val userId = UserId.from(3L)
            message.updatePinStatus(true, userId)

            // when
            message.updatePinStatus(false)

            // then
            assertThat(message.isPinned).isFalse()
            assertThat(message.pinnedBy).isNull()
            assertThat(message.pinnedAt).isNull()
        }
    }

    @Nested
    @DisplayName("채팅방에서 메시지 고정 시")
    inner class PinMessageInRoom {

        @Test
        @DisplayName("[happy] 채팅방에 고정된 메시지가 없으면 메시지를 고정할 수 있다")
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
            val result = message.pinMessageInRoom(userId, 0, 5)

            // then
            assertThat(result.pinnedMessage.isPinned).isTrue()
            assertThat(result.pinnedMessage.pinnedBy).isEqualTo(userId)
            assertThat(result.pinnedMessage.pinnedAt).isNotNull()
            assertThat(result.unpinnedMessage).isNull()
        }

        @Test
        @DisplayName("[happy] 채팅방에 이미 고정된 메시지가 있으면 기존 메시지를 해제하고 새 메시지를 고정할 수 있다")
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
            existingMessage.updatePinStatus(true, userId)

            // when
            val result = newMessage.pinMessageInRoom(userId, 1, 5)

            // then
            assertThat(result.pinnedMessage.isPinned).isTrue()
            assertThat(result.pinnedMessage.pinnedBy).isEqualTo(userId)
            assertThat(result.pinnedMessage.pinnedAt).isNotNull()
            assertThat(result.unpinnedMessage).isNull()
        }

        @Test
        @DisplayName("[happy] 이미 고정된 메시지를 다시 고정하면 변경 없이 그대로 반환한다")
        fun `이미 고정된 메시지를 다시 고정하면 변경 없이 그대로 반환한다`() {
            // given
            val message = ChatMessage.create(
                roomId = ChatRoomId.from(1L),
                senderId = UserId.from(2L),
                text = "안녕하세요",
                type = MessageType.TEXT
            )
            val userId = UserId.from(3L)
            message.updatePinStatus(true, userId)

            // when
            val result = message.pinMessageInRoom(userId, 0, 5)

            // then
            assertThat(result.pinnedMessage).isEqualTo(message)
            assertThat(result.unpinnedMessage).isNull()
        }
    }
    */

    @Nested
    @DisplayName("메시지 내용 수정 시")
    inner class EditMessage {

        @Test
        @DisplayName("[happy] 텍스트 타입 메시지의 내용을 수정할 수 있다")
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
            message.editMessage(newContent)

            // then
            assertThat(message.content.text).isEqualTo(newContent)
            assertThat(message.content.isEdited).isTrue()
            assertThat(message.updatedAt).isNotNull()
        }

        @Test
        @DisplayName("[happy] 생성 후 23시간 59분 경과한 메시지를 수정할 수 있다")
        fun `생성 후 23시간 59분 경과한 메시지를 수정할 수 있다`() {
            // given
            val oldCreatedAt = Instant.now().minus(23, ChronoUnit.HOURS).minus(59, ChronoUnit.MINUTES)
            val message = ChatMessage.create(
                roomId = ChatRoomId.from(1L),
                senderId = UserId.from(2L),
                text = "안녕하세요",
                type = MessageType.TEXT
            )
            message.createdAt = oldCreatedAt
            val newContent = "수정된 내용입니다"

            // when
            message.editMessage(newContent)

            // then
            assertThat(message.content.text).isEqualTo(newContent)
            assertThat(message.content.isEdited).isTrue()
        }

        @Test
        @DisplayName("[bad] 생성 후 24시간이 경과한 메시지는 수정할 수 없다")
        fun `생성 후 24시간이 경과한 메시지는 수정할 수 없다`() {
            // given
            val oldCreatedAt = Instant.now().minus(24, ChronoUnit.HOURS)
            val message = ChatMessage.create(
                roomId = ChatRoomId.from(1L),
                senderId = UserId.from(2L),
                text = "안녕하세요",
                type = MessageType.TEXT
            )
            message.createdAt = oldCreatedAt
            val newContent = "수정된 내용입니다"

            // when & then
            assertThatThrownBy { message.editMessage(newContent) }
                .isInstanceOf(MessageException.EditTimeExpired::class.java)
                .hasMessageContaining("메시지는 생성 후 24시간 이내에만 수정할 수 있습니다")
        }

        @Test
        @DisplayName("[bad] 생성 후 25시간이 경과한 메시지는 수정할 수 없다")
        fun `생성 후 25시간이 경과한 메시지는 수정할 수 없다`() {
            // given
            val oldCreatedAt = Instant.now().minus(25, ChronoUnit.HOURS)
            val message = ChatMessage.create(
                roomId = ChatRoomId.from(1L),
                senderId = UserId.from(2L),
                text = "안녕하세요",
                type = MessageType.TEXT
            )
            message.createdAt = oldCreatedAt
            val newContent = "수정된 내용입니다"

            // when & then
            assertThatThrownBy { message.editMessage(newContent) }
                .isInstanceOf(MessageException.EditTimeExpired::class.java)
                .hasMessageContaining("메시지는 생성 후 24시간 이내에만 수정할 수 있습니다")
        }

        @Test
        @DisplayName("[bad] 빈 내용으로 수정하려고 하면 예외가 발생한다")
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
                .isInstanceOf(MessageException.EmptyContent::class.java)
                .hasMessageContaining("메시지 내용은 비어있을 수 없습니다")
        }

        @Test
        @DisplayName("[bad] 삭제된 메시지를 수정하려고 하면 예외가 발생한다")
        fun `삭제된 메시지를 수정하려고 하면 예외가 발생한다`() {
            // given
            val message = ChatMessage.create(
                roomId = ChatRoomId.from(1L),
                senderId = UserId.from(2L),
                text = "안녕하세요",
                type = MessageType.TEXT
            )
            message.markAsDeleted()
            val newContent = "수정된 내용입니다"

            // when & then
            assertThatThrownBy { message.editMessage(newContent) }
                .isInstanceOf(MessageException.NotEditable::class.java)
                .hasMessageContaining("삭제된 메시지는 수정할 수 없습니다")
        }

        @Test
        @DisplayName("[bad] 텍스트 타입이 아닌 메시지를 수정하려고 하면 예외가 발생한다")
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
                .isInstanceOf(MessageException.NotEditable::class.java)
                .hasMessageContaining("텍스트 타입의 메시지만 수정할 수 있습니다")
        }
    }

    @Nested
    @DisplayName("메시지 삭제 시")
    inner class MarkAsDeleted {

        @Test
        @DisplayName("[happy] 메시지를 삭제 상태로 변경할 수 있다")
        fun `메시지를 삭제 상태로 변경할 수 있다`() {
            // given
            val message = ChatMessage.create(
                roomId = ChatRoomId.from(1L),
                senderId = UserId.from(2L),
                text = "안녕하세요",
                type = MessageType.TEXT
            )

            // when
            message.markAsDeleted()

            // then
            assertThat(message.content.isDeleted).isTrue()
            assertThat(message.updatedAt).isNotNull()
        }
    }

    /**
     * TODO: MessageReaction Aggregate 분리로 인한 재작성 필요
     * - toggleReaction 메서드가 ChatMessage에서 제거됨
     * - MessageReaction을 별도 Aggregate로 테스트해야 함
     *
     * Note: 테스트 전체 주석 처리 (컴파일 에러 방지)
     */
    /*
    @Nested
    @DisplayName("메시지 반응 토글 시")
    inner class ToggleReaction {

        @Test
        @DisplayName("[happy] 메시지에 반응을 추가할 수 있다")
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
        @DisplayName("[happy] 이미 추가한 반응과 같은 반응을 선택하면 제거할 수 있다")
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
        @DisplayName("[happy] 이미 추가한 반응과 다른 반응을 선택하면 기존 반응을 제거하고 새 반응을 추가할 수 있다")
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
    */

    @Nested
    @DisplayName("URL 미리보기 처리 시")
    inner class UrlPreview {

        @Test
        @DisplayName("[happy] URL 미리보기 정보를 설정할 수 있다")
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
            message.setUrlPreview(urlPreview)

            // then
            assertThat(message.metadata.urlPreview).isEqualTo(urlPreview)
            assertThat(message.metadata.needsUrlPreview).isFalse()
            assertThat(message.updatedAt).isNotNull()
        }

        @Test
        @DisplayName("[happy] URL 미리보기가 필요함을 표시할 수 있다")
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
            message.markNeedsUrlPreview(url)

            // then
            assertThat(message.metadata.needsUrlPreview).isTrue()
            assertThat(message.metadata.previewUrl).isEqualTo(url)
            assertThat(message.updatedAt).isNotNull()
        }
    }

    @Nested
    @DisplayName("메시지 만료 설정 시")
    inner class Expiration {

        @Test
        @DisplayName("[happy] 만료 시간이 지나면 만료로 판단한다")
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
        @DisplayName("[happy] 미래의 만료 시간은 만료되지 않은 것으로 판단한다")
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
