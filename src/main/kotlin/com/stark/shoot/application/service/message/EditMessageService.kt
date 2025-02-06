package com.stark.shoot.application.service.message

import com.stark.shoot.application.port.`in`.message.EditMessageUseCase
import com.stark.shoot.application.port.out.LoadChatMessagePort
import com.stark.shoot.application.port.out.SaveChatMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.common.util.toObjectId
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class EditMessageService(
    private val loadChatMessagePort: LoadChatMessagePort,
    private val saveChatMessagePort: SaveChatMessagePort
) : EditMessageUseCase {

    /**
     * @apiNote 메시지를 수정합니다.
     * @param messageId 수정할 메시지 ID
     * @param newContent 새로운 메시지 내용
     */
    override fun editMessage(
        messageId: String,
        newContent: String
    ): ChatMessage {
        // 메시지 조회
        val existingMessage = loadChatMessagePort.findById(messageId.toObjectId())
            ?: throw IllegalArgumentException("메시지를 찾을 수 없습니다.")

        // 내용 업데이트 및 편집 여부 설정
        val updateContent = existingMessage.content.copy(
            text = newContent,
            isEdited = true
        )

        val updatedMessage = existingMessage.copy(
            content = updateContent,
            updatedAt = Instant.now()
        )

        // 업데이트된 메시지 저장 후 반환
        return saveChatMessagePort.save(updatedMessage)
    }

}