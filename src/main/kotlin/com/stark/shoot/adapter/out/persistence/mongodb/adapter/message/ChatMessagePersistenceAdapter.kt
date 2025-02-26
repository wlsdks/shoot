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

    override fun findByRoomId(
        roomId: ObjectId,
        limit: Int
    ): List<ChatMessage> {
        val pageable = PageRequest.of(
            0,
            limit,
            Sort.by(Sort.Direction.DESC, "_id") // 최신순 정렬
        )

        return chatMessageRepository.findByRoomId(roomId, pageable)
            .map(chatMessageMapper::toDomain)
    }

    override fun findByRoomIdAndBeforeId(
        roomId: ObjectId,
        lastId: ObjectId,
        limit: Int
    ): List<ChatMessage> {
        val pageable = PageRequest.of(
            0,
            limit,
            Sort.by(Sort.Direction.DESC, "_id") // 최신순 정렬
        )

        return chatMessageRepository.findByRoomIdAndIdBefore(roomId, lastId, pageable)
            .map(chatMessageMapper::toDomain)
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