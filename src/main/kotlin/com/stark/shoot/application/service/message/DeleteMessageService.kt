package com.stark.shoot.application.service.message

import com.stark.shoot.application.port.`in`.message.DeleteMessageUseCase
import com.stark.shoot.application.port.out.message.LoadChatMessagePort
import com.stark.shoot.application.port.out.message.SaveChatMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.util.toObjectId
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class DeleteMessageService(
    private val loadChatMessagePort: LoadChatMessagePort,
    private val saveChatMessagePort: SaveChatMessagePort
) : DeleteMessageUseCase {

    /**
     * @apiNote 메시지 삭제
     * @param messageId 삭제할 메시지 ID
     */
    override fun deleteMessage(messageId: String): ChatMessage {
        // 메시지 로드
        val existingMessage = loadChatMessagePort.findById(messageId.toObjectId())
            ?: throw IllegalArgumentException("메시지를 찾을 수 없습니다. messageId=$messageId")

        // 삭제 상태로 변경 (isDeleted 플래그 설정)
        val updatedContent = existingMessage.content.copy(
            isDeleted = true
        )
        val updatedMessage = existingMessage.copy(
            content = updatedContent,
            updatedAt = Instant.now()
        )

        // 업데이트된 메시지 저장 후 반환
        return saveChatMessagePort.save(updatedMessage)
    }

}