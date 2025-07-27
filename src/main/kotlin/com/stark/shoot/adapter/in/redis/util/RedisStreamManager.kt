package com.stark.shoot.adapter.`in`.redis.util

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.connection.RedisConnection
import org.springframework.data.redis.connection.stream.*
import org.springframework.data.redis.core.ScanOptions
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.pow

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

    // 스트림 키 캐시 (패턴별로 캐싱)
    private val streamKeysCache = ConcurrentHashMap<String, Pair<Set<String>, Long>>()

    // 소비자 그룹 존재 여부 캐시 (스트림키:그룹명 -> 존재여부)
    private val consumerGroupExistsCache = ConcurrentHashMap<String, Boolean>()

    companion object {
        private const val DEFAULT_CONSUMER_GROUP = "chat-consumers"
        private const val DEFAULT_STREAM_KEY_PATTERN = "stream:chat:room:*"
        private const val CACHE_TTL_MS = 30_000L // 30초 캐시 유효 시간
    }

    /**
     * Redis Stream의 소비자 그룹을 생성합니다.
     * 여러 스트림에 대해 일괄적으로 소비자 그룹을 생성합니다.
     *
     * @param pattern 스트림 키 패턴 (기본값: stream:chat:room:*)
     * @param consumerGroup 소비자 그룹 이름 (기본값: chat-consumers)
     * @param useCache 캐시 사용 여부 (기본값: true)
     * @return 성공적으로 처리된 스트림 수
     */
    fun createConsumerGroups(
        pattern: String = DEFAULT_STREAM_KEY_PATTERN,
        consumerGroup: String = DEFAULT_CONSUMER_GROUP,
        useCache: Boolean = true
    ): Int {
        // 패턴에 맞는 스트림 키를 스캔하여 가져옵니다.
        val streamKeys = scanStreamKeys(pattern, useCache)

        // 스트림 키가 없으면 로그를 남기고 0 반환
        if (streamKeys.isEmpty()) {
            logger.info { "채팅방 스트림이 없습니다. 패턴: $pattern" }
            return 0
        }

        var successCount = 0
        streamKeys.forEach { streamKey ->
            try {
                // 각 스트림 키에 대해 스트림이 존재하는지 확인하고, 없으면 생성 후 소비자 그룹 생성
                val streamExists = ensureStreamExists(streamKey)

                // 스트림이 존재하면 소비자 그룹 생성 시도
                if (streamExists) {
                    val groupCreated = createConsumerGroup(streamKey, consumerGroup, useCache)

                    // 소비자 그룹이 성공적으로 생성되면 카운트 증가
                    if (groupCreated) {
                        successCount++
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "Stream initialization error for: $streamKey - ${e.message}" }
            }
        }

        logger.info { "소비자 그룹 생성 완료: $successCount/${streamKeys.size} 스트림 처리됨" }
        return successCount
    }

    /**
     * 지정한 스트림이 존재하지 않으면 빈 스트림을 생성합니다.
     *
     * @param streamKey 스트림 키
     * @return 스트림이 존재하거나 성공적으로 생성되었는지 여부
     */
    fun ensureStreamExists(streamKey: String): Boolean {
        try {
            // 스트림이 이미 존재하는지 확인 (존재하면 true 반환)
            if (redisTemplate.hasKey(streamKey)) {
                return true
            }

            // 스트림이 존재하지 않으면 초기 레코드와 함께 스트림을 생성
            val record = StreamRecords.newRecord()
                .ofMap(mapOf("init" to "true"))  // 초기화 표시용 더미 데이터
                .withStreamKey(streamKey)

            // 스트림에 초기 레코드를 추가하여 스트림을 생성 (레코드 ID 반환)
            val recordId = redisTemplate.opsForStream<String, String>().add(record)
            logger.info { "Created empty stream: $streamKey, initial record ID: $recordId" }
            return true
        } catch (e: Exception) {
            logger.error(e) { "스트림 생성 실패: $streamKey - ${e.message}" }
            return false
        }
    }

    /**
     * 지정한 스트림에 대해 소비자 그룹을 생성합니다.
     * 이미 존재하는 소비자 그룹은 다시 생성하지 않습니다.
     * 결과는 캐싱되어 성능을 향상시킵니다.
     *
     * @param streamKey 스트림 키
     * @param consumerGroup 소비자 그룹 이름
     * @param useCache 캐시 사용 여부 (기본값: true)
     * @return 생성 성공 여부
     */
    fun createConsumerGroup(
        streamKey: String,
        consumerGroup: String = DEFAULT_CONSUMER_GROUP,
        useCache: Boolean = true
    ): Boolean {
        // 캐시 키 생성
        val cacheKey = "$streamKey:$consumerGroup"

        // 캐시 확인 (이미 존재하는 소비자 그룹인지 확인) -> 캐시가 활성화되어 있고, 해당 키가 캐시에 존재하면 true 반환
        if (useCache && consumerGroupExistsCache[cacheKey] == true) {
            logger.debug { "Consumer group already exists (from cache): $streamKey:$consumerGroup" }
            return true
        }

        try {
            // Redis에서 지정된 스트림의 모든 소비자 그룹 목록을 조회
            val consumerGroups = redisTemplate.opsForStream<Any, Any>().groups(streamKey)

            // 조회된 그룹 목록에서 생성하려는 그룹명이 이미 존재하는지 확인 (null 안전성 처리 완료)
            val groupExists = consumerGroups?.any { it.groupName() == consumerGroup } ?: false

            // 소비자 그룹이 이미 존재하는 경우 로그 출력 및 캐시에 저장
            if (groupExists) {
                logger.debug { "Consumer group already exists: $streamKey:$consumerGroup" }
                if (useCache) {
                    // 캐시에 존재 여부를 저장하여 다음 호출 시 Redis 조회 생략
                    consumerGroupExistsCache[cacheKey] = true
                }
                return true
            }

            // 소비자 그룹이 존재하지 않으면 새로 생성
            redisTemplate.opsForStream<Any, Any>().createGroup(streamKey, consumerGroup)
            logger.info { "Created consumer group: $streamKey:$consumerGroup" }

            // 생성 성공 시 캐시에 존재 여부를 저장
            if (useCache) {
                consumerGroupExistsCache[cacheKey] = true
            }

            // 성공적으로 생성되었음을 반환
            return true
        } catch (e: Exception) {
            // 예외 처리: BUSYGROUP 오류는 이미 그룹이 존재한다는 의미
            if (e.message?.contains("BUSYGROUP") == true) {
                logger.debug { "Consumer group already exists (BUSYGROUP): $streamKey:$consumerGroup" }

                // 캐시에 존재 여부를 저장하여 다음 호출 시 Redis 조회 생략
                if (useCache) {
                    consumerGroupExistsCache[cacheKey] = true
                }
                return true
            } else {
                logger.warn(e) { "소비자 그룹 생성 오류: $streamKey:$consumerGroup - ${e.message}" }
                return false
            }
        }
    }

    /**
     * Redis Stream에서 키를 스캔하여 해당 패턴에 맞는 모든 키를 반환합니다.
     * 결과는 캐싱되어 성능을 향상시킵니다.
     *
     * @param pattern 검색할 키 패턴
     * @param useCache 캐시 사용 여부 (기본값: true)
     * @return 패턴에 맞는 모든 키의 집합
     */
    fun scanStreamKeys(
        pattern: String,
        useCache: Boolean = true
    ): Set<String> {
        // 캐시 사용이 활성화되어 있고, 캐시에 해당 패턴의 결과가 있으면 캐시된 결과 반환
        if (useCache) {
            val cachedResult = streamKeysCache[pattern]
            val currentTime = System.currentTimeMillis()

            if (cachedResult != null &&
                (currentTime - cachedResult.second) < CACHE_TTL_MS
            ) {
                logger.debug { "Using cached stream keys for pattern: $pattern (${cachedResult.first.size} keys)" }
                return cachedResult.first
            }
        }

        // Redis에서 패턴에 맞는 키들을 저장할 집합 생성
        val keys = mutableSetOf<String>()

        // Redis SCAN 명령어 옵션 설정: 패턴 매칭과 한 번에 스캔할 키 개수(100개) 지정
        val scanOptions = ScanOptions.scanOptions().match(pattern).count(100).build()

        // Redis 연결 객체를 저장할 변수 초기화 (바로 다음 줄에서 연결을 얻음)
        var connection: RedisConnection? = null

        try {
            // Redis 연결 팩토리에서 직접 연결 객체를 가져옴
            connection = redisTemplate.connectionFactory?.connection

            // 연결의 키 명령어를 통해 SCAN 커서를 생성 (앞서 설정한 scanOptions 사용)
            val cursorFactory = connection?.keyCommands()?.scan(scanOptions)

            // 커서가 있으면 반복하며 키들을 수집
            cursorFactory?.let { cursor ->
                while (cursor.hasNext()) {
                    // 바이트 배열로 반환되는 키를 UTF-8 문자열로 변환
                    val key = String(cursor.next(), StandardCharsets.UTF_8)
                    // 변환된 키를 집합에 추가
                    keys.add(key)
                }
            }

            // 결과 캐싱
            if (useCache) {
                streamKeysCache[pattern] = Pair(keys, System.currentTimeMillis())
                logger.debug { "Cached ${keys.size} stream keys for pattern: $pattern" }
            }

            return keys
        } catch (e: Exception) {
            logger.error(e) { "Error scanning Redis keys with pattern: $pattern" }
            return emptySet()
        } finally {
            try {
                // 연결 반환 (Spring이 자동으로 처리하지만 명시적으로 처리)
                connection?.close()
            } catch (e: Exception) {
                logger.warn(e) { "Error closing Redis connection" }
            }
        }
    }

    /**
     * Redis Stream에서 메시지를 읽습니다.
     * 스트림과 소비자 그룹이 없으면 자동으로 생성합니다.
     *
     * @param streamKey 스트림 키
     * @param consumerGroup 소비자 그룹 이름
     * @param count 한 번에 읽을 최대 메시지 수
     * @param blockTime 메시지가 없을 경우 대기할 시간
     * @param retryCount 현재 재시도 횟수 (내부 재귀용)
     * @param maxRetries 최대 재시도 횟수
     * @param useCache 캐시 사용 여부
     * @return 읽은 메시지 목록
     */
    fun readMessages(
        streamKey: String,
        consumerGroup: String = DEFAULT_CONSUMER_GROUP,
        count: Long = 10,
        blockTime: Duration = Duration.ofMillis(100),
        retryCount: Int = 0,
        maxRetries: Int = 3,
        useCache: Boolean = true
    ): List<MapRecord<String, String, Any>> {
        try {
            // 1. 스트림과 소비자 그룹 준비 (존재하지 않으면 생성)
            prepareStreamAndConsumerGroup(streamKey, consumerGroup, useCache)

            // 2. 메시지 읽기 설정 (옵션 설정)
            val readOptions = StreamReadOptions.empty()
                .count(count)
                .block(blockTime)

            // 소비자 옵션 설정 (소비자 그룹과 현재 인스턴스의 소비자 ID 사용)
            val consumerOptions = Consumer.from(consumerGroup, consumerId)

            // 3. 메시지 읽기 시도
            return try {
                // lastConsumed()를 사용하여 마지막으로 읽은 메시지 이후의 메시지를 읽음
                val messages = redisTemplate.opsForStream<String, Any>()
                    .read(consumerOptions, readOptions, StreamOffset.create(streamKey, ReadOffset.lastConsumed()))

                // 읽은 메시지를 리스트로 변환 (null 체크 포함)
                val result = messages?.toList() ?: emptyList()

                // 읽은 메시지가 있다면 로그 출력
                if (result.isNotEmpty()) {
                    logger.debug { "Read ${result.size} messages from $streamKey" }
                }
                result
            } catch (e: Exception) {
                handleReadException(
                    e,
                    streamKey,
                    consumerGroup,
                    count,
                    blockTime,
                    readOptions,
                    consumerOptions,
                    useCache
                )
            }
        } catch (e: Exception) {
            return handleConnectionException(
                e,
                streamKey,
                consumerGroup,
                count,
                blockTime,
                retryCount,
                maxRetries,
                useCache
            )
        }
    }

    /**
     * 스트림과 소비자 그룹이 존재하는지 확인하고 필요시 생성합니다.
     */
    private fun prepareStreamAndConsumerGroup(
        streamKey: String,
        consumerGroup: String,
        useCache: Boolean
    ): Boolean {
        // 캐시 키 생성
        val cacheKey = "$streamKey:$consumerGroup"

        // 캐시에서 소비자 그룹 존재 여부 확인
        if (useCache && consumerGroupExistsCache[cacheKey] == true) {
            return true
        }

        // 스트림 존재 확인 및 생성
        if (!ensureStreamExists(streamKey)) {
            logger.warn { "스트림 생성 실패: $streamKey" }
            return false
        }

        // 소비자 그룹 생성
        return createConsumerGroup(streamKey, consumerGroup, useCache)
    }

    /**
     * 메시지 읽기 중 발생한 예외 처리
     */
    private fun handleReadException(
        e: Exception,
        streamKey: String,
        consumerGroup: String,
        count: Long,
        blockTime: Duration,
        readOptions: StreamReadOptions,
        consumerOptions: Consumer,
        useCache: Boolean
    ): List<MapRecord<String, String, Any>> {
        // NOGROUP 오류 처리 (소비자 그룹이 없는 경우)
        if (e.message?.contains("NOGROUP") == true) {
            logger.warn { "소비자 그룹이 없음: $streamKey - 재생성 시도" }

            // 캐시에서 해당 소비자 그룹 정보 제거
            val cacheKey = "$streamKey:$consumerGroup"
            consumerGroupExistsCache.remove(cacheKey)

            // 스트림과 소비자 그룹 재생성
            if (ensureStreamExists(streamKey) && createConsumerGroup(streamKey, consumerGroup, false)) {
                // 재생성 성공 시 다시 메시지 읽기 시도
                try {
                    val retryMessages = redisTemplate.opsForStream<String, Any>()
                        .read(consumerOptions, readOptions, StreamOffset.create(streamKey, ReadOffset.lastConsumed()))

                    return retryMessages?.toList() ?: emptyList()
                } catch (retryEx: Exception) {
                    logger.error(retryEx) { "재시도 후에도 스트림 처리 오류: $streamKey" }
                }
            } else {
                logger.error { "소비자 그룹 재생성 실패: $streamKey" }
            }
        } else {
            logger.error(e) { "스트림 처리 오류: $streamKey - ${e.message}" }
        }

        return emptyList()
    }

    /**
     * 연결 관련 예외 처리 및 재시도 로직
     */
    private fun handleConnectionException(
        e: Exception,
        streamKey: String,
        consumerGroup: String,
        count: Long,
        blockTime: Duration,
        retryCount: Int,
        maxRetries: Int,
        useCache: Boolean
    ): List<MapRecord<String, String, Any>> {
        // 연결 관련 오류 처리 (LettuceConnectionFactory was destroyed 등)
        val isConnectionError = e.message?.let { msg ->
            msg.contains("LettuceConnectionFactory was destroyed") ||
                    msg.contains("Connection closed") ||
                    msg.contains("Connection reset")
        } ?: false

        if (isConnectionError && retryCount < maxRetries) {
            // 지수 백오프로 재시도 간격 증가
            val backoffTime = (2.0.pow(retryCount.toDouble()) * 100).toLong()
            logger.warn { "Redis 연결 오류, ${backoffTime}ms 후 재시도 (${retryCount + 1}/$maxRetries): $streamKey" }

            try {
                // 비동기 방식으로 변경하면 더 좋을 수 있음
                TimeUnit.MILLISECONDS.sleep(backoffTime)
                return readMessages(streamKey, consumerGroup, count, blockTime, retryCount + 1, maxRetries, useCache)
            } catch (ie: InterruptedException) {
                Thread.currentThread().interrupt()
                logger.error(ie) { "재시도 중 인터럽트 발생" }
            }
        } else if (retryCount >= maxRetries) {
            logger.error(e) { "최대 재시도 횟수 초과 ($maxRetries): $streamKey" }
        } else {
            logger.error(e) { "예상치 못한 오류 발생: $streamKey - ${e.message}" }
        }

        return emptyList()
    }

    /**
     * 메시지를 처리 완료로 표시합니다 (ACK).
     * 메시지 처리가 완료되면 이 메서드를 호출하여 Redis Stream에서 해당 메시지가 처리되었음을 알립니다.
     *
     * @param streamKey 스트림 키
     * @param consumerGroup 소비자 그룹 이름
     * @param messageId 메시지 ID
     * @param retryCount 현재 재시도 횟수 (내부 재귀용)
     * @param maxRetries 최대 재시도 횟수
     * @return 성공 여부
     */
    fun acknowledgeMessage(
        streamKey: String,
        consumerGroup: String = DEFAULT_CONSUMER_GROUP,
        messageId: RecordId,
        retryCount: Int = 0,
        maxRetries: Int = 2
    ): Boolean {
        try {
            val result = redisTemplate.opsForStream<String, Any>()
                .acknowledge(consumerGroup, streamKey, messageId)

            // 결과가 0인 경우는 이미 처리되었거나 존재하지 않는 메시지로 간주
            if (result != null && result > 0) {
                logger.debug { "메시지 ACK 성공: $streamKey, $messageId, 처리된 메시지 수: $result" }
                return true
            } else if (result == 0L) {
                // 결과가 0인 경우는 이미 처리되었거나 존재하지 않는 메시지로 간주 (정상 케이스)
                logger.debug { "메시지가 이미 처리되었거나 존재하지 않음: $streamKey, $messageId" }
                return true
            } else {
                logger.warn { "메시지 ACK 실패 (결과: $result): $streamKey, $messageId" }
                return false
            }
        } catch (e: Exception) {
            // 연결 관련 오류인 경우 재시도
            val isConnectionError = e.message?.let { msg ->
                msg.contains("LettuceConnectionFactory was destroyed") ||
                        msg.contains("Connection closed") ||
                        msg.contains("Connection reset")
            } ?: false

            if (isConnectionError && retryCount < maxRetries) {
                // 지수 백오프로 재시도 간격 증가
                val backoffTime = (2.0.pow(retryCount.toDouble()) * 50).toLong()
                logger.warn { "ACK 중 연결 오류, ${backoffTime}ms 후 재시도 (${retryCount + 1}/$maxRetries): $messageId" }

                try {
                    TimeUnit.MILLISECONDS.sleep(backoffTime)
                    return acknowledgeMessage(streamKey, consumerGroup, messageId, retryCount + 1, maxRetries)
                } catch (ie: InterruptedException) {
                    Thread.currentThread().interrupt()
                    logger.error(ie) { "ACK 재시도 중 인터럽트 발생" }
                }
            } else {
                logger.error(e) { "메시지 ACK 오류 (ID: $messageId): ${e.message}" }
            }

            return false
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
