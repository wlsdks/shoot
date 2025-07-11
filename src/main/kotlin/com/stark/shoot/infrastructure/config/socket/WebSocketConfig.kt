package com.stark.shoot.infrastructure.config.socket

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.application.port.out.user.UserQueryPort
import com.stark.shoot.infrastructure.config.security.JwtAuthenticationService
import com.stark.shoot.infrastructure.config.socket.interceptor.AuthHandshakeInterceptor
import com.stark.shoot.infrastructure.config.socket.interceptor.CustomHandshakeHandler
import com.stark.shoot.infrastructure.config.socket.interceptor.RateLimitInterceptor
import com.stark.shoot.infrastructure.config.socket.interceptor.StompChannelInterceptor
import io.github.oshai.kotlinlogging.KotlinLogging
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
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean
import java.util.concurrent.TimeUnit

@Configuration
@EnableWebSocketMessageBroker // STOMP 메시징을 활성화 이로 인해 서버는 STOMP 프로토콜 형식의 메시지를 기대합니다.
class WebSocketConfig(
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val userQueryPort: UserQueryPort,
    private val jwtAuthenticationService: JwtAuthenticationService,
    private val objectMapper: ObjectMapper,
    private val rateLimitInterceptor: RateLimitInterceptor
) : WebSocketMessageBrokerConfigurer {

    private val logger = KotlinLogging.logger {}

    // CPU 코어 수 계산을 한 번만 수행하도록 최적화
    private val availableProcessors = Runtime.getRuntime().availableProcessors()

    /**
     * 1) STOMP WebSocket 연결 엔드포인트 설정
     * - /ws/chat 로 연결
     * - AuthHandshakeInterceptor로 JWT 인증
     * - CORS 제한 해제
     */
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws/chat") // WebSocket 엔드포인트
            .addInterceptors(AuthHandshakeInterceptor(jwtAuthenticationService)) // 인증 인터셉터 추가
            .setHandshakeHandler(CustomHandshakeHandler())
            .setAllowedOriginPatterns("*") // CORS 문제 방지
            .withSockJS() // SockJS fallback 엔드포인트 활성화
            .setDisconnectDelay(2000) // 연결 해제 지연 시간 최적화 (2초)
            .setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js") // CDN 사용
    }

    /**
     * 2) 메시지 브로커 설정
     * - /topic, /queue 경로로 SimpleBroker 활성화
     * - /app prefix로 메시지 송신
     * - 최적화된 heartbeat 간격 (5초)
     */
    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic", "/queue") // 클라이언트가 구독할 경로
            .setHeartbeatValue(longArrayOf(5000, 5000))  // 서버->클라이언트, 클라이언트->서버 각각 5초로 최적화
            .setTaskScheduler(heartbeatScheduler())      // 최적화된 TaskScheduler 설정

        // `SendMessageStompHandler.kt`에서 `/chat` 경로에 대한 메시지 핸들러가 정의되어 있습니다.
        // 접두사(`/app`)와 매핑 경로(`/chat`)를 합치면 `/app/chat`이 됩니다. (StompChannelInterceptor 에서도 이 경로에 대한 인터셉터가 존재)
        registry.setApplicationDestinationPrefixes("/app") // 클라이언트가 메시지를 전송할 경로

        // 성능 로깅
        logger.info { "WebSocket 메시지 브로커 설정 완료: heartbeat=5초, prefixes=/app" }
    }

    /**
     * 3) WebSocket 전송 관련 제한 설정
     * - 단일 메시지 전송 시간, 메시지 크기 제한 최적화
     * - 성능 최적화를 위한 버퍼 크기 조정
     */
    override fun configureWebSocketTransport(registration: WebSocketTransportRegistration) {
        registration
            .setSendTimeLimit(10_000)         // 10초로 최적화
            .setSendBufferSizeLimit(256 * 1024) // 256KB로 최적화
            .setMessageSizeLimit(64 * 1024)   // 64KB로 최적화
            .setTimeToFirstMessage(30_000)    // 첫 메시지 대기 시간 설정
    }

    /**
     * 4) 클라이언트 -> 서버 인바운드 채널 설정
     * - StompChannelInterceptor + RateLimitInterceptor 등록
     * - 스레드 풀 설정 최적화
     */
    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(
            StompChannelInterceptor(chatRoomQueryPort, userQueryPort, objectMapper),
            rateLimitInterceptor
        )

        // 인바운드 채널 스레드 풀 설정
        val inboundCorePoolSize = calculateCorePoolSize(2) // CPU 코어 수 * 2
        val inboundMaxPoolSize = calculateMaxPoolSize(8)   // CPU 코어 수 * 8

        registration.taskExecutor()
            .corePoolSize(inboundCorePoolSize)
            .maxPoolSize(inboundMaxPoolSize)
            .queueCapacity(2000)               // 큐 크기 대폭 증가
            .keepAliveSeconds(60)              // 유휴 스레드 유지 시간

        logger.info { "WebSocket 인바운드 채널 스레드 풀 설정: core=$inboundCorePoolSize, max=$inboundMaxPoolSize, queue=2000" }
    }

    /**
     * 5) 클라이언트 <- 서버 아웃바운드 채널 설정
     * - 스레드 풀 설정 최적화
     */
    override fun configureClientOutboundChannel(registration: ChannelRegistration) {
        // 아웃바운드 채널 스레드 풀 설정
        val outboundCorePoolSize = calculateCorePoolSize(2) // CPU 코어 수 * 2
        val outboundMaxPoolSize = calculateMaxPoolSize(6)   // CPU 코어 수 * 6

        registration.taskExecutor()
            .corePoolSize(outboundCorePoolSize)
            .maxPoolSize(outboundMaxPoolSize)
            .queueCapacity(3000)               // 큐 크기 대폭 증가
            .keepAliveSeconds(60)              // 유휴 스레드 유지 시간

        logger.info { "WebSocket 아웃바운드 채널 스레드 풀 설정: core=$outboundCorePoolSize, max=$outboundMaxPoolSize, queue=3000" }
    }

    /**
     * CPU 코어 수에 기반한 코어 풀 크기 계산
     * @param multiplier 코어 수에 곱할 승수
     * @return 계산된 코어 풀 크기
     */
    private fun calculateCorePoolSize(multiplier: Int): Int {
        return availableProcessors * multiplier
    }

    /**
     * CPU 코어 수에 기반한 최대 풀 크기 계산
     * @param multiplier 코어 수에 곱할 승수
     * @return 계산된 최대 풀 크기
     */
    private fun calculateMaxPoolSize(multiplier: Int): Int {
        return availableProcessors * multiplier
    }

    /**
     * Heartbeat 처리를 위한 최적화된 TaskScheduler
     */
    @Bean
    fun heartbeatScheduler(): TaskScheduler {
        return ThreadPoolTaskScheduler().apply {
            poolSize = 8 // heartbeat 처리용 스레드 풀 대폭 증가
            setThreadNamePrefix("ws-heartbeat-")
            setAwaitTerminationSeconds(5)
            setWaitForTasksToCompleteOnShutdown(true)
            setErrorHandler { t -> logger.error(t) { "Heartbeat 스케줄러 오류" } }
            initialize()
        }
    }

    /**
     * WebSocket 서버 컨테이너 설정 최적화
     */
    @Bean
    fun createWebSocketContainer(): ServletServerContainerFactoryBean {
        return ServletServerContainerFactoryBean().apply {
            setMaxTextMessageBufferSize(128 * 1024) // 128KB로 증가
            setMaxBinaryMessageBufferSize(128 * 1024) // 128KB로 증가
            setMaxSessionIdleTimeout(TimeUnit.MINUTES.toMillis(20)) // 20분으로 증가
            setAsyncSendTimeout(10000) // 10초로 증가
        }
    }
}
