package com.stark.shoot.application.port.out.message

import com.stark.shoot.domain.chat.message.vo.MessageId

interface MessageCommandPort : SaveMessagePort {
    /**
     * 메시지 삭제 (보상 트랜잭션용)
     *
     * @param messageId 삭제할 메시지 ID
     */
    fun delete(messageId: MessageId)
}