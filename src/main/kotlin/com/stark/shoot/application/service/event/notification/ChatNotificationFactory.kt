package com.stark.shoot.application.service.event.notification

import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.user.vo.UserId
import org.springframework.stereotype.Component

/**
 * 채팅 관련 알림을 생성하는 팩토리 클래스입니다.
 * 다양한 유형의 알림을 생성하는 메서드를 제공합니다.
 */
@Component
class ChatNotificationFactory {

    /**
     * 멘션 알림을 생성합니다.
     *
     * @param userId 알림을 받을 사용자 ID
     * @param message 채팅 메시지
     * @return 생성된 멘션 알림
     */
    fun createMentionNotification(userId: Long, message: ChatMessage): Notification {
        val truncatedText = truncateMessageText(message.content.text)

        return Notification.fromChatEvent(
            userId = UserId.from(userId),
            title = "새로운 멘션",
            message = "메시지에서 언급되었습니다: $truncatedText",
            type = NotificationType.MENTION,
            sourceId = message.roomId.toString(),
            metadata = createMessageMetadata(message)
        )
    }

    /**
     * 일반 메시지 알림을 생성합니다.
     *
     * @param userId 알림을 받을 사용자 ID
     * @param message 채팅 메시지
     * @return 생성된 메시지 알림
     */
    fun createMessageNotification(userId: Long, message: ChatMessage): Notification {
        val truncatedText = truncateMessageText(message.content.text)

        return Notification.fromChatEvent(
            userId = UserId.from(userId),
            title = "새로운 메시지",
            message = truncatedText,
            type = NotificationType.NEW_MESSAGE,
            sourceId = message.roomId.toString(),
            metadata = createMessageMetadata(message)
        )
    }

    /**
     * 메시지 텍스트를 적절한 길이로 잘라냅니다.
     *
     * @param text 원본 메시지 텍스트
     * @return 잘라낸 메시지 텍스트
     */
    private fun truncateMessageText(text: String): String {
        val maxLength = 50
        return if (text.length > maxLength) {
            text.take(maxLength) + "..."
        } else {
            text
        }
    }

    /**
     * 메시지 메타데이터를 생성합니다.
     *
     * @param message 채팅 메시지
     * @return 메타데이터 맵
     */
    private fun createMessageMetadata(message: ChatMessage): Map<String, Any> {
        return mapOf(
            "senderId" to message.senderId.toString(),
            "messageId" to (message.id ?: "").toString()
        )
    }

    /**
     * 메시지 반응 알림을 생성합니다.
     *
     * @param userId 알림을 받을 사용자 ID (메시지 작성자)
     * @param reactingUserId 반응을 추가한 사용자 ID
     * @param messageId 메시지 ID
     * @param roomId 채팅방 ID
     * @param reactionType 반응 타입
     * @return 생성된 반응 알림
     */
    fun createReactionNotification(
        userId: UserId,
        reactingUserId: UserId,
        messageId: MessageId,
        roomId: ChatRoomId,
        reactionType: String
    ): Notification {
        return Notification.fromChatEvent(
            userId = userId,
            title = "새로운 반응",
            message = "내 메시지에 새로운 반응이 추가되었습니다",
            type = NotificationType.REACTION,
            sourceId = roomId.toString(),
            metadata = mapOf(
                "messageId" to messageId,
                "reactingUserId" to reactingUserId.toString(),
                "reactionType" to reactionType
            )
        )
    }
}
