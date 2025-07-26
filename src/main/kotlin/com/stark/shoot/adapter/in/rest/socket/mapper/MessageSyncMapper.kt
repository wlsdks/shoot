package com.stark.shoot.adapter.`in`.rest.socket.mapper

import com.stark.shoot.adapter.`in`.rest.dto.message.MessageContentRequest
import com.stark.shoot.adapter.`in`.rest.socket.dto.MessageSyncInfoDto
import com.stark.shoot.adapter.`in`.rest.socket.dto.ReactionDto
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.reaction.type.ReactionType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * 채팅 메시지 도메인 객체를 DTO로 변환하는 매퍼
 */
@Component
class MessageSyncMapper {

    private val logger = KotlinLogging.logger {}

    /**
     * ChatMessage를 MessageSyncInfoDto로 변환
     *
     * @param message ChatMessage 객체
     * @return MessageSyncInfoDto 객체
     */
    fun toSyncInfoDto(message: ChatMessage, replyCount: Long? = null): MessageSyncInfoDto {
        logger.trace { "메시지 변환: messageId=${message.id}, senderId=${message.senderId}" }

        // 도메인 Attachment 객체를 ID 문자열로 변환
        val attachmentIds = message.content.attachments.map { it.id }

        // 리액션 맵을 ReactionDto 리스트로 변환
        val reactionDtos = message.reactions.map { (reactionType, userIds) ->
            val reaction = ReactionType.fromCode(reactionType)
            ReactionDto(
                reactionType = reactionType,
                emoji = reaction?.emoji ?: "",
                description = reaction?.description ?: "",
                userIds = userIds.toList(),
                count = userIds.size
            )
        }

        return MessageSyncInfoDto(
            id = message.id?.value ?: "",
            tempId = message.metadata.tempId,
            timestamp = message.createdAt ?: Instant.now(),
            senderId = message.senderId.value,
            status = message.status.name,
            content = MessageContentRequest(
                text = message.content.text,
                type = message.content.type,
                attachments = attachmentIds,
                isEdited = message.content.isEdited,
                isDeleted = message.content.isDeleted
            ),
            readBy = message.readBy.mapKeys { it.key.value },
            reactions = reactionDtos,
            replyCount = replyCount
        )
    }

}
