package com.stark.shoot.application.port.out.message.readreceipt

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.readreceipt.MessageReadReceipt
import com.stark.shoot.domain.chat.readreceipt.vo.MessageReadReceiptId
import com.stark.shoot.domain.chat.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId

/**
 * 메시지 읽음 표시 조회 포트
 *
 * MessageReadReceipt Aggregate의 조회 작업을 담당하는 outbound port입니다.
 */
interface MessageReadReceiptQueryPort {
    /**
     * ID로 읽음 표시를 조회합니다.
     *
     * @param id 읽음 표시 ID
     * @return 읽음 표시 (없으면 null)
     */
    fun findById(id: MessageReadReceiptId): MessageReadReceipt?

    /**
     * 메시지 ID와 사용자 ID로 읽음 표시를 조회합니다.
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @return 읽음 표시 (없으면 null)
     */
    fun findByMessageIdAndUserId(messageId: MessageId, userId: UserId): MessageReadReceipt?

    /**
     * 특정 메시지의 모든 읽음 표시를 조회합니다.
     *
     * @param messageId 메시지 ID
     * @return 읽음 표시 목록
     */
    fun findAllByMessageId(messageId: MessageId): List<MessageReadReceipt>

    /**
     * 특정 채팅방의 모든 읽음 표시를 조회합니다.
     *
     * @param roomId 채팅방 ID
     * @return 읽음 표시 목록
     */
    fun findAllByRoomId(roomId: ChatRoomId): List<MessageReadReceipt>

    /**
     * 메시지를 읽은 사용자 수를 조회합니다.
     *
     * @param messageId 메시지 ID
     * @return 읽은 사용자 수
     */
    fun countByMessageId(messageId: MessageId): Long

    /**
     * 사용자가 메시지를 읽었는지 확인합니다.
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @return 읽음 여부
     */
    fun hasRead(messageId: MessageId, userId: UserId): Boolean

    /**
     * 메시지별 읽은 사용자 ID 목록을 Map으로 조회합니다.
     *
     * @param messageId 메시지 ID
     * @return Map<UserId, Boolean> (읽음 여부)
     */
    fun getReadByMap(messageId: MessageId): Map<UserId, Boolean>
}
