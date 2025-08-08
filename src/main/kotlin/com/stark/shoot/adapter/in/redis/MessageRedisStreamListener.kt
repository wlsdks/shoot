package com.stark.shoot.adapter.`in`.redis

import com.fasterxml.jackson.core.JsonProcessingException
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
    private var pollingJob: Job? = null

    companion object {
        private const val MESSAGE_FIELD = "message"
    }

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
                    handleConnectionError(e)
                } catch (e: Exception) {
                    handlePollingError(e)
                }
            }
        }
    }

    /**
     * Redis 연결 오류를 처리합니다.
     * 연결 실패 시 로그를 출력하고, 일정 시간 후 재시도합니다.
     *
     * @param e 발생한 RedisConnectionFailureException
     */
    private suspend fun handleConnectionError(e: RedisConnectionFailureException) {
        logger.error(e) { "Redis 연결 실패, ${errorRetryDelayMs}ms 후 재시도" }
        delay(errorRetryDelayMs)
    }

    /**
     * 폴링 중 발생한 오류를 처리합니다.
     * 예외가 발생하면 로그를 출력하고, 일정 시간 후 재시도합니다.
     *
     * @param e 발생한 예외
     */
    private suspend fun handlePollingError(e: Exception) {
        logger.error(e) { "Redis Stream 폴링 중 오류 발생, ${errorRetryDelayMs}ms 후 재시도" }
        delay(errorRetryDelayMs)
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
                processStreamKey(streamKey)
            }
        } catch (e: Exception) {
            handlePollMessagesError(e)
        }
    }

    /**
     * 주어진 Redis Stream 키에 대해 메시지를 읽고 처리합니다.
     *
     * @param streamKey Redis Stream 키
     */
    private fun processStreamKey(streamKey: String) {
        try {
            setupStreamInfrastructure(streamKey)
            val messages = redisStreamManager.readMessages(streamKey, consumerGroup)
            messages.forEach { message ->
                processMessageSafely(message, streamKey)
            }
        } catch (e: Exception) {
            logger.error(e) { "스트림 처리 중 오류 발생: $streamKey - ${e.message}" }
        }
    }

    /**
     * Redis Stream 키에 대한 인프라를 설정합니다.
     * 스트림이 존재하지 않으면 생성하고, 소비자 그룹을 설정합니다.
     *
     * @param streamKey Redis Stream 키
     */
    private fun setupStreamInfrastructure(streamKey: String) {
        redisStreamManager.ensureStreamExists(streamKey)
        redisStreamManager.createConsumerGroup(streamKey, consumerGroup)
    }

    /**
     * Redis Stream에서 읽은 메시지를 안전하게 처리합니다.
     * 메시지 처리 중 발생하는 예외를 잡아 로그로 출력하고, 메시지를 ACK 처리합니다.
     *
     * @param message Redis Stream에서 읽은 메시지 레코드
     * @param streamKey Redis Stream 키
     */
    private fun processMessageSafely(message: MapRecord<*, *, *>, streamKey: String) {
        try {
            processMessage(message)
            redisStreamManager.acknowledgeMessage(streamKey, consumerGroup, message.id)
        } catch (e: JsonProcessingException) {
            // 포이즌 메시지: JSON 파싱 실패 시 ACK하여 무한 재처리 방지
            logger.error(e) { "메시지 파싱 오류 (ID: ${message.id}): ${e.message}" }
            handlePoisonMessage(message, streamKey, "JSON 파싱 실패")
        } catch (e: Exception) {
            logger.error(e) { "메시지 처리 오류 (ID: ${message.id}, 스트림: $streamKey): ${e.message}" }
        }
    }

    /**
     * 포이즌 메시지를 처리합니다.
     * 파싱 불가능하거나 처리할 수 없는 메시지를 ACK하여 PEL에서 제거합니다.
     * 추후 DLQ(Dead Letter Queue) 기능이 추가되면 해당 로직으로 확장 가능합니다.
     *
     * @param message 처리 실패한 메시지 레코드
     * @param streamKey Redis Stream 키
     * @param reason 실패 사유
     */
    private fun handlePoisonMessage(message: MapRecord<*, *, *>, streamKey: String, reason: String) {
        try {
            // TODO: 추후 DLQ 기능 추가 시 여기에 moveToDeadLetter 로직 구현
            // redisStreamManager.moveToDeadLetter(streamKey, message)

            // 현재는 ACK만 수행하여 무한 재처리 방지
            redisStreamManager.acknowledgeMessage(streamKey, consumerGroup, message.id)
            logger.warn { "포이즌 메시지 ACK 처리 완료 (ID: ${message.id}, 사유: $reason)" }
        } catch (ackException: Exception) {
            logger.error(ackException) { "포이즌 메시지 ACK 실패 (ID: ${message.id}): ${ackException.message}" }
        }
    }

    /**
     * Redis Stream 폴링 중 발생한 오류를 처리합니다.
     * 예외 타입에 따라 적절한 로그 메시지를 출력합니다.
     *
     * @param e 발생한 예외
     */
    private suspend fun handlePollMessagesError(e: Exception) {
        // 예외의 원인을 확인
        val cause = e.cause ?: e

        // Redis 연결 오류 여부를 확인
        val isConnectionError =
            cause is RedisConnectionFailureException ||
                    cause.cause is RedisConnectionFailureException

        // 연결 오류 여부에 따라 다른 로그 메시지 출력
        if (isConnectionError) {
            logger.error(e) { "Redis Stream 폴링 중 연결 오류 발생, ${errorRetryDelayMs}ms 후 재시도" }
        } else {
            logger.error(e) { "Redis Stream 폴링 중 오류 발생, ${errorRetryDelayMs}ms 후 재시도" }
        }

        // 일정 시간 후 재시도
        delay(errorRetryDelayMs)
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
        val streamKey = record.stream.toString()
        val roomId = extractRoomId(streamKey) ?: return
        val messageValue = extractMessageValue(record) ?: return

        val success = redisMessageProcessor.processMessage(roomId, messageValue)

        if (success) {
            logger.debug { "메시지 처리 성공: 채팅방=$roomId, 메시지ID=${record.id}" }
        }
    }

    /**
     * Redis Stream 키에서 채팅방 ID를 추출합니다.
     *
     * @param streamKey Redis Stream 키 (예: "stream:chat:room:123")
     * @return 추출된 채팅방 ID 문자열, 추출 실패 시 null
     */
    private fun extractRoomId(streamKey: String): String? {
        return redisMessageProcessor.extractRoomIdFromStreamKey(streamKey) ?: run {
            logger.warn { "채팅방 ID를 추출할 수 없음: $streamKey" }
            null
        }
    }

    /**
     * Redis Stream 레코드에서 메시지 값을 추출합니다.
     *
     * @param record Redis Stream 레코드
     * @return 추출된 메시지 값 문자열, 추출 실패 시 null
     */
    private fun extractMessageValue(record: MapRecord<*, *, *>): String? {
        return record.value[MESSAGE_FIELD]?.toString() ?: run {
            logger.warn { "메시지 값이 없음: ${record.id}" }
            null
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
