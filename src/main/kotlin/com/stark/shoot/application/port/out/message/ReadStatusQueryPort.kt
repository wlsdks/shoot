package com.stark.shoot.application.port.out.message

import com.stark.shoot.adapter.`in`.web.dto.message.read.ReadStatus
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId

interface ReadStatusQueryPort {
    fun findByRoomIdAndUserId(roomId: ChatRoomId, userId: UserId): ReadStatus?
    fun findAllByRoomId(roomId: ChatRoomId): List<ReadStatus>
}
