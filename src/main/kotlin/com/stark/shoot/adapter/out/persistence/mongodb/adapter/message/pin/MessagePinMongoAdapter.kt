package com.stark.shoot.adapter.out.persistence.mongodb.adapter.message.pin

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.pin.MessagePinDocument
import com.stark.shoot.adapter.out.persistence.mongodb.repository.MessagePinMongoRepository
import com.stark.shoot.application.port.out.message.pin.MessagePinCommandPort
import com.stark.shoot.application.port.out.message.pin.MessagePinQueryPort
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.pin.MessagePin
import com.stark.shoot.domain.chat.pin.vo.MessagePinId
import com.stark.shoot.domain.chat.vo.ChatRoomId
import com.stark.shoot.infrastructure.annotation.Adapter
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

@Adapter
class MessagePinMongoAdapter(
    private val messagePinMongoRepository: MessagePinMongoRepository
) : MessagePinCommandPort, MessagePinQueryPort {

    // ========== CommandPort 구현 ==========

    override fun save(messagePin: MessagePin): MessagePin {
        val document = MessagePinDocument.fromDomain(messagePin)
        val savedDocument = messagePinMongoRepository.save(document)
        return savedDocument.toDomain()
    }

    override fun delete(id: MessagePinId) {
        messagePinMongoRepository.deleteById(id.value)
    }

    override fun deleteByMessageId(messageId: MessageId) {
        messagePinMongoRepository.deleteByMessageId(messageId.value)
    }

    override fun deleteAllByRoomId(roomId: ChatRoomId) {
        messagePinMongoRepository.deleteAllByRoomId(roomId.value)
    }

    // ========== QueryPort 구현 ==========

    override fun findById(id: MessagePinId): MessagePin? {
        return messagePinMongoRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findByMessageId(messageId: MessageId): MessagePin? {
        return messagePinMongoRepository.findByMessageId(messageId.value)
            ?.toDomain()
    }

    override fun findAllByRoomId(roomId: ChatRoomId, limit: Int): List<MessagePin> {
        val pageable = if (limit == Int.MAX_VALUE) {
            PageRequest.of(0, Int.MAX_VALUE, Sort.by(Sort.Direction.DESC, "pinnedAt"))
        } else {
            PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "pinnedAt"))
        }

        return messagePinMongoRepository.findAllByRoomId(roomId.value)
            .sortedByDescending { it.pinnedAt }
            .take(limit)
            .map { it.toDomain() }
    }

    override fun countByRoomId(roomId: ChatRoomId): Long {
        return messagePinMongoRepository.countByRoomId(roomId.value)
    }

    override fun isPinned(messageId: MessageId): Boolean {
        return messagePinMongoRepository.findByMessageId(messageId.value) != null
    }
}
