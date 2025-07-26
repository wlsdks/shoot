package com.stark.shoot.adapter.`in`.rest.socket.message

import com.stark.shoot.adapter.`in`.rest.dto.message.ChatMessageRequest
import com.stark.shoot.application.port.`in`.message.SendMessageUseCase
import com.stark.shoot.application.port.`in`.message.command.SendMessageCommand
import io.swagger.v3.oas.annotations.Operation
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller

@Controller
class SendMessageStompHandler(
    private val sendMessageUseCase: SendMessageUseCase
) {

    /**
     * 클라이언트로부터 메시지를 수신하여 처리합니다.
     * 1. 메시지에 임시 ID와 "sending" 상태 추가
     * 2. Redis를 통해 메시지 즉시 브로드캐스트 (실시간성)
     * 3. Kafka를 통해 메시지 영속화 (안정성)
     * 4. 메시지 상태 업데이트를 클라이언트에 전송
     */
    @Operation(
        summary = "클라이언트로부터 메시지를 수신하여 Redis, Kafka로 전달하여 처리합니다.",
        description = """
            - 웹소켓으로 메시지를 받으면 Redis(Pub/Sub)로 실시간 전송을 하고 Kafka로 mongoDB에 저장합니다.
              - 1. 메시지에 임시 ID와 "sending" 상태 추가 (임시 상태를 웹소켓으로 보내서 프론트에서 상태 제어: 전송중, 실패 등)
              - 2. Redis를 통해 메시지 즉시 브로드캐스트    (일단 실시간으로 웹소켓으로 상대방에게 메시지 전송)
              - 3. Kafka를 통해 메시지 영속화            (메시지 저장을 보장하기 위해서 분리)
              - 4. 메시지 상태 업데이트를 클라이언트에 전송   (최종 저장되면 SAVED 상태 전송)
        """
    )
    @MessageMapping("/chat")
    fun handleChatMessage(message: ChatMessageRequest) {
        val command = SendMessageCommand.of(message)
        sendMessageUseCase.sendMessage(command)
    }

}
