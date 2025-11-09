package com.stark.shoot.application.service.message.mark

import com.stark.shoot.application.port.`in`.message.mark.MessageReadUseCase
import com.stark.shoot.application.port.`in`.message.mark.command.MarkAllMessagesAsReadCommand
import com.stark.shoot.application.port.`in`.message.mark.command.MarkMessageAsReadCommand
import com.stark.shoot.application.port.out.chatroom.ChatRoomCommandPort
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.application.port.out.message.readreceipt.MessageReadReceiptCommandPort
import com.stark.shoot.application.port.out.message.readreceipt.MessageReadReceiptQueryPort
import com.stark.shoot.application.acl.*
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.readreceipt.MessageReadReceipt
import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Transactional
@UseCase
class MessageReadService(
    private val messageQueryPort: MessageQueryPort,
    private val messageReadReceiptCommandPort: MessageReadReceiptCommandPort,
    private val messageReadReceiptQueryPort: MessageReadReceiptQueryPort,
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val chatRoomCommandPort: ChatRoomCommandPort,
    private val notificationService: MessageReadNotificationService
) : MessageReadUseCase {
    private val logger = KotlinLogging.logger {}

    // 상수 정의
    companion object {
        private const val BATCH_SIZE = 100
    }

    /**
     * 단일 메시지를 읽음 상태로 표시합니다.
     *
     * @param command 메시지 읽음 처리 커맨드
     */
    override fun markMessageAsRead(command: MarkMessageAsReadCommand) {
        val messageId = command.messageId
        val userId = command.userId

        // 1. 메시지 조회
        val chatMessage = findMessageOrThrow(messageId)

        // 2. 이미 읽은 메시지인지 확인
        if (isAlreadyRead(messageId, userId)) {
            return
        }

        // 3. MessageReadReceipt Aggregate 생성 및 저장
        val readReceipt = createAndSaveReadReceipt(messageId, chatMessage.roomId, userId)
        val roomId = chatMessage.roomId

        // 4. 후속 작업 처리 (비동기적으로 처리되며 실패해도 메인 트랜잭션에 영향 없음)
        processSingleMessageReadSideEffects(roomId.toChatRoom(), messageId, userId)
    }

    /**
     * 채팅방의 모든 메시지를 읽음 상태로 표시합니다.
     *
     * @param command 모든 메시지 읽음 처리 커맨드
     */
    override fun markAllMessagesAsRead(command: MarkAllMessagesAsReadCommand) {
        val roomId = command.roomId
        val userId = command.userId

        try {
            // 1. 채팅방 및 사용자 유효성 검증
            validateChatRoomAndUser(roomId, userId)

            // 2. 읽지 않은 메시지 일괄 처리
            val (updatedMessageIds, lastMessage) = processAllUnreadMessages(roomId, userId)

            // 3. 읽을 메시지가 없으면 종료
            if (updatedMessageIds.isEmpty()) {
                logger.info { "읽지 않은 메시지가 없습니다. roomId=$roomId, userId=$userId" }
                return
            }

            // 4. 마지막으로 읽은 메시지 ID 업데이트
            updateLastReadMessageId(roomId, userId, lastMessage)

            // 5. 알림 발행
            sendNotificationsAndEvents(roomId, userId, updatedMessageIds)

            logger.info { "채팅방 모든 메시지 읽음 처리 완료: roomId=$roomId, userId=$userId, 메시지 수=${updatedMessageIds.size}" }
        } catch (e: Exception) {
            handleMarkAllMessagesAsReadException(e, roomId, userId)
        }
    }

    // ===== 단일 메시지 읽음 처리 관련 메서드 =====

    /**
     * 메시지 ID로 메시지를 조회하고, 없으면 예외를 발생시킵니다.
     */
    private fun findMessageOrThrow(messageId: MessageId): ChatMessage {
        return messageQueryPort.findById(messageId)
            ?: throw ResourceNotFoundException("메시지를 찾을 수 없습니다. messageId=$messageId")
    }

    /**
     * 이미 읽은 메시지인지 확인합니다.
     */
    private fun isAlreadyRead(messageId: MessageId, userId: UserId): Boolean {
        if (messageReadReceiptQueryPort.hasRead(messageId, userId)) {
            logger.debug { "이미 읽은 메시지입니다: messageId=$messageId, userId=$userId" }
            return true
        }
        return false
    }

    /**
     * MessageReadReceipt를 생성하고 저장합니다.
     */
    private fun createAndSaveReadReceipt(
        messageId: MessageId,
        roomId: com.stark.shoot.domain.chat.vo.ChatRoomId,
        userId: UserId
    ): MessageReadReceipt {
        val readReceipt = MessageReadReceipt.create(messageId, roomId, userId)
        return messageReadReceiptCommandPort.save(readReceipt)
    }

    /**
     * 단일 메시지 읽음 처리 후 필요한 부가 작업을 수행합니다.
     */
    private fun processSingleMessageReadSideEffects(
        roomId: ChatRoomId,
        messageId: MessageId,
        userId: UserId
    ) {
        try {
            // 1. 채팅방 사용자의 마지막 읽은 메시지 ID 업데이트
            chatRoomCommandPort.updateLastReadMessageId(roomId, userId, messageId)

            // 2. 읽음 상태 알림 전송
            notificationService.sendSingleReadNotification(roomId.toChat(), messageId, userId)

        } catch (e: Exception) {
            logger.error(e) { "메시지 읽음 처리 후속 작업 실패: messageId=$messageId, userId=$userId" }
            // 비동기 작업 실패 시 로깅만 하고 예외는 전파하지 않음 (메인 트랜잭션은 이미 완료됨)
        }
    }

    // ===== 모든 메시지 읽음 처리 관련 메서드 =====

    /**
     * 채팅방과 사용자의 유효성을 검증합니다.
     */
    private fun validateChatRoomAndUser(roomId: ChatRoomId, userId: UserId): ChatRoom {
        val chatRoom = chatRoomQueryPort.findById(roomId)
            ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다. roomId=$roomId")

        if (!chatRoom.participants.contains(userId)) {
            throw IllegalArgumentException("참여하지 않은 채팅방입니다. userId=$userId, roomId=$roomId")
        }

        return chatRoom
    }

    /**
     * 모든 읽지 않은 메시지를 처리합니다.
     *
     * @return Pair(업데이트된 메시지 ID 목록, 마지막 메시지)
     */
    private fun processAllUnreadMessages(
        roomId: ChatRoomId,
        userId: UserId
    ): Pair<List<String>, ChatMessage?> {
        var processedCount = 0
        val allUpdatedMessageIds = mutableListOf<String>()
        var lastMessage: ChatMessage? = null

        while (true) {
            // 1. 읽지 않은 메시지 조회 (내가 보낸 메시지 제외)
            val unreadMessages = fetchUnreadMessages(roomId, userId)

            // 2. 더 이상 처리할 메시지가 없으면 종료
            if (unreadMessages.isEmpty()) {
                break
            }

            // 3. 모든 메시지 읽음 처리
            val updatedMessageIds = markMessagesAsRead(unreadMessages, userId)
            allUpdatedMessageIds.addAll(updatedMessageIds.map { it.value })

            // 4. 마지막 메시지 업데이트
            lastMessage = updateLastMessageIfNewer(unreadMessages, lastMessage)

            // 5. 처리 상태 업데이트
            processedCount += unreadMessages.size
            logger.debug { "배치 처리 완료: $processedCount 메시지 처리됨, roomId=$roomId, userId=$userId" }

            // 6. 더 이상 처리할 메시지가 없으면 종료
            if (unreadMessages.size < BATCH_SIZE) {
                break
            }
        }

        return Pair(allUpdatedMessageIds, lastMessage)
    }

    /**
     * 읽지 않은 메시지를 조회합니다.
     */
    private fun fetchUnreadMessages(roomId: ChatRoomId, userId: UserId): List<ChatMessage> {
        // 채팅방의 모든 메시지 조회 (최신 BATCH_SIZE개)
        val allMessages = messageQueryPort.findByRoomId(roomId, BATCH_SIZE)

        // 이미 읽은 메시지 제외하고, 자신이 보낸 메시지도 제외
        return allMessages.filter { message ->
            message.id?.let { messageId ->
                !messageReadReceiptQueryPort.hasRead(messageId, userId) && message.senderId != userId
            } ?: false
        }
    }

    /**
     * 메시지 목록을 읽음 상태로 표시합니다.
     */
    private fun markMessagesAsRead(
        messages: List<ChatMessage>,
        userId: UserId
    ): List<MessageId> {
        if (messages.isEmpty()) {
            return emptyList()
        }

        // MessageReadReceipt Aggregate 생성 및 일괄 저장
        val readReceipts = messages.mapNotNull { message ->
            message.id?.let { messageId ->
                MessageReadReceipt.create(
                    messageId = messageId,
                    roomId = message.roomId,
                    userId = userId
                )
            }
        }

        readReceipts.forEach { messageReadReceiptCommandPort.save(it) }

        return messages.mapNotNull { it.id }
            .also { messageIds ->
                logger.debug { "일괄 읽음 처리 완료: ${messageIds.size}개 메시지, userId=$userId" }
            }
    }

    /**
     * 가장 최신 메시지를 찾아 업데이트합니다.
     */
    private fun updateLastMessageIfNewer(
        messages: List<ChatMessage>,
        currentLastMessage: ChatMessage?
    ): ChatMessage? {
        val batchLastMessage = messages.maxByOrNull { it.createdAt ?: Instant.MIN }
            ?: return currentLastMessage

        if (currentLastMessage == null) {
            return batchLastMessage
        }

        val currentTimestamp = currentLastMessage.createdAt ?: Instant.MIN
        val newTimestamp = batchLastMessage.createdAt ?: Instant.MIN

        return if (newTimestamp > currentTimestamp) batchLastMessage else currentLastMessage
    }

    /**
     * 마지막으로 읽은 메시지 ID를 업데이트합니다.
     */
    private fun updateLastReadMessageId(
        roomId: ChatRoomId,
        userId: UserId,
        lastMessage: ChatMessage?
    ) {
        lastMessage?.id?.let { lastMessageId ->
            chatRoomCommandPort.updateLastReadMessageId(roomId, userId, lastMessageId)
        }
    }

    /**
     * 알림 및 이벤트를 발행합니다.
     */
    private fun sendNotificationsAndEvents(
        roomId: ChatRoomId,
        userId: UserId,
        updatedMessageIds: List<String>
    ) {
        if (updatedMessageIds.isEmpty()) {
            return
        }

        // WebSocket을 통해 읽음 완료처리된 메시지 id를 실시간 알림
        val messageIdVos = updatedMessageIds.map { MessageId.from(it) }
        notificationService.sendBulkReadNotification(roomId.toChat(), messageIdVos, userId)
    }

    /**
     * markAllMessagesAsRead 메서드에서 발생한 예외를 처리합니다.
     */
    private fun handleMarkAllMessagesAsReadException(
        exception: Exception,
        roomId: ChatRoomId,
        userId: UserId
    ) {
        when (exception) {
            is ResourceNotFoundException -> {
                logger.error(exception) { "채팅방 메시지 읽음 처리 실패 (리소스 없음): roomId=$roomId, userId=$userId" }
                throw exception
            }

            is IllegalArgumentException -> {
                logger.error(exception) { "채팅방 메시지 읽음 처리 실패 (잘못된 요청): roomId=$roomId, userId=$userId" }
                throw exception
            }

            else -> {
                logger.error(exception) { "채팅방 메시지 읽음 처리 중 예상치 못한 오류 발생: roomId=$roomId, userId=$userId" }
                throw exception
            }
        }
    }

}
