package com.stark.shoot.adapter.out.persistence.mongodb.document.message.readreceipt

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.readreceipt.MessageReadReceipt
import com.stark.shoot.domain.chat.readreceipt.vo.MessageReadReceiptId
import com.stark.shoot.domain.chat.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "message_read_receipts")
@CompoundIndexes(
    CompoundIndex(name = "message_user_idx", def = "{'messageId': 1, 'userId': 1}", unique = true),
    CompoundIndex(name = "room_user_idx", def = "{'roomId': 1, 'userId': 1}"),
    CompoundIndex(name = "message_read_idx", def = "{'messageId': 1, 'readAt': -1}")
)
data class MessageReadReceiptDocument(
    @Id
    val id: Long? = null,
    @Indexed
    val messageId: String,
    val roomId: Long,
    val userId: Long,
    val readAt: Instant = Instant.now(),
    val createdAt: Instant = Instant.now()
) {
    fun toDomain(): MessageReadReceipt {
        return MessageReadReceipt(
            id = id?.let { MessageReadReceiptId.from(it) },
            messageId = MessageId.from(messageId),
            roomId = ChatRoomId.from(roomId),
            userId = UserId.from(userId),
            readAt = readAt,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(receipt: MessageReadReceipt): MessageReadReceiptDocument {
            return MessageReadReceiptDocument(
                id = receipt.id?.value,
                messageId = receipt.messageId.value,
                roomId = receipt.roomId.value,
                userId = receipt.userId.value,
                readAt = receipt.readAt,
                createdAt = receipt.createdAt
            )
        }
    }
}
