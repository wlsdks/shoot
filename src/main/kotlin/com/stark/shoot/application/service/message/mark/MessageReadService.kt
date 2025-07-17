package com.stark.shoot.application.service.message.mark

import com.stark.shoot.adapter.`in`.web.socket.WebSocketMessageBroker
import com.stark.shoot.application.port.`in`.message.mark.MessageReadUseCase
import com.stark.shoot.application.port.`in`.message.mark.command.MarkAllMessagesAsReadCommand
import com.stark.shoot.application.port.`in`.message.mark.command.MarkMessageAsReadCommand
import com.stark.shoot.application.port.out.chatroom.ChatRoomCommandPort
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.application.port.out.message.MessageCommandPort
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.event.MessageBulkReadEvent
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Transactional
@UseCase
class MessageReadService(
    private val messageQueryPort: MessageQueryPort,
    private val messageCommandPort: MessageCommandPort,
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val chatRoomCommandPort: ChatRoomCommandPort,
    private val webSocketMessageBroker: WebSocketMessageBroker
) : MessageReadUseCase {
    private val logger = KotlinLogging.logger {}

    // 상수 정의
    companion object {
        private const val REDIS_LOCK_EXPIRATION_SECONDS = 30L
        private const val BATCH_SIZE = 100
        private const val MAX_MESSAGE_SUMMARY_LENGTH = 30
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
        if (isAlreadyRead(chatMessage, userId)) {
            return
        }

        // 3. 메시지 읽음 상태 업데이트
        val updatedMessage = updateMessageReadStatus(chatMessage, userId)
        val roomId = chatMessage.roomId

        // 4. 후속 작업 처리 (비동기적으로 처리되며 실패해도 메인 트랜잭션에 영향 없음)
        processSingleMessageReadSideEffects(roomId, messageId, userId, updatedMessage)
    }

    /**
     * 채팅방의 모든 메시지를 읽음 상태로 표시합니다.
     *
     * @param command 모든 메시지 읽음 처리 커맨드
     */
    override fun markAllMessagesAsRead(command: MarkAllMessagesAsReadCommand) {
        val roomId = command.roomId
        val userId = command.userId
        val requestId = command.requestId
        // 1. 중복 요청 체크
        if (isDuplicateRequest(roomId, userId, requestId)) {
            return
        }

        try {
            // 2. 채팅방 및 사용자 유효성 검증
            val chatRoom = validateChatRoomAndUser(roomId, userId)

            // 3. 읽지 않은 메시지 일괄 처리
            val (updatedMessageIds, lastMessage) = processAllUnreadMessages(roomId, userId)

            // 4. 읽을 메시지가 없으면 종료
            if (updatedMessageIds.isEmpty()) {
                logger.info { "읽지 않은 메시지가 없습니다. roomId=$roomId, userId=$userId" }
                return
            }

            // 5. 마지막으로 읽은 메시지 ID 업데이트
            updateLastReadMessageId(roomId, userId, lastMessage)

            // 6. 마지막 메시지 텍스트 조회
            val lastMessageText = getLastMessageText(chatRoom)

            // 7. 알림 및 이벤트 발행
            sendNotificationsAndEvents(roomId, userId, updatedMessageIds, lastMessageText)

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
    private fun isAlreadyRead(message: ChatMessage, userId: UserId): Boolean {
        if (message.readBy[userId] == true) {
            logger.debug { "이미 읽은 메시지입니다: messageId=${message.id}, userId=$userId" }
            return true
        }
        return false
    }

    /**
     * 메시지 읽음 상태를 업데이트합니다.
     */
    private fun updateMessageReadStatus(message: ChatMessage, userId: UserId): ChatMessage {
        return messageCommandPort.save(message.markAsRead(userId))
    }

    /**
     * 단일 메시지 읽음 처리 후 필요한 부가 작업을 수행합니다.
     */
    private fun processSingleMessageReadSideEffects(
        roomId: ChatRoomId,
        messageId: MessageId,
        userId: UserId,
        updatedMessage: ChatMessage
    ) {
        try {
            // 1. 채팅방 사용자의 마지막 읽은 메시지 ID 업데이트
            chatRoomCommandPort.updateLastReadMessageId(roomId, userId, messageId)

            // 2. 읽음 상태 알림 전송
            sendSingleReadNotification(roomId, updatedMessage, userId)

            // 3. 업데이트된 메시지 WebSocket 전송
            sendUpdatedMessage(roomId, updatedMessage)
        } catch (e: Exception) {
            logger.error(e) { "메시지 읽음 처리 후속 작업 실패: messageId=$messageId, userId=$userId" }
            // 비동기 작업 실패 시 로깅만 하고 예외는 전파하지 않음 (메인 트랜잭션은 이미 완료됨)
        }
    }

    /**
     * 단일 메시지 읽음 상태 알림을 전송합니다.
     */
    private fun sendSingleReadNotification(
        roomId: ChatRoomId,
        message: ChatMessage,
        userId: UserId
    ) {
        webSocketMessageBroker.sendMessage(
            "/topic/read/${roomId.value}",
            mapOf(
                "messageId" to message.id?.value,
                "userId" to userId.value,
                "readBy" to message.readBy
            )
        )
    }

    /**
     * 업데이트된 메시지를 WebSocket으로 전송합니다.
     */
    private fun sendUpdatedMessage(roomId: ChatRoomId, message: ChatMessage) {
        webSocketMessageBroker.sendMessage(
            destination = "/topic/messages/${roomId.value}",
            payload = message
        )
    }

    // ===== 모든 메시지 읽음 처리 관련 메서드 =====

    /**
     * 중복 요청인지 확인합니다.
     */
    private fun isDuplicateRequest(roomId: ChatRoomId, userId: UserId, requestId: String?): Boolean {
        // Redis 중복 체크 로직 제거
        return false
    }

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
        return messageQueryPort.findUnreadByRoomId(roomId, userId, BATCH_SIZE)
            .filter { it.senderId != userId }
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

        // 도메인 객체의 메서드를 사용하여 모든 메시지를 한 번에 업데이트하도록 최적화
        val markedMessages = messages.map { it.markAsRead(userId) }

        // 일괄 저장으로 DB 쿼리 최소화
        return messageCommandPort.saveAll(markedMessages)
            .mapNotNull { it.id }
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
     * 채팅방의 마지막 메시지 내용을 조회합니다.
     */
    private fun getLastMessageText(chatRoom: ChatRoom): String {
        return chatRoom.lastMessageId?.let { lastMessageId ->
            try {
                // 실제 메시지 내용 조회
                val lastMessage = messageQueryPort.findById(lastMessageId)
                formatMessageContent(lastMessage)
            } catch (e: Exception) {
                logger.warn(e) { "마지막 메시지 조회 실패: $lastMessageId" }
                "최근 메시지"
            }
        } ?: "메시지가 없습니다"
    }

    /**
     * 메시지 내용을 포맷팅합니다.
     */
    private fun formatMessageContent(message: ChatMessage?): String {
        return when {
            message == null -> "메시지를 찾을 수 없습니다"
            message.content.isDeleted -> "삭제된 메시지입니다"
            message.content.text.isNotBlank() -> {
                if (message.content.text.length > MAX_MESSAGE_SUMMARY_LENGTH) {
                    "${message.content.text.take(MAX_MESSAGE_SUMMARY_LENGTH)}..."
                } else {
                    message.content.text
                }
            }

            message.content.attachments.isNotEmpty() -> "첨부파일이 포함된 메시지"
            else -> "내용 없는 메시지"
        }
    }

    /**
     * 알림 및 이벤트를 발행합니다.
     */
    private fun sendNotificationsAndEvents(
        roomId: ChatRoomId,
        userId: UserId,
        updatedMessageIds: List<String>,
        lastMessageText: String
    ) {
        if (updatedMessageIds.isEmpty()) {
            return
        }

        // 1. WebSocket을 통해 읽음 완료처리된 메시지 id를 실시간 알림
        val messageIdVos = updatedMessageIds.map { MessageId.from(it) }
        sendBulkReadNotification(roomId, messageIdVos, userId)
    }

    /**
     * 채팅방의 모든 참여자에게 읽음 상태 업데이트를 WebSocket을 통해 알립니다.
     */
    private fun sendBulkReadNotification(
        roomId: ChatRoomId,
        messageIds: List<MessageId>,
        userId: UserId
    ) {
        if (messageIds.isEmpty()) {
            logger.debug { "알림 대상 메시지가 없습니다: roomId=${roomId.value}, userId=${userId.value}" }
            return
        }

        try {
            webSocketMessageBroker.sendMessage(
                "/topic/read-bulk/${roomId.value}",
                MessageBulkReadEvent.create(roomId, messageIds, userId)
            )
            logger.debug { "일괄 읽음 알림 전송 완료: roomId=${roomId.value}, userId=${userId.value}, 메시지 수=${messageIds.size}" }
        } catch (e: Exception) {
            logger.error(e) { "일괄 읽음 알림 전송 실패: roomId=$roomId, userId=$userId" }
            // WebSocket 알림 실패는 중요하지만 전체 프로세스를 중단시킬 만큼 치명적이지 않음
        }
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
