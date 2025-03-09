package com.stark.shoot.adapter.`in`.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.data.redis.connection.stream.*
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Redis Stream을 통한 메시지 수신 및 처리를 담당하는 리스너 클래스입니다.
 * Redis Stream에 저장된 채팅 메시지를 주기적으로 폴링하여 WebSocket을 통해 클라이언트에게 전달합니다.
 */
@Component
class RedisStreamListener(
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val objectMapper: ObjectMapper,
    private val redisTemplate: StringRedisTemplate
) {
    private val logger = KotlinLogging.logger {}
    private val executor = Executors.newSingleThreadScheduledExecutor()

    companion object {
        private val ROOM_ID_PATTERN = Regex("stream:chat:room:([^:]+)")
    }

    /**
     * 리스너 초기화 메서드로, 애플리케이션 시작 시 자동으로 실행됩니다.
     * Redis Stream 소비자 그룹을 생성하고 메시지 폴링 스케줄러를 시작합니다.
     */
    @PostConstruct
    fun init() {
        // 소비자 그룹 생성 (없으면)
        createConsumerGroups()

        // 주기적으로 Stream 폴링
        executor.scheduleAtFixedRate(
            { pollMessages() },
            0, 100, TimeUnit.MILLISECONDS
        )
    }

    /**
     * Redis Stream의 소비자 그룹을 생성합니다.
     * 채팅방별 스트림에 대해 'chat-consumers' 그룹을 생성하여 메시지 처리를 관리합니다.
     * 이미 그룹이 존재하는 경우(BUSYGROUP 오류)는 무시합니다.
     */
    private fun createConsumerGroups() {
        try {
            val streamKeys = redisTemplate.keys("stream:chat:room:*")
            streamKeys.forEach { streamKey ->
                try {
                    redisTemplate.opsForStream<Any, Any>()
                        .createGroup(streamKey, "chat-consumers")
                } catch (e: Exception) {
                    // 이미 존재하는 그룹은 무시
                    if (e.message != null && !e.message!!.contains("BUSYGROUP")) {
                        logger.warn(e) { "소비자 그룹 생성 오류: $streamKey" }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "소비자 그룹 초기화 오류" }
        }
    }

    /**
     * Redis Stream에서 새로운 메시지를 주기적으로 폴링합니다.
     * 각 채팅방별 스트림에서 최대 10개의 메시지를 읽고, 처리 후 확인(ACK)합니다.
     * 이 메서드는 executor에 의해 100ms 간격으로 실행됩니다.
     */
    private fun pollMessages() {
        try {
            val streamKeys = redisTemplate.keys("stream:chat:room:*")

            if (streamKeys.isEmpty()) return

            // 각 스트림에서 최대 10개 메시지를 읽음
            val readOptions = StreamReadOptions.empty()
                .count(10)
                .block(Duration.ofMillis(100))

            val consumerOptions = Consumer.from("chat-consumers", "consumer-1")

            // 각 스트림에 대해 별도로 처리
            for (key in streamKeys) {
                try {
                    // 명시적인 타입 선언으로 타입 불일치 문제 해결
                    val messages = redisTemplate.opsForStream<Any, Any>()
                        .read(consumerOptions, readOptions, StreamOffset.create(key, ReadOffset.lastConsumed()))

                    // 메시지가 없으면 다음 스트림으로
                    if (messages?.isEmpty() != false) continue

                    // 각 메시지 처리
                    for (message in messages) {
                        processMessage(message)

                        // 처리 완료된 메시지 승인 (ACK)
                        redisTemplate.opsForStream<Any, Any>()
                            .acknowledge("chat-consumers", key, message.id)
                    }
                } catch (e: Exception) {
                    logger.error(e) { "스트림 처리 오류: $key" }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Stream 메시지 폴링 오류" }
        }
    }

    /**
     * Redis Stream에서 읽은 개별 메시지를 처리합니다.
     * 메시지에서 채팅방 ID를 추출하고, JSON 형식의 메시지를 ChatMessageRequest 객체로 변환한 후
     * WebSocket을 통해 해당 채팅방의 구독자들에게 전달합니다.
     *
     * @param record Redis Stream에서 읽은 맵 형식의 레코드
     */
    private fun processMessage(record: MapRecord<*, *, *>) {
        try {
            // 안전한 타입 처리를 위해 toString() 사용
            val streamKey = record.stream.toString()
            val roomIdMatch = ROOM_ID_PATTERN.find(streamKey)
            val roomId = roomIdMatch?.groupValues?.getOrNull(1)

            if (roomId != null) {
                // 레코드 값을 안전하게 추출
                val messageValue = record.value["message"]?.toString() ?: return
                val chatMessage = objectMapper.readValue(messageValue, ChatMessageRequest::class.java)
                simpMessagingTemplate.convertAndSend("/topic/messages/$roomId", chatMessage)
            } else {
                logger.warn { "Could not extract roomId from stream key: $streamKey" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Redis Stream 메시지 처리 오류: ${e.message}" }
        }
    }

    /**
     * 애플리케이션 종료 시 리소스를 정리합니다.
     * 메시지 폴링을 담당하는 스케줄러를 안전하게 종료합니다.
     */
    @PreDestroy
    fun shutdown() {
        executor.shutdown()
    }

}