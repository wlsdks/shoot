package com.stark.shoot.adapter.out.persistence.mongodb.adapter.message

import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.adapter.out.persistence.mongodb.repository.ChatMessageMongoRepository
import com.stark.shoot.application.port.out.message.MessageCommandPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.infrastructure.annotation.Adapter

@Adapter
class MessageCommandMongoAdapter(
    private val chatMessageRepository: ChatMessageMongoRepository,
    private val chatMessageMapper: ChatMessageMapper
) : MessageCommandPort {

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

    /**
     * 채팅 메시지 삭제 (보상 트랜잭션용)
     *
     * Saga 실패 시 롤백을 위해 메시지를 물리적으로 삭제합니다.
     *
     * @param messageId 삭제할 메시지 ID
     */
    override fun delete(messageId: MessageId) {
        chatMessageRepository.deleteById(org.bson.types.ObjectId(messageId.value))
    }

}
