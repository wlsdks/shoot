package com.stark.shoot.adapter.out.redis.message

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import com.stark.shoot.application.port.out.message.PublishMessagePort
import com.stark.shoot.infrastructure.annotation.Adapter
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.connection.stream.StreamRecords

@Adapter
class RedisMessagePublishAdapter(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) : PublishMessagePort {

    private val logger = KotlinLogging.logger {}

    override suspend fun publish(message: ChatMessageRequest) {
        val streamKey = generateStreamKey(message.roomId)
        val messageJson = objectMapper.writeValueAsString(message)
        val map = mapOf("message" to messageJson)
        val record = StreamRecords.newRecord()
            .ofMap(map)
            .withStreamKey(streamKey)
        val id = redisTemplate.opsForStream<String, String>().add(record)
        logger.debug { "Redis Stream publish: key=$streamKey id=$id tempId=${message.tempId}" }
    }

    private fun generateStreamKey(roomId: Long): String {
        return "stream:chat:room:$roomId"
    }
}
