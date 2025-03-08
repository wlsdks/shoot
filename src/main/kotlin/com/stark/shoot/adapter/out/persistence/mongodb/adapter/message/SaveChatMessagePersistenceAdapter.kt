package com.stark.shoot.adapter.out.persistence.mongodb.adapter.message

import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.adapter.out.persistence.mongodb.repository.ChatMessageMongoRepository
import com.stark.shoot.application.port.out.message.SaveChatMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.annotation.Adapter

@Adapter
class SaveChatMessagePersistenceAdapter(
    private val chatMessageRepository: ChatMessageMongoRepository,
    private val chatMessageMapper: ChatMessageMapper
) : SaveChatMessagePort {

    /**
     * 채팅 메시지 저장
     *
     * @param message 채팅 메시지
     * @return 저장된 채팅 메시지
     */
    override fun save(
        message: ChatMessage
    ): ChatMessage {
        val document = chatMessageMapper.toDocument(message)
        return chatMessageRepository.save(document)
            .let(chatMessageMapper::toDomain)
    }

    /**
     * 채팅 메시지 목록 저장
     *
     * @param messages 채팅 메시지 목록
     * @return 저장된 채팅 메시지 목록
     */
    override fun saveAll(
        messages: List<ChatMessage>
    ): List<ChatMessage> {
        val documents = messages.map(chatMessageMapper::toDocument)
        return chatMessageRepository.saveAll(documents)
            .map(chatMessageMapper::toDomain)
            .toList()
    }

}