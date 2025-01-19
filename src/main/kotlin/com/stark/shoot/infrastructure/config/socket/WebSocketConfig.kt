package com.stark.shoot.infrastructure.config.socket

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.application.port.out.LoadChatRoomPort
import com.stark.shoot.application.port.out.user.RetrieveUserPort
import com.stark.shoot.infrastructure.config.security.JwtAuthenticationService
import com.stark.shoot.infrastructure.config.socket.interceptor.AuthHandshakeInterceptor
import com.stark.shoot.infrastructure.config.socket.interceptor.RateLimitInterceptor
import com.stark.shoot.infrastructure.config.socket.interceptor.StompChannelInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration

@Configuration
@EnableWebSocketMessageBroker // STOMP 메시징을 활성화 이로 인해 서버는 STOMP 프로토콜 형식의 메시지를 기대합니다.
class WebSocketConfig(
    private val loadChatRoomPort: LoadChatRoomPort,
    private val retrieveUserPort: RetrieveUserPort,
    private val jwtAuthenticationService: JwtAuthenticationService,
    private val objectMapper: ObjectMapper,
    private val rateLimitInterceptor: RateLimitInterceptor
) : WebSocketMessageBrokerConfigurer {
    /**
     * 1) STOMP WebSocket 연결 엔드포인트 설정
     * - /ws/chat 로 연결
     * - AuthHandshakeInterceptor로 JWT 인증
     * - CORS 제한 해제
     */
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws/chat") // WebSocket 엔드포인트
            .addInterceptors(AuthHandshakeInterceptor(jwtAuthenticationService)) // 인증 인터셉터 추가
            .setAllowedOriginPatterns("*") // CORS 문제 방지
    }

    /**
     * 2) 메시지 브로커 설정
     * - /topic, /queue 경로로 SimpleBroker 활성화
     * - /app prefix로 메시지 송신
     * - 10초마다 heartbeat
     */
    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic", "/queue") // 클라이언트가 구독할 경로
            .setHeartbeatValue(longArrayOf(10000, 10000))                 // 서버->클라이언트, 클라이언트->서버 각각 10초
            .setTaskScheduler(heartbeatScheduler())                       // TaskScheduler 설정 추가

        registry.setApplicationDestinationPrefixes("/app")                // 클라이언트가 메시지를 전송할 경로
    }

    /**
     * 3) WebSocket 전송 관련 제한 설정
     * - 단일 메시지 전송 시간, 메시지 크기 제한
     */
    override fun configureWebSocketTransport(registration: WebSocketTransportRegistration) {
        registration
            .setSendTimeLimit(15_000)        // 15초
            .setSendBufferSizeLimit(512 * 1024) // 512KB
            .setMessageSizeLimit(128 * 1024) // 128KB
    }

    /**
     * 4) 클라이언트 -> 서버 인바운드 채널 설정
     * - StompChannelInterceptor + RateLimitInterceptor 등록
     * - 스레드 풀 설정
     */
    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(
            StompChannelInterceptor(loadChatRoomPort, retrieveUserPort, objectMapper),
            rateLimitInterceptor
        )
        registration.taskExecutor()
            .corePoolSize(4)
            .maxPoolSize(10)
            .queueCapacity(50)
    }

    /**
     * Heartbeat 처리를 위한 TaskScheduler
     */
    @Bean
    fun heartbeatScheduler(): TaskScheduler {
        return ThreadPoolTaskScheduler().apply {
            poolSize = 2 // heartbeat 처리용 스레드 풀
            setThreadNamePrefix("ws-heartbeat-")
            initialize()
        }
    }

}
