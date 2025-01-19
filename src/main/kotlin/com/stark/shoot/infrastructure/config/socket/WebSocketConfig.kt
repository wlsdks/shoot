package com.stark.shoot.infrastructure.config.socket

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker // STOMP 메시징을 활성화 이로 인해 서버는 STOMP 프로토콜 형식의 메시지를 기대합니다.
class WebSocketConfig : WebSocketMessageBrokerConfigurer {

    /**
     * WebSocket Heartbeat 처리용 스케줄러 설정
     * - poolSize: 동시에 처리할 수 있는 heartbeat 스레드 수 (2개)
     * - threadNamePrefix: 스레드 식별을 위한 이름 접두사
     *
     * 동작 방식:
     * 1. 설정된 주기(10초)마다 연결된 모든 클라이언트에 heartbeat 전송
     * 2. 2개의 스레드가 번갈아가며 heartbeat 처리
     * 3. 클라이언트로부터 heartbeat 응답이 없으면 연결 종료 처리
     */
    @Bean
    fun heartbeatScheduler(): TaskScheduler {
        return ThreadPoolTaskScheduler().apply {
            poolSize = 2 // heartbeat 처리용 스레드 풀
            setThreadNamePrefix("ws-heartbeat-")
            initialize()
        }
    }

    /**
     * 메시지 브로커(중계자) 설정
     * - SimpleBroker 활성화 (/topic, /queue 경로)
     * - Heartbeat 설정:
     *   - 첫 번째 값: 서버 -> 클라이언트 heartbeat 주기 (10초)
     *   - 두 번째 값: 클라이언트 -> 서버 heartbeat 기대 시간 (10초)
     * - 애플리케이션 메시지 prefix 설정 (/app)
     *
     * 동작 방식:
     * 1. 서버는 10초마다 클라이언트에게 heartbeat 전송
     * 2. 서버는 10초 동안 클라이언트의 heartbeat가 없으면 연결 끊김으로 간주
     */
    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic", "/queue") // 클라이언트가 구독할 경로
            .setHeartbeatValue(longArrayOf(10000, 10000))                 // 서버->클라이언트, 클라이언트->서버 각각 10초
            .setTaskScheduler(heartbeatScheduler())                       // TaskScheduler 설정 추가
        registry.setApplicationDestinationPrefixes("/app")                // 클라이언트가 메시지를 전송할 경로
    }

    /**
     * STOMP WebSocket 연결 엔드포인트 설정
     * - /ws/chat 경로로 웹소켓 연결 접수
     * - AuthHandshakeInterceptor로 연결 시 인증 처리
     * - CORS 설정으로 크로스 도메인 접근 허용
     *
     * 연결 과정:
     * 1. 클라이언트 연결 요청
     * 2. HandshakeInterceptor에서 인증 처리
     * 3. 인증 성공 시 WebSocket 연결 수립
     */
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws/chat") // WebSocket 엔드포인트
            .addInterceptors(AuthHandshakeInterceptor()) // 인증 인터셉터 추가
            .setAllowedOriginPatterns("*") // CORS 문제 방지
    }

}
