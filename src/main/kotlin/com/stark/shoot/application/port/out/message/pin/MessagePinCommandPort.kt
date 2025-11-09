package com.stark.shoot.application.port.out.message.pin

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.pin.MessagePin
import com.stark.shoot.domain.chat.pin.vo.MessagePinId
import com.stark.shoot.domain.chat.vo.ChatRoomId

/**
 * 메시지 고정 명령 포트
 *
 * MessagePin Aggregate의 저장/삭제 작업을 담당하는 outbound port입니다.
 */
interface MessagePinCommandPort {
    /**
     * 메시지 고정 정보를 저장합니다.
     *
     * @param messagePin 저장할 메시지 고정 정보
     * @return 저장된 메시지 고정 정보
     */
    fun save(messagePin: MessagePin): MessagePin

    /**
     * 메시지 고정 정보를 삭제합니다.
     *
     * @param id 삭제할 메시지 고정 ID
     */
    fun delete(id: MessagePinId)

    /**
     * 특정 메시지의 고정 정보를 삭제합니다.
     *
     * @param messageId 메시지 ID
     */
    fun deleteByMessageId(messageId: MessageId)

    /**
     * 특정 채팅방의 모든 메시지 고정 정보를 삭제합니다.
     *
     * @param roomId 채팅방 ID
     */
    fun deleteAllByRoomId(roomId: ChatRoomId)
}
