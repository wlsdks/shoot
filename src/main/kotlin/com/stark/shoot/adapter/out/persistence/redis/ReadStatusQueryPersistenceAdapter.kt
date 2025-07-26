package com.stark.shoot.adapter.out.persistence.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.rest.dto.message.read.ReadStatus
import com.stark.shoot.application.port.out.message.readstatus.ReadStatusQueryPort
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.Adapter
import org.springframework.data.redis.core.StringRedisTemplate

@Adapter
class ReadStatusQueryPersistenceAdapter(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) : ReadStatusQueryPort {

    companion object {
        private const val KEY_PREFIX = "read:status:"
    }

    override fun findByRoomIdAndUserId(
        roomId: ChatRoomId,
        userId: UserId
    ): ReadStatus? {
        val key = getKey(roomId, userId)
        val value = redisTemplate.opsForValue().get(key) ?: return null
        return objectMapper.readValue(value, ReadStatus::class.java)
    }

    override fun findAllByRoomId(roomId: ChatRoomId): List<ReadStatus> {
        val pattern = "$KEY_PREFIX$roomId:*"
        return redisTemplate.keys(pattern).mapNotNull { key ->
            val value = redisTemplate.opsForValue().get(key) ?: return@mapNotNull null
            objectMapper.readValue(value, ReadStatus::class.java)
        }
    }

    private fun getKey(
        roomId: ChatRoomId,
        userId: UserId
    ): String = "$KEY_PREFIX${roomId.value}:${userId.value}"

}