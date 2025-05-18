package com.stark.shoot.application.service.message

import com.stark.shoot.application.port.`in`.message.EditMessageUseCase
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.application.port.out.message.SaveMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.util.toObjectId
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Duration
import java.time.Instant

@UseCase
class EditMessageService(
    private val loadMessagePort: LoadMessagePort,
    private val saveMessagePort: SaveMessagePort
) : EditMessageUseCase {
    private val logger = KotlinLogging.logger {}

    companion object {
        // 메시지 편집 가능 시간 제한 (24시간)
        private val MAX_EDIT_DURATION = Duration.ofHours(24)
    }

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

        // 메시지 조회
        val existingMessage = loadMessagePort.findById(messageId.toObjectId())
            ?: run {
                logger.warn { "메시지를 찾을 수 없습니다: messageId=$messageId" }
                throw IllegalArgumentException("메시지를 찾을 수 없습니다.")
            }

        // 시간 제한 검사: 생성 후 일정 시간(24시간)이 지난 메시지는 수정 불가
        val now = Instant.now()
        val messageCreationTime = existingMessage.createdAt ?: now
        val timeSinceCreation = Duration.between(messageCreationTime, now)

        if (timeSinceCreation.compareTo(MAX_EDIT_DURATION) > 0) {
            logger.warn { "메시지 수정 시간 초과: messageId=$messageId, createdAt=${existingMessage.createdAt}, timeSinceCreation=${timeSinceCreation.toHours()}시간" }
            throw IllegalArgumentException("메시지 생성 후 ${MAX_EDIT_DURATION.toHours()}시간이 지나 수정할 수 없습니다.")
        }

        try {
            // 도메인 객체의 메서드를 사용하여 메시지 수정
            val updatedMessage = existingMessage.editMessage(newContent)

            // 업데이트된 메시지 저장 후 반환
            logger.info { "메시지가 성공적으로 수정되었습니다: messageId=$messageId" }
            return saveMessagePort.save(updatedMessage)
        } catch (e: IllegalArgumentException) {
            logger.warn { "메시지 수정 실패: ${e.message}, messageId=$messageId" }
            throw e
        }
    }

}
