package com.stark.shoot.infrastructure.config.socket

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.application.port.out.LoadChatRoomPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration

@Configuration
class WebSocketChannelConfig(
    private val loadChatRoomPort: LoadChatRoomPort,
    private val objectMapper: ObjectMapper,
    private val rateLimitInterceptor: RateLimitInterceptor
) : WebSocketMessageBrokerConfigurer {

    /**
     * STOMP 채널 인터셉터 Bean 생성
     */
    @Bean
    fun stompChannelInterceptor(): StompChannelInterceptor {
        return StompChannelInterceptor(loadChatRoomPort, objectMapper)
    }

    /**
     * WebSocket 전송 관련 제한 설정
     * - sendTimeLimit: 메시지 전송 시간 제한 (15초) : 단일 메시지 전송 시간 제한
     * - sendBufferSizeLimit: 전송 버퍼 크기 제한 (512KB)
     * - messageSizeLimit: 단일 메시지 크기 제한 (128KB)
     */
    override fun configureWebSocketTransport(registration: WebSocketTransportRegistration) {
        registration
            .setSendTimeLimit(15 * 1000) // 15초
            .setSendBufferSizeLimit(512 * 1024) // 512KB
            .setMessageSizeLimit(128 * 1024) // 128KB
    }

    /**
     * 클라이언트로부터 들어오는 메시지 처리 채널 설정
     * - StompChannelInterceptor를 통해 메시지 인터셉트
     * - 스레드 풀 설정으로 동시 처리량 조절
     *   - corePoolSize: 기본 실행 스레드 수 (4개)
     *   - maxPoolSize: 최대 스레드 수 (10개)
     *   - queueCapacity: 대기열 크기 (50개)
     */
    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(
            stompChannelInterceptor(),  // Bean으로 생성한 인터셉터 사용
            rateLimitInterceptor
        )
        registration.taskExecutor()
            .corePoolSize(4)
            .maxPoolSize(10)
            .queueCapacity(50)
    }

}