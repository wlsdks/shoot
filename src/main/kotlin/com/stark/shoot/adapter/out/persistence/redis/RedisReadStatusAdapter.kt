package com.stark.shoot.adapter.out.persistence.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.message.read.ReadStatus
import com.stark.shoot.application.port.out.message.ReadStatusPort
import com.stark.shoot.domain.chat.room.vo.ChatRoomId
import com.stark.shoot.domain.common.vo.MessageId
import com.stark.shoot.domain.common.vo.UserId
import com.stark.shoot.infrastructure.annotation.Adapter
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Instant

@Adapter
class RedisReadStatusAdapter(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) : ReadStatusPort {

    companion object {
        private const val KEY_PREFIX = "read:status:"
    }

    private fun getKey(roomId: ChatRoomId, userId: UserId): String = "$KEY_PREFIX${roomId.value}:${userId.value}"

    override fun save(readStatus: ReadStatus): ReadStatus {
        val key = getKey(readStatus.roomId, readStatus.userId)
        val value = objectMapper.writeValueAsString(readStatus)
        redisTemplate.opsForValue().set(key, value)
        return readStatus
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

    override fun updateLastReadMessageId(
        roomId: ChatRoomId,
        userId: UserId,
        messageId: MessageId
    ): ReadStatus {
        val currentStatus = findByRoomIdAndUserId(roomId, userId)
            ?: ReadStatus.Companion.create(roomId, userId)

        val updatedStatus = currentStatus.markAsRead(messageId)
        return save(updatedStatus)
    }

    override fun incrementUnreadCount(
        roomId: ChatRoomId,
        userId: UserId
    ): ReadStatus {
        val currentStatus = findByRoomIdAndUserId(roomId, userId)
            ?: ReadStatus.Companion.create(roomId, userId)

        val updatedStatus = currentStatus.incrementUnreadCount()
        return save(updatedStatus)
    }

    override fun resetUnreadCount(
        roomId: ChatRoomId,
        userId: UserId
    ): ReadStatus {
        val currentStatus = findByRoomIdAndUserId(roomId, userId)
            ?: ReadStatus.Companion.create(roomId, userId)

        val updatedStatus = currentStatus.copy(
            unreadCount = 0, lastReadAt = Instant.now()
        )

        return save(updatedStatus)
    }

}