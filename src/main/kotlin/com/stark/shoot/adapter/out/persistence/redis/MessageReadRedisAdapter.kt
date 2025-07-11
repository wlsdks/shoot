package com.stark.shoot.adapter.out.persistence.redis

import com.stark.shoot.application.port.out.message.readstatus.MessageReadRedisPort
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.Adapter
import org.springframework.data.redis.connection.ReturnType
import org.springframework.data.redis.core.StringRedisTemplate
import java.util.concurrent.TimeUnit

@Adapter
class MessageReadRedisAdapter(
    private val redisTemplate: StringRedisTemplate
) : MessageReadRedisPort {

    companion object {
        // Lua 스크립트로 원자적 감소 처리
        private const val DECREMENT_UNREAD_COUNT_SCRIPT = """
            local current = tonumber(redis.call('hget', KEYS[1], ARGV[1]) or '0')
            if current > 0 then
                return redis.call('hincrby', KEYS[1], ARGV[1], -1)
            end
            return current
        """

        // Redis 키 만료 시간 (초)
        private const val REDIS_KEY_EXPIRATION_SECONDS = 30L

        // Redis 키 접두사
        private const val UNREAD_KEY_PREFIX = "unread:"
        private const val READ_OPERATION_KEY_PREFIX = "read_operation:"
    }

    /**
     * 읽지 않은 메시지 카운트를 원자적으로 감소시킵니다.
     *
     * @param userId 사용자 ID
     * @param roomId 채팅방 ID
     * @return 감소 후 남은 읽지 않은 메시지 수
     */
    override fun decrementUnreadCount(
        userId: UserId,
        roomId: ChatRoomId
    ): Long {
        val unreadKey = createUnreadKey(userId)
        val roomIdStr = roomId.toString()

        return redisTemplate.execute<Long> { connection ->
            connection.scriptingCommands().eval(
                DECREMENT_UNREAD_COUNT_SCRIPT.toByteArray(),
                ReturnType.INTEGER,
                1,
                unreadKey.toByteArray(),
                roomIdStr.toByteArray()
            )
        } ?: 0L
    }

    /**
     * 읽지 않은 메시지 카운트를 0으로 설정합니다.
     *
     * @param userId 사용자 ID
     * @param roomId 채팅방 ID
     * @return 작업 성공 여부
     */
    override fun resetUnreadCount(
        userId: UserId,
        roomId: ChatRoomId
    ): Boolean {
        val unreadKey = createUnreadKey(userId)
        val roomIdStr = roomId.toString()

        return redisTemplate.execute<Boolean> { connection ->
            connection.hashCommands().hSet(
                unreadKey.toByteArray(),
                roomIdStr.toByteArray(),
                "0".toByteArray()
            )
        } ?: false
    }

    /**
     * 중복 요청 방지를 위해 키를 설정합니다. 이미 존재하는 경우 false를 반환합니다.
     *
     * @param key 키
     * @param value 값
     * @param timeout 만료 시간
     * @param unit 시간 단위
     * @return 설정 성공 여부 (이미 존재하면 false)
     */
    override fun setIfAbsent(
        key: String,
        value: String,
        timeout: Long,
        unit: TimeUnit
    ): Boolean {
        return redisTemplate.opsForValue()
            .setIfAbsent(key, value, timeout, unit) ?: false
    }

    /**
     * 키가 존재하는지 확인합니다.
     *
     * @param key 키
     * @return 존재 여부
     */
    override fun hasKey(key: String): Boolean {
        return redisTemplate.hasKey(key)
    }

    /**
     * 키의 만료 시간을 설정합니다.
     *
     * @param key 키
     * @param timeout 만료 시간
     * @param unit 시간 단위
     * @return 설정 성공 여부
     */
    override fun setExpire(
        key: String,
        timeout: Long,
        unit: TimeUnit
    ): Boolean {
        return redisTemplate.expire(key, timeout, unit)
    }

    /**
     * 사용자의 읽지 않은 메시지 카운트를 저장하는 Redis 키를 생성합니다.
     *
     * @param userId 사용자 ID
     * @return Redis 키
     */
    override fun createUnreadKey(userId: UserId): String {
        val key = "$UNREAD_KEY_PREFIX${userId.value}"

        // 키가 존재하지 않는 경우에만 만료 시간 설정 (기존 데이터 유지)
        if (!hasKey(key)) {
            // 사용자 활동이 없는 경우를 대비해 7일 만료 시간 설정
            setExpire(key, 7, TimeUnit.DAYS)
        }

        return key
    }

    /**
     * 중복 읽기 요청을 방지하기 위한 Redis 키를 생성합니다.
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @param requestId 요청 ID
     * @return Redis 키
     */
    override fun createReadOperationKey(
        roomId: ChatRoomId,
        userId: UserId,
        requestId: String
    ): String = "$READ_OPERATION_KEY_PREFIX${roomId.value}:${userId.value}:$requestId"

}