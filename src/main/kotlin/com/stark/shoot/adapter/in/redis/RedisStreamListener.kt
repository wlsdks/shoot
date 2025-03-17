package com.stark.shoot.adapter.`in`.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.UrlPreviewMapper
import com.stark.shoot.domain.chat.message.UrlPreview
import com.stark.shoot.infrastructure.config.async.ApplicationCoroutineScope
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.springframework.data.redis.connection.stream.*
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.*

/**
 * Redis Stream을 통한 메시지 수신 및 처리를 담당하는 리스너 클래스입니다.
 * 코루틴 기반으로 Redis Stream에 저장된 채팅 메시지를 주기적으로 폴링하여
 * WebSocket을 통해 클라이언트에게 전달합니다.
 */
@Component
class RedisStreamListener(
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val objectMapper: ObjectMapper,
    private val redisTemplate: StringRedisTemplate,
    private val urlPreviewMapper: UrlPreviewMapper,
    private val appCoroutineScope: ApplicationCoroutineScope
) {
    private val logger = KotlinLogging.logger {}

    // 코루틴 Job 참조 (나중에 취소하기 위함)
    private var pollingJob: Job? = null

    // 각 서버 인스턴스에 고유한 소비자 ID 생성 (여러 서버를 고려)
    private val consumerId = UUID.randomUUID().toString()

    companion object {
        private val ROOM_ID_PATTERN = Regex("stream:chat:room:([^:]+)")
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
        createConsumerGroups()

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
     * Redis Stream의 소비자 그룹을 생성합니다.
     */
    private fun createConsumerGroups() {
        val streamKeys = redisTemplate.keys(STREAM_KEY_PATTERN) ?: emptySet()
        if (streamKeys.isEmpty()) {
            logger.info { "채팅방 스트림이 없습니다." }
            return
        }
        streamKeys.forEach { streamKey ->
            try {
                ensureStreamExists(streamKey)
                createConsumerGroup(streamKey)
            } catch (e: Exception) {
                logger.error(e) { "Stream initialization error for: $streamKey" }
            }
        }
    }


    /**
     * 지정한 스트림이 존재하지 않으면 빈 스트림을 생성합니다.
     */
    private fun ensureStreamExists(streamKey: String) {
        if (!redisTemplate.hasKey(streamKey)) {
            redisTemplate.opsForStream<String, String>()
                .add(
                    // 빈 스트림 생성을 위해 초기화 레코드 추가
                    StreamRecords.newRecord()
                        .ofMap(mapOf("init" to "true"))
                        .withStreamKey(streamKey)
                )
            logger.info { "Created empty stream: $streamKey" }
        }
    }

    /**
     * 지정한 스트림에 대해 소비자 그룹을 생성합니다.
     */
    private fun createConsumerGroup(streamKey: String) {
        try {
            redisTemplate.opsForStream<Any, Any>().createGroup(streamKey, CONSUMER_GROUP)
            logger.info { "Created consumer group for: $streamKey" }
        } catch (e: Exception) {
            if (e.message?.contains("BUSYGROUP") != true) {
                logger.warn(e) { "소비자 그룹 생성 오류: $streamKey" }
            }
        }
    }


    /**
     * Redis Stream에서 새로운 메시지를 주기적으로 폴링합니다.
     */
    private suspend fun pollMessages() {
        val streamKeys = redisTemplate.keys(STREAM_KEY_PATTERN) ?: emptySet()
        if (streamKeys.isEmpty()) return

        // 각 스트림에서 최대 10개 메시지를 읽음
        val readOptions = StreamReadOptions.empty()
            .count(10)
            .block(Duration.ofMillis(100))

        // 소비자 옵션 생성 (각 서버 인스턴스별로 고유한 ID 사용)
        val consumerOptions = Consumer.from(CONSUMER_GROUP, consumerId)

        // 각 스트림에 대해 병렬적으로 처리할 수도 있지만, 메시지 순서 보장을 위해 순차적으로 처리
        streamKeys.forEach { key ->
            try {
                // 명시적인 타입 선언으로 타입 불일치 문제 해결
                val messages = redisTemplate.opsForStream<String, Any>()
                    .read(consumerOptions, readOptions, StreamOffset.create(key, ReadOffset.lastConsumed()))

                // 각 메시지 개별 처리 및 ACK
                messages?.forEach { message ->
                    try {
                        processMessage(message)
                        redisTemplate.opsForStream<String, Any>()
                            .acknowledge(CONSUMER_GROUP, key, message.id) // 개별 메시지 ACK
                    } catch (e: Exception) {
                        logger.error { "메시지 처리 오류 (ID: ${message.id}): ${e.message}" }
                    }
                }
            } catch (e: Exception) {
                if (e.message?.contains("NOGROUP") == true) {
                    logger.warn { "소비자 그룹이 없음: $key - 재생성 시도" }
                    try {
                        createConsumerGroup(key)
                    } catch (ex: Exception) {
                        logger.error { "소비자 그룹 재생성 실패: $key - ${ex.message}" }
                    }
                } else {
                    logger.error { "스트림 처리 오류: $key - ${e.message}" }
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
        val roomId = ROOM_ID_PATTERN.find(streamKey)?.groupValues?.getOrNull(1)
        if (roomId == null) {
            logger.warn { "Could not extract roomId from stream key: $streamKey" }
            return
        }

        // 레코드 값을 안전하게 추출
        val messageValue = record.value["message"]?.toString() ?: return
        val chatMessage = objectMapper.readValue(messageValue, ChatMessageRequest::class.java)

        // URL 미리보기가 메타데이터에 있는 경우 처리
        if (chatMessage.metadata.containsKey("urlPreview")) {
            runCatching {
                // String으로 저장된 UrlPreview 객체를 다시 역직렬화
                val previewJson = chatMessage.metadata["urlPreview"] as? String

                // URL 미리보기 정보를 클라이언트가 사용할 수 있는 DTO로 변환해서 추가
                previewJson?.let {
                    val urlPreview = objectMapper.readValue(it, UrlPreview::class.java)
                    chatMessage.content.urlPreview = urlPreviewMapper.domainToDto(urlPreview)
                }
            }.onFailure { e ->
                logger.warn { "URL 미리보기 정보 처리 실패: ${e.message}" }
            }
        }
        simpMessagingTemplate.convertAndSend("/topic/messages/$roomId", chatMessage)
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