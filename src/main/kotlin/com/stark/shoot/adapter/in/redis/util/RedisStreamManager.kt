package com.stark.shoot.adapter.`in`.redis.util

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.connection.stream.*
import org.springframework.data.redis.core.ScanOptions
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.nio.charset.Charset
import java.time.Duration
import java.util.*

/**
 * Redis Stream 관리를 위한 유틸리티 클래스
 *
 * 이 클래스는 Redis Stream의 생성, 소비자 그룹 관리, 메시지 폴링 등의 기능을 제공합니다.
 */
@Component
class RedisStreamManager(
    private val redisTemplate: StringRedisTemplate
) {
    private val logger = KotlinLogging.logger {}

    // 각 서버 인스턴스에 고유한 소비자 ID 생성 (여러 서버를 고려)
    private val consumerId = UUID.randomUUID().toString()

    companion object {
        private const val DEFAULT_CONSUMER_GROUP = "chat-consumers"
        private const val DEFAULT_STREAM_KEY_PATTERN = "stream:chat:room:*"
    }

    /**
     * Redis Stream의 소비자 그룹을 생성합니다.
     *
     * @param pattern 스트림 키 패턴 (기본값: stream:chat:room:*)
     * @param consumerGroup 소비자 그룹 이름 (기본값: chat-consumers)
     */
    fun createConsumerGroups(
        pattern: String = DEFAULT_STREAM_KEY_PATTERN,
        consumerGroup: String = DEFAULT_CONSUMER_GROUP
    ) {
        val streamKeys = scanStreamKeys(pattern)
        if (streamKeys.isEmpty()) {
            logger.info { "채팅방 스트림이 없습니다." }
            return
        }
        
        streamKeys.forEach { streamKey ->
            try {
                ensureStreamExists(streamKey)
                createConsumerGroup(streamKey, consumerGroup)
            } catch (e: Exception) {
                logger.error(e) { "Stream initialization error for: $streamKey" }
            }
        }
    }

    /**
     * 지정한 스트림이 존재하지 않으면 빈 스트림을 생성합니다.
     *
     * @param streamKey 스트림 키
     */
    fun ensureStreamExists(streamKey: String) {
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
     *
     * @param streamKey 스트림 키
     * @param consumerGroup 소비자 그룹 이름
     */
    fun createConsumerGroup(
        streamKey: String,
        consumerGroup: String = DEFAULT_CONSUMER_GROUP
    ) {
        try {
            redisTemplate.opsForStream<Any, Any>().createGroup(streamKey, consumerGroup)
            logger.info { "Created consumer group for: $streamKey" }
        } catch (e: Exception) {
            if (e.message?.contains("BUSYGROUP") != true) {
                logger.warn(e) { "소비자 그룹 생성 오류: $streamKey" }
            }
        }
    }

    /**
     * Redis Stream에서 키를 스캔하여 해당 패턴에 맞는 모든 키를 반환합니다.
     *
     * @param pattern 검색할 키 패턴
     * @return 패턴에 맞는 모든 키의 집합
     */
    fun scanStreamKeys(pattern: String): Set<String> {
        val keys = mutableSetOf<String>()
        val scanOptions = ScanOptions.scanOptions().match(pattern).count(100).build()

        val cursorFactory = redisTemplate.connectionFactory?.connection?.keyCommands()?.scan(scanOptions)

        cursorFactory?.let { cursor ->
            while (cursor.hasNext()) {
                val key = String(cursor.next(), Charset.defaultCharset())
                keys.add(key)
            }
        }

        return keys
    }

    /**
     * Redis Stream에서 메시지를 읽습니다.
     *
     * @param streamKey 스트림 키
     * @param consumerGroup 소비자 그룹 이름
     * @param count 한 번에 읽을 최대 메시지 수
     * @param blockTime 메시지가 없을 경우 대기할 시간
     * @return 읽은 메시지 목록
     */
    fun readMessages(
        streamKey: String,
        consumerGroup: String = DEFAULT_CONSUMER_GROUP,
        count: Long = 10,
        blockTime: Duration = Duration.ofMillis(100)
    ): List<MapRecord<String, String, Any>> {
        val readOptions = StreamReadOptions.empty()
            .count(count)
            .block(blockTime)

        val consumerOptions = Consumer.from(consumerGroup, consumerId)

        return try {
            val messages = redisTemplate.opsForStream<String, Any>()
                .read(consumerOptions, readOptions, StreamOffset.create(streamKey, ReadOffset.lastConsumed()))
            
            messages?.toList() ?: emptyList()
        } catch (e: Exception) {
            if (e.message?.contains("NOGROUP") == true) {
                logger.warn { "소비자 그룹이 없음: $streamKey - 재생성 시도" }
                try {
                    createConsumerGroup(streamKey, consumerGroup)
                } catch (ex: Exception) {
                    logger.error { "소비자 그룹 재생성 실패: $streamKey - ${ex.message}" }
                }
            } else {
                logger.error { "스트림 처리 오류: $streamKey - ${e.message}" }
            }
            emptyList()
        }
    }

    /**
     * 메시지를 처리 완료로 표시합니다 (ACK).
     *
     * @param streamKey 스트림 키
     * @param consumerGroup 소비자 그룹 이름
     * @param messageId 메시지 ID
     * @return 성공 여부
     */
    fun acknowledgeMessage(
        streamKey: String,
        consumerGroup: String = DEFAULT_CONSUMER_GROUP,
        messageId: RecordId
    ): Boolean {
        return try {
            redisTemplate.opsForStream<String, Any>()
                .acknowledge(consumerGroup, streamKey, messageId)
            true
        } catch (e: Exception) {
            logger.error { "메시지 ACK 오류 (ID: $messageId): ${e.message}" }
            false
        }
    }

    /**
     * 현재 인스턴스의 소비자 ID를 반환합니다.
     *
     * @return 소비자 ID
     */
    fun getConsumerId(): String {
        return consumerId
    }
}