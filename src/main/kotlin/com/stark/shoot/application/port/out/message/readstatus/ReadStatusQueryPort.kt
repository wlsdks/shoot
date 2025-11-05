package com.stark.shoot.application.port.out.message.readstatus

import com.stark.shoot.adapter.`in`.rest.dto.message.read.ReadStatus
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId

interface ReadStatusQueryPort {
    fun findByRoomIdAndUserId(roomId: ChatRoomId, userId: UserId): ReadStatus?
    fun findAllByRoomId(roomId: ChatRoomId): List<ReadStatus>
}