package com.stark.shoot.application.port.out.message.readstatus

import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import java.util.concurrent.TimeUnit

/**
 * Redis 작업을 위한 포트 인터페이스
 * MessageReadService에서 사용하는 Redis 작업을 추상화합니다.
 */
interface MessageReadRedisPort {
    /**
     * 읽지 않은 메시지 카운트를 원자적으로 감소시킵니다.
     *
     * @param userId 사용자 ID
     * @param roomId 채팅방 ID
     * @return 감소 후 남은 읽지 않은 메시지 수
     */
    fun decrementUnreadCount(userId: UserId, roomId: ChatRoomId): Long

    /**
     * 읽지 않은 메시지 카운트를 0으로 설정합니다.
     *
     * @param userId 사용자 ID
     * @param roomId 채팅방 ID
     * @return 작업 성공 여부
     */
    fun resetUnreadCount(userId: UserId, roomId: ChatRoomId): Boolean

    /**
     * 중복 요청 방지를 위해 키를 설정합니다. 이미 존재하는 경우 false를 반환합니다.
     *
     * @param key 키
     * @param value 값
     * @param timeout 만료 시간
     * @param unit 시간 단위
     * @return 설정 성공 여부 (이미 존재하면 false)
     */
    fun setIfAbsent(key: String, value: String, timeout: Long, unit: TimeUnit): Boolean

    /**
     * 키가 존재하는지 확인합니다.
     *
     * @param key 키
     * @return 존재 여부
     */
    fun hasKey(key: String): Boolean

    /**
     * 키의 만료 시간을 설정합니다.
     *
     * @param key 키
     * @param timeout 만료 시간
     * @param unit 시간 단위
     * @return 설정 성공 여부
     */
    fun setExpire(key: String, timeout: Long, unit: TimeUnit): Boolean

    /**
     * 사용자의 읽지 않은 메시지 카운트를 저장하는 Redis 키를 생성합니다.
     *
     * @param userId 사용자 ID
     * @return Redis 키
     */
    fun createUnreadKey(userId: UserId): String

    /**
     * 중복 읽기 요청을 방지하기 위한 Redis 키를 생성합니다.
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @param requestId 요청 ID
     * @return Redis 키
     */
    fun createReadOperationKey(roomId: ChatRoomId, userId: UserId, requestId: String): String
}