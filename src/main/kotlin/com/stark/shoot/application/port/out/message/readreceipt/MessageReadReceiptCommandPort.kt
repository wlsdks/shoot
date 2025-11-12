package com.stark.shoot.application.port.out.message.readreceipt

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.readreceipt.MessageReadReceipt
import com.stark.shoot.domain.chat.readreceipt.vo.MessageReadReceiptId
import com.stark.shoot.domain.chat.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId

/**
 * 메시지 읽음 표시 명령 포트
 *
 * MessageReadReceipt Aggregate의 저장/삭제 작업을 담당하는 outbound port입니다.
 */
interface MessageReadReceiptCommandPort {
    /**
     * 메시지 읽음 표시를 저장합니다.
     *
     * @param readReceipt 저장할 읽음 표시
     * @return 저장된 읽음 표시
     */
    fun save(readReceipt: MessageReadReceipt): MessageReadReceipt

    /**
     * 읽음 표시가 없는 경우에만 저장합니다 (경쟁 조건 방지).
     * 이미 존재하는 경우 기존 레코드를 반환합니다.
     *
     * @param readReceipt 저장할 읽음 표시
     * @return 저장되거나 이미 존재하는 읽음 표시
     */
    fun saveIfNotExists(readReceipt: MessageReadReceipt): MessageReadReceipt

    /**
     * 메시지 읽음 표시를 삭제합니다.
     *
     * @param id 삭제할 읽음 표시 ID
     */
    fun delete(id: MessageReadReceiptId)

    /**
     * 특정 메시지의 특정 사용자 읽음 표시를 삭제합니다.
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     */
    fun deleteByMessageIdAndUserId(messageId: MessageId, userId: UserId)

    /**
     * 특정 메시지의 모든 읽음 표시를 삭제합니다.
     *
     * @param messageId 메시지 ID
     */
    fun deleteAllByMessageId(messageId: MessageId)

    /**
     * 특정 채팅방의 모든 읽음 표시를 삭제합니다.
     *
     * @param roomId 채팅방 ID
     */
    fun deleteAllByRoomId(roomId: ChatRoomId)
}
