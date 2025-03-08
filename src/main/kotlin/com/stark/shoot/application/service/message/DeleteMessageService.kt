package com.stark.shoot.application.service.message

import com.stark.shoot.application.port.`in`.message.DeleteMessageUseCase
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.application.port.out.message.SaveMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.util.toObjectId
import java.time.Instant

@UseCase
class DeleteMessageService(
    private val loadMessagePort: LoadMessagePort,
    private val saveMessagePort: SaveMessagePort
) : DeleteMessageUseCase {

    /**
     * @apiNote 메시지 삭제
     * @param messageId 삭제할 메시지 ID
     */
    override fun deleteMessage(messageId: String): ChatMessage {
        // 메시지 로드
        val existingMessage = loadMessagePort.findById(messageId.toObjectId())
            ?: throw IllegalArgumentException("메시지를 찾을 수 없습니다. messageId=$messageId")

        // 삭제 상태로 변경 (isDeleted 플래그 설정)
        val updatedContent = existingMessage.content.copy(
            isDeleted = true
        )

        // 업데이트된 메시지 생성
        val updatedMessage = existingMessage.copy(
            content = updatedContent,
            updatedAt = Instant.now()
        )

        // 업데이트된 메시지 저장 후 반환
        return saveMessagePort.save(updatedMessage)
    }

}