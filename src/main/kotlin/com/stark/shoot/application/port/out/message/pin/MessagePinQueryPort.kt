package com.stark.shoot.application.port.out.message.pin

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.pin.MessagePin
import com.stark.shoot.domain.chat.pin.vo.MessagePinId
import com.stark.shoot.domain.chat.vo.ChatRoomId

/**
 * 메시지 고정 조회 포트
 *
 * MessagePin Aggregate의 조회 작업을 담당하는 outbound port입니다.
 */
interface MessagePinQueryPort {
    /**
     * ID로 메시지 고정 정보를 조회합니다.
     *
     * @param id 메시지 고정 ID
     * @return 메시지 고정 정보 (없으면 null)
     */
    fun findById(id: MessagePinId): MessagePin?

    /**
     * 메시지 ID로 고정 정보를 조회합니다.
     *
     * @param messageId 메시지 ID
     * @return 메시지 고정 정보 (없으면 null)
     */
    fun findByMessageId(messageId: MessageId): MessagePin?

    /**
     * 채팅방의 모든 고정 메시지를 조회합니다.
     *
     * @param roomId 채팅방 ID
     * @param limit 최대 조회 개수
     * @return 메시지 고정 정보 목록
     */
    fun findAllByRoomId(roomId: ChatRoomId, limit: Int = Int.MAX_VALUE): List<MessagePin>

    /**
     * 채팅방의 고정된 메시지 개수를 조회합니다.
     *
     * @param roomId 채팅방 ID
     * @return 고정된 메시지 개수
     */
    fun countByRoomId(roomId: ChatRoomId): Long

    /**
     * 메시지가 고정되어 있는지 확인합니다.
     *
     * @param messageId 메시지 ID
     * @return 고정 여부
     */
    fun isPinned(messageId: MessageId): Boolean
}
