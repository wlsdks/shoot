package com.stark.shoot.adapter.`in`.redis

import com.stark.shoot.adapter.`in`.redis.util.RedisMessageProcessor
import com.stark.shoot.adapter.`in`.redis.util.RedisStreamManager
import com.stark.shoot.infrastructure.config.async.ApplicationCoroutineScope
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.stereotype.Component

/**
 * Redis Stream을 통한 메시지 수신 및 처리를 담당하는 리스너 클래스입니다.
 * 코루틴 기반으로 Redis Stream에 저장된 채팅 메시지를 주기적으로 폴링하여
 * WebSocket을 통해 클라이언트에게 전달합니다.
 */
@Component
class RedisStreamListener(
    private val redisStreamManager: RedisStreamManager,
    private val redisMessageProcessor: RedisMessageProcessor,
    private val appCoroutineScope: ApplicationCoroutineScope
) {
    private val logger = KotlinLogging.logger {}

    // 코루틴 Job 참조 (나중에 취소하기 위함)
    private var pollingJob: Job? = null

    companion object {
        private const val CONSUMER_GROUP = "chat-consumers"
        private const val STREAM_KEY_PATTERN = "stream:chat:room:*"
    }

    /**
     * 리스너 초기화 메서드로, 애플리케이션 시작 시 자동으로 실행됩니다.
     * Redis Stream 소비자 그룹을 생성하고 코루틴 기반 메시지 폴링을 시작합니다.
     */
    @PostConstruct
    fun init() {
        // 소비자 그룹 생성 (없으면)
        redisStreamManager.createConsumerGroups(STREAM_KEY_PATTERN, CONSUMER_GROUP)

        // 코루틴으로 주기적 폴링 시작
        startPolling()
    }

    /**
     * 코루틴을 사용한 메시지 폴링 시작
     */
    private fun startPolling() {
        pollingJob = appCoroutineScope.launch {
            logger.info { "Redis Stream 메시지 폴링 시작" }

            while (isActive) {
                try {
                    pollMessages()
                    delay(100) // 100ms 간격으로 폴링
                } catch (e: Exception) {
                    logger.error(e) { "Redis Stream 폴링 중 오류 발생" }
                    delay(1000) // 오류 발생 시 1초 대기 후 재시도
                }
            }
        }
    }

    /**
     * Redis Stream에서 새로운 메시지를 주기적으로 폴링합니다.
     */
    private suspend fun pollMessages() {
        val streamKeys = redisStreamManager.scanStreamKeys(STREAM_KEY_PATTERN)
        if (streamKeys.isEmpty()) return

        // 각 스트림에 대해 순차적으로 처리 (메시지 순서 보장)
        streamKeys.forEach { streamKey ->
            val messages = redisStreamManager.readMessages(streamKey, CONSUMER_GROUP)

            // 각 메시지 개별 처리 및 ACK
            messages.forEach { message ->
                try {
                    processMessage(message)
                    redisStreamManager.acknowledgeMessage(streamKey, CONSUMER_GROUP, message.id)
                } catch (e: Exception) {
                    logger.error { "메시지 처리 오류 (ID: ${message.id}): ${e.message}" }
                }
            }
        }
    }

    /**
     * Redis Stream에서 읽은 개별 메시지를 처리합니다.
     *
     * 메시지에서 채팅방 ID를 추출하고, JSON 형식의 메시지를 ChatMessageRequest 객체로 변환한 후
     * WebSocket을 통해 해당 채팅방의 구독자들에게 전달합니다.
     *
     * @param record Redis Stream에서 읽은 맵 형식의 레코드
     */
    private fun processMessage(record: MapRecord<*, *, *>) {
        // 안전한 타입 처리를 위해 toString() 사용
        val streamKey = record.stream.toString()
        val roomId = redisMessageProcessor.extractRoomIdFromStreamKey(streamKey) ?: return

        // 레코드 값을 안전하게 추출
        val messageValue = record.value["message"]?.toString() ?: return
        redisMessageProcessor.processMessage(roomId, messageValue)
    }

    /**
     * 애플리케이션 종료 시 리소스를 정리합니다.
     */
    @PreDestroy
    fun shutdown() {
        // 코루틴 작업 취소
        pollingJob?.cancel()
        logger.info { "Redis Stream 메시지 폴링 중단됨" }
    }
}
