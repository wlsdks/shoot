package com.stark.shoot.application.service.message

import com.stark.shoot.application.port.`in`.message.EditMessageUseCase
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.application.port.out.message.SaveMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.message.service.MessageEditDomainService
import com.stark.shoot.infrastructure.annotation.UseCase
import io.github.oshai.kotlinlogging.KotlinLogging

@UseCase
class EditMessageService(
    private val loadMessagePort: LoadMessagePort,
    private val saveMessagePort: SaveMessagePort,
    private val messageEditDomainService: MessageEditDomainService
) : EditMessageUseCase {
    private val logger = KotlinLogging.logger {}

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
        logger.debug { "메시지 수정 요청: messageId=$messageId, newContent=$newContent" }

        // 메시지 조회
        val existingMessage = loadMessagePort.findById(messageId)
            ?: run {
                logger.warn { "메시지를 찾을 수 없습니다: messageId=$messageId" }
                throw IllegalArgumentException("메시지를 찾을 수 없습니다.")
            }

        try {
            // 도메인 서비스를 사용하여 메시지 수정
            val updatedMessage = messageEditDomainService.editMessage(existingMessage, newContent)

            // 업데이트된 메시지 저장 후 반환
            logger.info { "메시지가 성공적으로 수정되었습니다: messageId=$messageId" }
            return saveMessagePort.save(updatedMessage)
        } catch (e: IllegalArgumentException) {
            logger.warn { "메시지 수정 실패: ${e.message}, messageId=$messageId" }
            throw e
        }
    }

}
