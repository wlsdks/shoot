package com.stark.shoot.adapter.out.persistence.mongodb.document.message.pin

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.pin.MessagePin
import com.stark.shoot.domain.chat.pin.vo.MessagePinId
import com.stark.shoot.domain.chat.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "message_pins")
@CompoundIndexes(
    CompoundIndex(name = "room_pinned_idx", def = "{'roomId': 1, 'pinnedAt': -1}")
)
data class MessagePinDocument(
    @Id
    val id: Long? = null,
    @Indexed
    val messageId: String,
    val roomId: Long,
    val pinnedBy: Long,
    val pinnedAt: Instant = Instant.now(),
    val createdAt: Instant = Instant.now()
) {
    fun toDomain(): MessagePin {
        return MessagePin(
            id = id?.let { MessagePinId.from(it) },
            messageId = MessageId.from(messageId),
            roomId = ChatRoomId.from(roomId),
            pinnedBy = UserId.from(pinnedBy),
            pinnedAt = pinnedAt,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(pin: MessagePin): MessagePinDocument {
            return MessagePinDocument(
                id = pin.id?.value,
                messageId = pin.messageId.value,
                roomId = pin.roomId.value,
                pinnedBy = pin.pinnedBy.value,
                pinnedAt = pin.pinnedAt,
                createdAt = pin.createdAt
            )
        }
    }
}
