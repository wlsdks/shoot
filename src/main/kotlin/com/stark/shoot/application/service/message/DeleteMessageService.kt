package com.stark.shoot.application.service.message

import com.stark.shoot.application.port.`in`.message.DeleteMessageUseCase
import com.stark.shoot.application.port.out.message.MessageCommandPort
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.infrastructure.annotation.UseCase

@UseCase
class DeleteMessageService(
    private val messageQueryPort: MessageQueryPort,
    private val messageCommandPort: MessageCommandPort
) : DeleteMessageUseCase {

    /**
     * @apiNote 메시지 삭제
     * @param messageId 삭제할 메시지 ID
     */
    override fun deleteMessage(messageId: MessageId): ChatMessage {
        // 메시지 로드
        val existingMessage = messageQueryPort.findById(messageId)
            ?: throw IllegalArgumentException("메시지를 찾을 수 없습니다. messageId=$messageId")

        // 도메인 객체의 메서드를 사용하여 메시지 삭제 상태로 변경
        val deletedMessage = existingMessage.markAsDeleted()

        // 업데이트된 메시지 저장 후 반환
        return messageCommandPort.save(deletedMessage)
    }

}
