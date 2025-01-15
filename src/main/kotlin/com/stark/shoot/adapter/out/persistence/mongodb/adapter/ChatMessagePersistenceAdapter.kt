package com.stark.shoot.adapter.out.persistence.mongodb.adapter

import com.stark.shoot.adapter.out.persistence.mongodb.repository.ChatMessageMongoRepository
import com.stark.shoot.application.port.out.LoadMessagePort
import com.stark.shoot.application.port.out.SaveMessagePort
import com.stark.shoot.domain.chat.ChatMessage
import org.bson.types.ObjectId
import org.springframework.stereotype.Component

@Component
class ChatMessagePersistenceAdapter(
    private val chatMessageRepository: ChatMessageMongoRepository,
    private val chatMessageMapper: ChatMessageMapper
) : SaveMessagePort, LoadMessagePort {

    override fun save(message: ChatMessage): ChatMessage {
        val entity = chatMessageMapper.toEntity(message)
        return chatMessageRepository.save(entity)
            .let { chatMessageMapper.toDomain(it) }
    }

    override fun findByRoomId(roomId: ObjectId): List<ChatMessage> {
        return chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(roomId)
            .map { chatMessageMapper.toDomain(it) }
    }

    override fun findById(id: ObjectId): ChatMessage? {
        return chatMessageRepository.findById(id)
            .map { chatMessageMapper.toDomain(it) }
            .orElse(null)
    }

}