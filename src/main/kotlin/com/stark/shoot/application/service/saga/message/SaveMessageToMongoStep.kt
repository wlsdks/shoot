package com.stark.shoot.application.service.saga.message

import com.stark.shoot.application.port.out.message.MessageCommandPort
import com.stark.shoot.application.port.out.message.SaveMessagePort
import com.stark.shoot.application.port.out.message.readreceipt.MessageReadReceiptCommandPort
import com.stark.shoot.application.port.out.message.readreceipt.MessageReadReceiptQueryPort
import com.stark.shoot.domain.chat.readreceipt.MessageReadReceipt
import com.stark.shoot.domain.saga.SagaStep
import com.stark.shoot.domain.saga.message.MessageSagaContext
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

/**
 * Saga Step 1: MongoDB에 메시지 저장
 *
 * DDD 개선: Context에서 도메인 객체 제거, Step에서 직접 주입받음
 */
@Component
class SaveMessageToMongoStep(
    private val saveMessagePort: SaveMessagePort,
    private val messageCommandPort: MessageCommandPort,
    private val messageReadReceiptCommandPort: MessageReadReceiptCommandPort,
    private val messageReadReceiptQueryPort: MessageReadReceiptQueryPort
) : SagaStep<MessageSagaContext> {

    private val logger = KotlinLogging.logger {}

    // DDD 개선: Orchestrator에서 주입받은 메시지 임시 저장
    @Volatile
    private var messageToProcess: com.stark.shoot.domain.chat.message.ChatMessage? = null

    /**
     * Orchestrator에서 호출하여 메시지 설정
     */
    fun setMessage(message: com.stark.shoot.domain.chat.message.ChatMessage) {
        this.messageToProcess = message
    }

    override fun execute(context: MessageSagaContext): Boolean {
        return try {
            val message = messageToProcess
                ?: throw IllegalStateException("Message not set")

            // 메시지 저장
            val savedMessage = saveMessagePort.save(message)
            val savedMessageId = savedMessage.id
                ?: throw IllegalStateException("Saved message has no ID")

            // 발신자 자동 읽음 처리 (MessageReadReceipt Aggregate 사용)
            if (!messageReadReceiptQueryPort.hasRead(savedMessageId, message.senderId)) {
                val senderReadReceipt = MessageReadReceipt.create(
                    messageId = savedMessageId,
                    roomId = message.roomId,
                    userId = message.senderId
                )
                messageReadReceiptCommandPort.save(senderReadReceipt)
            }

            // Context에는 ID만 저장
            context.savedMessageId = savedMessageId.value
            context.recordStep(stepName())

            logger.info { "Message saved to MongoDB: messageId=${savedMessageId.value}" }
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to save message to MongoDB" }
            context.markFailed(e)
            false
        } finally {
            // 메모리 정리
            messageToProcess = null
        }
    }

    override fun compensate(context: MessageSagaContext): Boolean {
        return try {
            val messageIdStr = context.savedMessageId
            if (messageIdStr != null) {
                // MongoDB에서 메시지 삭제 (물리 삭제)
                val messageId = com.stark.shoot.domain.chat.message.vo.MessageId.from(messageIdStr)
                messageCommandPort.delete(messageId)
                logger.info { "Compensated: Deleted message from MongoDB: messageId=$messageIdStr" }
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
