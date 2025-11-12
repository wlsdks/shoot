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

    /**
     * 읽음 표시가 없는 경우에만 저장합니다 (경쟁 조건 방지).
     * 이미 존재하는 경우 기존 레코드를 반환합니다.
     *
     * @param readReceipt 저장할 읽음 표시
     * @return 저장되거나 이미 존재하는 읽음 표시
     */
    override fun saveIfNotExists(readReceipt: MessageReadReceipt): MessageReadReceipt {
        // 1. 이미 존재하는지 확인
        val existing = findByMessageIdAndUserId(readReceipt.messageId, readReceipt.userId)
        if (existing != null) {
            return existing
        }

        // 2. 없으면 새로 저장 (중복 키 에러가 발생할 수 있음)
        return try {
            save(readReceipt)
        } catch (e: Exception) {
            // 3. 중복 키 에러 발생 시 (다른 요청이 동시에 저장한 경우) 다시 조회
            findByMessageIdAndUserId(readReceipt.messageId, readReceipt.userId)
                ?: throw e // 조회도 실패하면 원래 예외를 다시 던짐
        }
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
