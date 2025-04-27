package com.stark.shoot.application.service.message

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageType
import com.stark.shoot.application.port.`in`.message.EditMessageUseCase
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.application.port.out.message.SaveMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.util.toObjectId
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant

@UseCase
class EditMessageService(
    private val loadMessagePort: LoadMessagePort,
    private val saveMessagePort: SaveMessagePort
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
        messageId: String,
        newContent: String
    ): ChatMessage {
        logger.debug { "메시지 수정 요청: messageId=$messageId, newContent=$newContent" }

        // 내용 유효성 검사
        if (newContent.isBlank()) {
            logger.warn { "메시지 내용이 비어있습니다: messageId=$messageId" }
            throw IllegalArgumentException("메시지 내용은 비어있을 수 없습니다.")
        }

        // 메시지 조회
        val existingMessage = loadMessagePort.findById(messageId.toObjectId())
            ?: run {
                logger.warn { "메시지를 찾을 수 없습니다: messageId=$messageId" }
                throw IllegalArgumentException("메시지를 찾을 수 없습니다.")
            }

        // 삭제된 메시지 확인
        if (existingMessage.isDeleted) {
            logger.warn { "이미 삭제된 메시지입니다: messageId=$messageId" }
            throw IllegalArgumentException("삭제된 메시지는 수정할 수 없습니다.")
        }

        // 메시지 타입 확인 (TEXT 타입만 수정 가능)
        if (existingMessage.content.type != MessageType.TEXT) {
            logger.warn { "텍스트 타입이 아닌 메시지는 수정할 수 없습니다: messageId=$messageId, type=${existingMessage.content.type}" }
            throw IllegalArgumentException("텍스트 타입의 메시지만 수정할 수 있습니다.")
        }

        // 내용 업데이트 및 편집 여부 설정
        val updateContent = existingMessage.content.copy(
            text = newContent,
            isEdited = true
        )

        // 업데이트된 메시지 생성
        val updatedMessage = existingMessage.copy(
            content = updateContent,
            updatedAt = Instant.now()
        )

        // 업데이트된 메시지 저장 후 반환
        logger.info { "메시지가 성공적으로 수정되었습니다: messageId=$messageId" }
        return saveMessagePort.save(updatedMessage)
    }

}
