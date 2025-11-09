package com.stark.shoot.adapter.out.persistence.mongodb.adapter.message.readreceipt

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.readreceipt.MessageReadReceiptDocument
import com.stark.shoot.adapter.out.persistence.mongodb.repository.MessageReadReceiptMongoRepository
import com.stark.shoot.application.port.out.message.readreceipt.MessageReadReceiptCommandPort
import com.stark.shoot.application.port.out.message.readreceipt.MessageReadReceiptQueryPort
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.readreceipt.MessageReadReceipt
import com.stark.shoot.domain.chat.readreceipt.vo.MessageReadReceiptId
import com.stark.shoot.domain.chat.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.Adapter

@Adapter
class MessageReadReceiptMongoAdapter(
    private val messageReadReceiptMongoRepository: MessageReadReceiptMongoRepository
) : MessageReadReceiptCommandPort, MessageReadReceiptQueryPort {

    // ========== CommandPort 구현 ==========

    override fun save(readReceipt: MessageReadReceipt): MessageReadReceipt {
        val document = MessageReadReceiptDocument.fromDomain(readReceipt)
        val savedDocument = messageReadReceiptMongoRepository.save(document)
        return savedDocument.toDomain()
    }

    override fun delete(id: MessageReadReceiptId) {
        messageReadReceiptMongoRepository.deleteById(id.value)
    }

    override fun deleteByMessageIdAndUserId(messageId: MessageId, userId: UserId) {
        messageReadReceiptMongoRepository.deleteByMessageIdAndUserId(messageId.value, userId.value)
    }

    override fun deleteAllByMessageId(messageId: MessageId) {
        messageReadReceiptMongoRepository.deleteByMessageId(messageId.value)
    }

    override fun deleteAllByRoomId(roomId: ChatRoomId) {
        messageReadReceiptMongoRepository.deleteAllByRoomId(roomId.value)
    }

    // ========== QueryPort 구현 ==========

    override fun findById(id: MessageReadReceiptId): MessageReadReceipt? {
        return messageReadReceiptMongoRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findByMessageIdAndUserId(messageId: MessageId, userId: UserId): MessageReadReceipt? {
        return messageReadReceiptMongoRepository.findByMessageIdAndUserId(messageId.value, userId.value)
            ?.toDomain()
    }

    override fun findAllByMessageId(messageId: MessageId): List<MessageReadReceipt> {
        return messageReadReceiptMongoRepository.findAllByMessageId(messageId.value)
            .map { it.toDomain() }
    }

    override fun findAllByRoomId(roomId: ChatRoomId): List<MessageReadReceipt> {
        return messageReadReceiptMongoRepository.findAllByRoomId(roomId.value)
            .map { it.toDomain() }
    }

    override fun countByMessageId(messageId: MessageId): Long {
        return messageReadReceiptMongoRepository.countByMessageId(messageId.value)
    }

    override fun hasRead(messageId: MessageId, userId: UserId): Boolean {
        return messageReadReceiptMongoRepository.existsByMessageIdAndUserId(messageId.value, userId.value)
    }

    override fun getReadByMap(messageId: MessageId): Map<UserId, Boolean> {
        val receipts = messageReadReceiptMongoRepository.findAllByMessageId(messageId.value)
        return receipts.associate { UserId.from(it.userId) to true }
    }
}
