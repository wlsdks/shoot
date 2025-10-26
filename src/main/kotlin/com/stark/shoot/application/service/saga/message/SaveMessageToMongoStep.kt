package com.stark.shoot.application.service.saga.message

import com.stark.shoot.application.port.out.message.MessageCommandPort
import com.stark.shoot.application.port.out.message.SaveMessagePort
import com.stark.shoot.domain.saga.SagaStep
import com.stark.shoot.domain.saga.message.MessageSagaContext
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

/**
 * Saga Step 1: MongoDB에 메시지 저장
 */
@Component
class SaveMessageToMongoStep(
    private val saveMessagePort: SaveMessagePort,
    private val messageCommandPort: MessageCommandPort
) : SagaStep<MessageSagaContext> {

    private val logger = KotlinLogging.logger {}

    override fun execute(context: MessageSagaContext): Boolean {
        return try {
            // 메시지 저장 (발신자 읽음 처리 포함)
            if (context.message.readBy[context.message.senderId] != true) {
                context.message.markAsRead(context.message.senderId)
            }

            val savedMessage = saveMessagePort.save(context.message)
            context.savedMessage = savedMessage
            context.recordStep(stepName())

            logger.info { "Message saved to MongoDB: messageId=${savedMessage.id?.value}" }
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to save message to MongoDB" }
            context.markFailed(e)
            false
        }
    }

    override fun compensate(context: MessageSagaContext): Boolean {
        return try {
            val messageId = context.savedMessage?.id
            if (messageId != null) {
                // MongoDB에서 메시지 삭제 (물리 삭제)
                // 주의: MongoDB의 deleteById는 메시지가 없어도 예외를 던지지 않음
                // 이미 삭제된 경우 = 보상 성공으로 간주 (멱등성 보장)
                messageCommandPort.delete(messageId)
                logger.info { "Compensated: Deleted message from MongoDB: messageId=${messageId.value}" }
            } else {
                logger.warn { "No saved message to compensate - skipping deletion" }
            }
            true
        } catch (e: Exception) {
            // 예외 발생 시에만 보상 실패로 간주
            logger.error(e) { "Failed to compensate message save - compensation failed" }
            false
        }
    }

    override fun stepName() = "SaveMessageToMongo"
}
