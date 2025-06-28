package com.stark.shoot.application.port.`in`.active.command

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.active.ChatActivity

/**
 * 사용자 활동 상태 커맨드
 * 사용자가 채팅방에 입장하거나 퇴장할 때 이 커맨드를 통해 상태를 업데이트합니다.
 */
data class UserActivityCommand(
    val userId: Long,
    val roomId: Long,
    val active: Boolean
) {
    companion object {
        /**
         * ChatActivity DTO로부터 커맨드 객체를 생성합니다.
         */
        fun of(activity: ChatActivity): UserActivityCommand {
            return UserActivityCommand(
                userId = activity.userId,
                roomId = activity.roomId,
                active = activity.active
            )
        }

        /**
         * JSON 문자열로부터 커맨드 객체를 생성합니다.
         */
        fun of(message: String, objectMapper: ObjectMapper): UserActivityCommand {
            val activity = objectMapper.readValue(message, ChatActivity::class.java)
            return of(activity)
        }
    }
}