package com.stark.shoot.adapter.`in`.web.socket.active

import com.stark.shoot.application.port.`in`.active.UserActiveUseCase
import io.swagger.v3.oas.annotations.Operation
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller

@Controller
class UserActivityStompHandler(
    private val userActiveUseCase: UserActiveUseCase
) {

    @Operation(
        summary = "유저가 활동중인지 그 여부를 설정합니다.",
        description = """
            - user1과 user2가 room123에 있을 때, user1이 채팅방을 나가면 Redis에 active:user1:room123이 "false"로 설정.
            - 만약 user1이 다시 채팅방에 들어오면 active:user1:room123을 "true"로 설정.
            - true: 채팅방에 참여 중, false: 채팅방에 참여하지 않음
        """
    )
    @MessageMapping("/active")
    fun handleActivity(message: String) {
        userActiveUseCase.updateUserActive(message)
    }

}