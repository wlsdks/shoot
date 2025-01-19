package com.stark.shoot.infrastructure.config.socket

import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.stereotype.Component
import java.nio.file.AccessDeniedException

/**
 * StompChannelInterceptor는 STOMP 프로토콜 기반의 메시지를 가로채어
 * 경로 접근 권한을 확인하고, 필요 시 예외를 발생시켜 메시지 처리를 중단하는 역할을 담당합니다.
 *
 * 메시지가 인바운드 채널(서버로 들어오는 경로)을 통과할 때 preSend()가 호출되어
 * STOMP 명령어(SEND, SUBSCRIBE 등)를 확인하고 권한 로직을 수행할 수 있습니다.
 */
@Component
class StompChannelInterceptor : ChannelInterceptor {

    /**
     * STOMP 프레임이 인바운드 채널을 통과하기 전에 호출됩니다.
     * 메시지의 STOMP 명령어와 헤더를 확인하여 특정 경로의 접근 권한을 검사할 수 있습니다.
     *
     * @param message 인바운드 메시지 (STOMP 프레임)
     * @param channel 메시지가 통과하는 채널
     * @return 원본 메시지를 그대로 반환하거나, 접근이 거부되면 예외 발생으로 처리를 중단
     * @throws AccessDeniedException 권한이 없는 사용자가 접근하려 할 경우 발생
     */
    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        // STOMP 헤더를 래핑해 편하게 분석하기 위해 사용
        val accessor = StompHeaderAccessor.wrap(message)

        when (accessor.command) {
            // 구독 명령어 (SUBSCRIBE)
            StompCommand.SUBSCRIBE -> {
                val destination = accessor.destination
                // 예시 로직: /topic/admin 경로는 관리자만 구독 가능하도록 설정
                if (destination?.startsWith("/topic/admin/") == true) {
                    // 예: 유저의 역할(ROLE)을 user 정보에서 가져옴
                    val userRole = accessor.user?.name
                    if (userRole != "ADMIN") {
                        // 권한이 없다면 예외 발생 -> 메시지 처리 중단
                        throw AccessDeniedException("권한이 없습니다.")
                    }
                }
            }

            // 메시지 전송 명령어 (SEND)
            StompCommand.SEND -> {
                // 특정 전송 경로에 대한 권한 체크 로직을 여기에 추가
                // 예: /app/admin/* 경로의 전송은 특정 권한만 허용
            }

            // 그 외 명령어 처리
            else -> {
                // CONNECT, DISCONNECT 등 다른 STOMP 커맨드에 대한 추가 로직
            }
        }

        // 별다른 문제가 없다면 메시지를 그대로 반환 -> 다음 단계로 진행
        return message
    }

}
