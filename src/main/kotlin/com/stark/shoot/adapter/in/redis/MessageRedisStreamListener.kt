package com.stark.shoot.adapter.`in`.redis

import com.fasterxml.jackson.core.JsonParseException
import com.stark.shoot.adapter.`in`.redis.util.RedisMessageProcessor
import com.stark.shoot.adapter.`in`.redis.util.RedisStreamManager
import com.stark.shoot.infrastructure.config.async.ApplicationCoroutineScope
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.stereotype.Component

/**
 * Redis Stream을 통한 메시지 수신 및 처리를 담당하는 리스너 클래스입니다.
 * 코루틴 기반으로 Redis Stream에 저장된 채팅 메시지를 주기적으로 폴링하여
 * WebSocket을 통해 클라이언트에게 전달합니다.
 */
@Component
class MessageRedisStreamListener(
    private val redisStreamManager: RedisStreamManager,
    private val redisMessageProcessor: RedisMessageProcessor,
    private val appCoroutineScope: ApplicationCoroutineScope,
    @Value("\${app.redis-stream.polling-interval-ms:100}") private val pollingIntervalMs: Long = 100,
    @Value("\${app.redis-stream.error-retry-delay-ms:1000}") private val errorRetryDelayMs: Long = 1000,
    @Value("\${app.redis-stream.consumer-group:chat-consumers}") private val consumerGroup: String = "chat-consumers",
    @Value("\${app.redis-stream.stream-key-pattern:stream:chat:room:*}") private val streamKeyPattern: String = "stream:chat:room:*"
) {
    private val logger = KotlinLogging.logger {}

    // 코루틴 Job 참조 (나중에 취소하기 위함)
    private var pollingJob: Job? = null

    /**
     * 리스너 초기화 메서드로, 애플리케이션 시작 시 자동으로 실행됩니다.
     * Redis Stream 소비자 그룹을 생성하고 코루틴 기반 메시지 폴링을 시작합니다.
     */
    @PostConstruct
    fun init() {
        // 소비자 그룹 생성 (없으면)
        redisStreamManager.createConsumerGroups(streamKeyPattern, consumerGroup)

        // 코루틴으로 주기적 폴링 시작
        startPolling()
    }

    /**
     * 코루틴을 사용한 메시지 폴링 시작
     */
    private fun startPolling() {
        pollingJob = appCoroutineScope.launch {
            logger.info { "Redis Stream 메시지 폴링 시작 (간격: ${pollingIntervalMs}ms)" }

            while (isActive) {
                try {
                    pollMessages()
                    delay(pollingIntervalMs)
                } catch (e: RedisConnectionFailureException) {
                    logger.error(e) { "Redis 연결 실패, ${errorRetryDelayMs}ms 후 재시도" }
                    delay(errorRetryDelayMs)
                } catch (e: Exception) {
                    logger.error(e) { "Redis Stream 폴링 중 오류 발생, ${errorRetryDelayMs}ms 후 재시도" }
                    delay(errorRetryDelayMs)
                }
            }
        }
    }

    /**
     * Redis Stream에서 새로운 메시지를 주기적으로 폴링합니다.
     */
    private suspend fun pollMessages() {
        try {
            // 스트림 키 패턴에 맞는 모든 스트림 키를 검색
            val streamKeys = redisStreamManager.scanStreamKeys(streamKeyPattern)

            // 스트림 키가 없으면 종료
            if (streamKeys.isEmpty()) return

            // 각 스트림에 대해 순차적으로 처리 (메시지 순서 보장)
            streamKeys.forEach { streamKey ->
                try {
                    // 스트림이 존재하는지 확인하고 없으면 생성
                    redisStreamManager.ensureStreamExists(streamKey)

                    // 소비자 그룹이 존재하는지 확인하고 없으면 생성
                    redisStreamManager.createConsumerGroup(streamKey, consumerGroup)

                    // 스트림에서 메시지 읽기
                    val messages = redisStreamManager.readMessages(streamKey, consumerGroup)

                    // 각 메시지 개별 처리 및 ACK
                    messages.forEach { message ->
                        try {
                            // 메시지를 처리하고 WebSocket으로 전송
                            processMessage(message)

                            // 메시지 처리 성공 시 ACK
                            redisStreamManager.acknowledgeMessage(streamKey, consumerGroup, message.id)
                        } catch (e: JsonParseException) {
                            logger.error(e) { "메시지 파싱 오류 (ID: ${message.id}): ${e.message}" }
                        } catch (e: Exception) {
                            logger.error(e) { "메시지 처리 오류 (ID: ${message.id}, 스트림: $streamKey): ${e.message}" }
                        }
                    }
                } catch (e: Exception) {
                    // 개별 스트림 처리 중 오류 발생 시 다른 스트림은 계속 처리
                    logger.error(e) { "스트림 처리 중 오류 발생: $streamKey - ${e.message}" }
                }
            }
        } catch (e: Exception) {
            if (e.message?.contains("LettuceConnectionFactory was destroyed") == true ||
                e.message?.contains("Connection closed") == true
            ) {
                logger.error(e) { "Redis Stream 폴링 중 연결 오류 발생, ${errorRetryDelayMs}ms 후 재시도" }
            } else {
                logger.error(e) { "Redis Stream 폴링 중 오류 발생, ${errorRetryDelayMs}ms 후 재시도" }
            }
            // 오류 발생 시 지정된 시간 후 재시도 (상위 코루틴에서 처리)
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

        // 스트림 키에서 채팅방 ID 추출 (null일 경우 경고 로그 출력)
        val roomId = redisMessageProcessor.extractRoomIdFromStreamKey(streamKey) ?: run {
            logger.warn { "채팅방 ID를 추출할 수 없음: $streamKey" }
            return
        }

        // 레코드 값을 안전하게 추출 (null일 경우 경고 로그 출력)
        val messageValue = record.value["message"]?.toString() ?: run {
            logger.warn { "메시지 값이 없음: ${record.id}" }
            return
        }

        // 메시지 처리 및 WebSocket 전송
        val success = redisMessageProcessor.processMessage(roomId, messageValue)

        // 메시지 처리 성공 여부에 따라 로그 출력
        if (success) {
            logger.debug { "메시지 처리 성공: 채팅방=$roomId, 메시지ID=${record.id}" }
        }
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
