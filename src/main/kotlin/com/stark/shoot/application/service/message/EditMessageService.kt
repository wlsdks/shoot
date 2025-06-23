package com.stark.shoot.application.service.message

import com.stark.shoot.application.port.`in`.message.EditMessageUseCase
import com.stark.shoot.application.port.out.message.MessageCommandPort
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.service.MessageEditDomainService
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.infrastructure.annotation.UseCase

@UseCase
class EditMessageService(
    private val messageQueryPort: MessageQueryPort,
    private val messageCommandPort: MessageCommandPort,
    private val messageEditDomainService: MessageEditDomainService
) : EditMessageUseCase {

    /**
     * @apiNote 메시지를 수정합니다.
     * @param messageId 수정할 메시지 ID
     * @param newContent 새로운 메시지 내용
     * @throws IllegalArgumentException 메시지를 찾을 수 없거나, 이미 삭제된 메시지이거나, 텍스트 타입이 아닌 경우, 또는 내용이 비어있는 경우
     * 이를 위해 EditMessageUseCase 인터페이스를 수정하여 userId 파라미터를 추가하거나,
     * SecurityContext에서 현재 사용자 정보를 가져오는 방식으로 구현할 수 있음
     */
    override fun editMessage(
        messageId: MessageId,
        newContent: String
    ): ChatMessage {
        // 메시지 조회
        val existingMessage = messageQueryPort.findById(messageId)
            ?: run {
                throw IllegalArgumentException("메시지를 찾을 수 없습니다.")
            }

        try {
            val updatedMessage = messageEditDomainService.editMessage(existingMessage, newContent)
            return messageCommandPort.save(updatedMessage)
        } catch (e: IllegalArgumentException) {
            throw e
        }
    }

}
