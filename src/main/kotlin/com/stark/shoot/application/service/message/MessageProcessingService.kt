package com.stark.shoot.application.service.message

import com.stark.shoot.adapter.`in`.web.socket.WebSocketMessageBroker
import com.stark.shoot.application.port.`in`.message.ProcessMessageUseCase
import com.stark.shoot.application.port.`in`.message.command.ProcessMessageCommand
import com.stark.shoot.application.port.out.chatroom.ChatRoomCommandPort
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.application.port.out.message.SaveMessagePort
import com.stark.shoot.application.port.out.message.preview.CacheUrlPreviewPort
import com.stark.shoot.application.port.out.message.preview.ExtractUrlPort
import com.stark.shoot.application.port.out.message.preview.LoadUrlContentPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.ChatMessageMetadata
import com.stark.shoot.domain.event.MessageSendedEvent
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.config.redis.RedisLockManager
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@UseCase
class MessageProcessingService(
    private val redisLockManager: RedisLockManager,
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val chatRoomCommandPort: ChatRoomCommandPort,
    private val saveMessagePort: SaveMessagePort,
    private val extractUrlPort: ExtractUrlPort,
    private val loadUrlContentPort: LoadUrlContentPort,
    private val cacheUrlPreviewPort: CacheUrlPreviewPort,
    private val webSocketMessageBroker: WebSocketMessageBroker,
    private val eventPublisher: EventPublisher,
) : ProcessMessageUseCase {

    private val logger = KotlinLogging.logger {}

    companion object {
        private const val LOCK_KEY_PREFIX = "chatroom:"
        private const val OWNER_ID_PREFIX = "processor-"
    }

    @Transactional
    override fun processMessageCreate(command: ProcessMessageCommand): ChatMessage {
        val message = command.message
        // 분산 락 키 생성 (채팅방별로 락을 걸기 위해 사용)
        val lockKey = "$LOCK_KEY_PREFIX${message.roomId}"
        val ownerId = "$OWNER_ID_PREFIX${UUID.randomUUID()}"

        try {
            return processMessageWithLock(message, lockKey, ownerId)
        } catch (e: Exception) {
            logger.error(e) { "메시지 처리 오류: ${message.id}" }
            throw e
        }
    }

    /**
     * 분산 락을 사용하여 메시지를 처리합니다.
     *
     * @param message 처리할 메시지
     * @param lockKey 분산 락 키
     * @param ownerId 락 소유자 ID
     * @return 처리된 메시지
     */
    private fun processMessageWithLock(
        message: ChatMessage,
        lockKey: String,
        ownerId: String
    ): ChatMessage {
        return redisLockManager.withLock(lockKey, ownerId) {
            processInternal(message)
        }
    }

    private fun processInternal(message: ChatMessage): ChatMessage {
        val chatRoom = chatRoomQueryPort.findById(message.roomId)
            ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다. roomId=${message.roomId}")

        var processed = message

        if (processed.content.type == MessageType.TEXT && processed.content.text.length >= 8) {
            val urls = extractUrlPort.extractUrls(processed.content.text)
            if (urls.isNotEmpty()) {
                val url = urls.first()
                val preview = cacheUrlPreviewPort.getCachedUrlPreview(url)
                    ?: loadUrlContentPort.fetchUrlContent(url)?.also {
                        cacheUrlPreviewPort.cacheUrlPreview(url, it)
                    }
                if (preview != null) {
                    val metadata = processed.content.metadata ?: ChatMessageMetadata()
                    val updatedMetadata = metadata.copy(urlPreview = preview)
                    val updatedContent = processed.content.copy(metadata = updatedMetadata)
                    processed = processed.copy(content = updatedContent)
                }
            }
        }

        if (processed.id == null) {
            processed = saveMessagePort.save(processed)
        }

        if (processed.readBy[processed.senderId] != true) {
            processed = processed.markAsRead(processed.senderId)
        }

        processed.id?.let { id ->
            chatRoomCommandPort.updateLastReadMessageId(processed.roomId, processed.senderId, id)
        }

        val now = Instant.now()
        val needsUpdate = processed.id != null &&
                (chatRoom.lastMessageId != processed.id || chatRoom.lastActiveAt.isBefore(now.minusSeconds(60)))
        if (needsUpdate) {
            val updatedRoom = chatRoom.copy(lastMessageId = processed.id, lastActiveAt = now)
            chatRoomCommandPort.save(updatedRoom)
        }

        webSocketMessageBroker.sendMessage("/topic/messages/${processed.roomId.value}", processed)
        eventPublisher.publish(MessageSendedEvent.create(processed))

        return processed
    }

}
