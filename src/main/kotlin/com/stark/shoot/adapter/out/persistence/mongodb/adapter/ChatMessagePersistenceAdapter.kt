package com.stark.shoot.adapter.out.persistence.mongodb.adapter

import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.adapter.out.persistence.mongodb.repository.ChatMessageMongoRepository
import com.stark.shoot.application.port.out.LoadChatMessagePort
import com.stark.shoot.application.port.out.SaveChatMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ChatMessagePersistenceAdapter(
    private val chatMessageRepository: ChatMessageMongoRepository,
    private val chatMessageMapper: ChatMessageMapper
) : SaveChatMessagePort, LoadChatMessagePort {

    override fun findById(id: ObjectId): ChatMessage? {
        return chatMessageRepository.findById(id)
            .map(chatMessageMapper::toDomain)
            .orElse(null)
    }

    override fun findByRoomId(roomId: ObjectId): List<ChatMessage> {
        return chatMessageRepository.findByRoomId(roomId)
            .map(chatMessageMapper::toDomain)
    }

    override fun findByRoomIdAndBeforeCreatedAt(roomId: ObjectId, createdAt: Instant): List<ChatMessage> {
        return chatMessageRepository.findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(roomId, createdAt)
            .map(chatMessageMapper::toDomain)
    }

    override fun save(message: ChatMessage): ChatMessage {
        val document = chatMessageMapper.toDocument(message)
        return chatMessageRepository.save(document)
            .let(chatMessageMapper::toDomain)
    }

    override fun saveAll(messages: List<ChatMessage>): List<ChatMessage> {
        val documents = messages.map(chatMessageMapper::toDocument)
        return chatMessageRepository.saveAll(documents)
            .map(chatMessageMapper::toDomain)
            .toList()
    }

}