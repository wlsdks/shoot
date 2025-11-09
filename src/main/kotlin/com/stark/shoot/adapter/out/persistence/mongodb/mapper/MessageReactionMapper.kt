package com.stark.shoot.adapter.out.persistence.mongodb.mapper

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.reaction.MessageReactionDocument
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.reaction.MessageReaction
import com.stark.shoot.domain.chat.reaction.type.ReactionType
import com.stark.shoot.domain.chat.reaction.vo.MessageReactionId
import com.stark.shoot.domain.shared.UserId
import org.springframework.stereotype.Component

/**
 * MessageReaction Domain ↔ MessageReactionDocument 변환 Mapper
 */
@Component
class MessageReactionMapper {

    /**
     * Document → Domain 변환
     *
     * @param document MessageReactionDocument
     * @return MessageReaction domain object
     */
    fun toDomain(document: MessageReactionDocument): MessageReaction {
        return MessageReaction(
            id = document.id?.let { MessageReactionId.from(it) },
            messageId = MessageId.from(document.messageId),
            userId = UserId.from(document.userId),
            reactionType = ReactionType.valueOf(document.reactionType),
            createdAt = document.createdAt,
            updatedAt = document.updatedAt
        )
    }

    /**
     * Domain → Document 변환
     *
     * @param domain MessageReaction domain object
     * @return MessageReactionDocument
     */
    fun toDocument(domain: MessageReaction): MessageReactionDocument {
        return MessageReactionDocument(
            id = domain.id?.value,
            messageId = domain.messageId.value,
            userId = domain.userId.value,
            reactionType = domain.reactionType.name,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }

    /**
     * 여러 Document를 Domain List로 변환
     *
     * @param documents MessageReactionDocument list
     * @return MessageReaction domain list
     */
    fun toDomainList(documents: List<MessageReactionDocument>): List<MessageReaction> {
        return documents.map { toDomain(it) }
    }
}
