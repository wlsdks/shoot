package com.stark.shoot.adapter.out.persistence.mongodb.adapter.message

import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.adapter.out.persistence.mongodb.repository.ChatMessageMongoRepository
import com.stark.shoot.application.port.out.LoadChatMessagePort
import com.stark.shoot.application.port.out.SaveChatMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import org.bson.types.ObjectId
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ChatMessagePersistenceAdapter(
    private val chatMessageRepository: ChatMessageMongoRepository,
    private val chatMessageMapper: ChatMessageMapper
) : SaveChatMessagePort, LoadChatMessagePort {

    override fun findById(
        id: ObjectId
    ): ChatMessage? {
        return chatMessageRepository.findById(id)
            .map(chatMessageMapper::toDomain)
            .orElse(null)
    }

    override fun findByRoomId(roomId: ObjectId): List<ChatMessage> {
        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
        return chatMessageRepository.findByRoomId(roomId, pageable)
            .map(chatMessageMapper::toDomain)
    }

    override fun findByRoomIdAndBeforeCreatedAt(roomId: ObjectId, createdAt: Instant): List<ChatMessage> {
        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
        return chatMessageRepository.findByRoomIdAndCreatedAtBefore(roomId, createdAt, pageable)
            .map(chatMessageMapper::toDomain)
    }

    override fun countUnreadMessages(
        roomId: String,
        lastReadMessageId: String?
    ): Int {
        val roomObjectId = ObjectId(roomId)
        val lastReadMessage = lastReadMessageId?.let { ObjectId(it) }

        // 마지막 읽은 메시지가 있는 경우
        return if (lastReadMessage != null) {
            val lastReadMessageCreatedAt = chatMessageRepository.findById(lastReadMessage)
                .map { it.createdAt }
                .orElseThrow { IllegalArgumentException("Invalid lastReadMessageId: $lastReadMessageId") }
            chatMessageRepository.countByRoomIdAndCreatedAtAfter(roomObjectId, lastReadMessageCreatedAt)
        } else {
            // 마지막 읽은 메시지가 없는 경우
            chatMessageRepository.countByRoomId(roomObjectId)
        }
    }

    override fun findUnreadByRoomId(
        roomId: ObjectId,
        userId: ObjectId
    ): List<ChatMessage> {
        val notReadMessage = chatMessageRepository.findByRoomIdAndReadByNotContaining(roomId, userId)
        return notReadMessage.map(chatMessageMapper::toDomain)
    }

    override fun save(
        message: ChatMessage
    ): ChatMessage {
        val document = chatMessageMapper.toDocument(message)
        return chatMessageRepository.save(document)
            .let(chatMessageMapper::toDomain)
    }

    override fun saveAll(
        messages: List<ChatMessage>
    ): List<ChatMessage> {
        val documents = messages.map(chatMessageMapper::toDocument)
        return chatMessageRepository.saveAll(documents)
            .map(chatMessageMapper::toDomain)
            .toList()
    }

}