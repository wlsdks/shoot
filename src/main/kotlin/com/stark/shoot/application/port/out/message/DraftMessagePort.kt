package com.stark.shoot.application.port.out.message

import com.stark.shoot.domain.chat.message.DraftMessage
import org.bson.types.ObjectId

interface DraftMessagePort {
    fun saveDraft(draft: DraftMessage): DraftMessage
    fun findById(id: ObjectId): DraftMessage?
    fun findByUserAndRoom(userId: Long, roomId: Long): DraftMessage?
    fun findAllByUser(userId: Long): List<DraftMessage>
    fun deleteDraft(id: ObjectId): Boolean
}