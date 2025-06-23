package com.stark.shoot.adapter.out.redis.message

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import com.stark.shoot.application.port.out.message.PublishMessagePort
import com.stark.shoot.infrastructure.annotation.Adapter
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.core.StringRedisTemplate

@Adapter
class PublishMessageRedisAdapter(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) : PublishMessagePort {

    override suspend fun publish(message: ChatMessageRequest) {
        val streamKey = "stream:chat:room:${'$'}{message.roomId}"
        val messageJson = objectMapper.writeValueAsString(message)
        val map = mapOf("message" to messageJson)
        val record = StreamRecords.newRecord().ofMap(map).withStreamKey(streamKey)
        redisTemplate.opsForStream<String, String>().add(record)
    }
}
