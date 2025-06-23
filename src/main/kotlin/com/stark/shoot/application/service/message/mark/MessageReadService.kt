package com.stark.shoot.application.service.message.mark

import com.stark.shoot.adapter.`in`.web.socket.WebSocketMessageBroker
import com.stark.shoot.application.port.`in`.message.mark.MessageReadUseCase
import com.stark.shoot.application.port.out.chatroom.ChatRoomCommandPort
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.application.port.out.message.MessageReadRedisPort
import com.stark.shoot.application.port.out.message.SaveMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.event.MessageBulkReadEvent
import com.stark.shoot.domain.event.MessageUnreadCountUpdatedEvent
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class MessageReadService(
    private val saveMessagePort: SaveMessagePort,
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val chatRoomCommandPort: ChatRoomCommandPort,
    private val eventPublisher: EventPublisher,
    private val loadMessagePort: LoadMessagePort,
    private val messageReadRedisPort: MessageReadRedisPort,
    private val webSocketMessageBroker: WebSocketMessageBroker
) : MessageReadUseCase {
    private val logger = KotlinLogging.logger {}

    /**
     * 단일 메시지를 읽음 상태로 표시합니다.
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @return 업데이트된 메시지
     */
    override fun markMessageAsRead(
        messageId: MessageId,
        userId: UserId
    ) {
        val chatMessage = loadMessagePort.findById(messageId)
            ?: throw ResourceNotFoundException("메시지를 찾을 수 없습니다. messageId=$messageId")

        // 이미 읽은 메시지인 경우 추가 작업 없이 반환
        if (chatMessage.readBy[userId] == true) {
            logger.debug { "이미 읽은 메시지입니다: messageId=$messageId, userId=$userId" }
            return
        }

        // 도메인 객체의 메서드를 사용하여 메시지 읽음 상태 업데이트
        val updatedMessage = saveMessagePort.save(chatMessage.markAsRead(userId))
        val roomId = chatMessage.roomId

        // 병렬로 비동기 작업 처리 (트랜잭션 외부에서 실행되어야 하는 작업들)
        try {
            // 1. 채팅방 사용자의 마지막 읽은 메시지 ID 업데이트
            chatRoomCommandPort.updateLastReadMessageId(roomId, userId, messageId)

            // 2. 읽지 않은 메시지 수 업데이트 (Redis 활용) - 원자적 연산 사용
            messageReadRedisPort.decrementUnreadCount(userId, roomId)

            // 3. 채팅방의 모든 참여자에게 읽은 상태 알림 (WebSocket)
            webSocketMessageBroker.sendMessage(
                "/topic/read/${roomId.value}",
                mapOf(
                    "messageId" to updatedMessage.id?.value,
                    "userId" to userId.value,
                    "readBy" to updatedMessage.readBy
                )
            )

            // 4. 업데이트된 메시지 WebSocket 전송
            webSocketMessageBroker.sendMessage(
                destination = "/topic/messages/${roomId.value}",
                payload = updatedMessage
            )
        } catch (e: Exception) {
            // 비동기 작업 실패 시 로깅만 하고 예외는 전파하지 않음 (메인 트랜잭션은 이미 완료됨)
            logger.error(e) { "메시지 읽음 처리 후속 작업 실패: messageId=$messageId, userId=$userId" }
        }
    }

    /**
     * 채팅방의 모든 메시지를 읽음 상태로 표시합니다.
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @param requestId 프론트에서 생성한 요청 ID (중복 요청 방지용, 선택적이며 유저나 채팅방id와는 전혀 관계 없음)
     */
    override fun markAllMessagesAsRead(
        roomId: ChatRoomId,
        userId: UserId,
        requestId: String?
    ) {
        // 중복 요청 체크 (Redis 캐시 활용)
        if (requestId != null) {
            val key = messageReadRedisPort.createReadOperationKey(roomId, userId, requestId)

            // 중복 요청 확인 및 설정을 원자적으로 수행 (SETNX 패턴)
            val isFirstRequest = messageReadRedisPort.setIfAbsent(key, "1", 30L, java.util.concurrent.TimeUnit.SECONDS)

            if (isFirstRequest != true) {
                logger.info { "중복 읽음 처리 요청 감지 (requestId=$requestId)" }
                return
            }

            // 만료 시간은 일반적인 네트워크 지연 및 클라이언트 재시도를 고려한 값
            // 클라이언트는 새로운 읽음 요청마다 새로운 requestId를 생성해야 함
        }

        try {
            // 채팅방 조회 및 사용자 참여 확인
            val chatRoom = chatRoomQueryPort.findById(roomId)
                ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다. roomId=$roomId")

            if (!chatRoom.participants.contains(userId)) {
                throw IllegalArgumentException("참여하지 않은 채팅방입니다. userId=$userId, roomId=$roomId")
            }

            // 읽지 않은 메시지를 배치 단위로 처리 (성능 최적화)
            val batchSize = 100
            var processedCount = 0
            val allUpdatedMessageIds = mutableListOf<String>()
            var lastMessage: ChatMessage? = null

            while (true) {
                // 읽지 않은 메시지 조회 (내가 보낸 메시지 제외)
                val unreadMessages = loadMessagePort.findUnreadByRoomId(roomId, userId, batchSize)
                    .filter { it.senderId != userId }

                // 더 이상 처리할 메시지가 없으면 종료
                if (unreadMessages.isEmpty()) {
                    break
                }

                // 모든 메시지 읽음 처리
                val updatedMessageIds = processUnreadMessages(unreadMessages, userId)
                allUpdatedMessageIds.addAll(updatedMessageIds.map { it.value })

                // 마지막 메시지 업데이트 (가장 최신 메시지를 찾기 위해)
                val batchLastMessage = unreadMessages.maxByOrNull { it.createdAt ?: java.time.Instant.MIN }
                if (batchLastMessage != null) {
                    if (lastMessage == null ||
                        (batchLastMessage.createdAt ?: java.time.Instant.MIN) > (lastMessage.createdAt
                            ?: java.time.Instant.MIN)
                    ) {
                        lastMessage = batchLastMessage
                    }
                }

                processedCount += unreadMessages.size

                logger.debug { "배치 처리 완료: $processedCount 메시지 처리됨, roomId=$roomId, userId=$userId" }

                // 더 이상 처리할 메시지가 없으면 종료
                if (unreadMessages.size < batchSize) {
                    break
                }
            }

            // 읽지 않은 메시지가 없으면 불필요한 작업 방지
            if (allUpdatedMessageIds.isEmpty()) {
                logger.info { "읽지 않은 메시지가 없습니다. roomId=$roomId, userId=$userId" }
                return
            }

            // 마지막으로 읽은 메시지 ID 업데이트
            lastMessage?.id?.let { lastMessageId ->
                chatRoomCommandPort.updateLastReadMessageId(roomId, userId, lastMessageId)
            }

            val updatedMessageIds = allUpdatedMessageIds

            // Redis와 이벤트 업데이트 (읽지 않은 메시지 수 0으로 설정)
            // 원자적 연산으로 효율적으로 처리
            messageReadRedisPort.resetUnreadCount(userId, roomId)

            // 채팅방의 마지막 메시지 내용 조회
            val lastMessageText = chatRoom.lastMessageId?.let { lastMessageId ->
                try {
                    // 실제 메시지 내용 조회
                    val lastMessage = loadMessagePort.findById(lastMessageId)

                    when {
                        lastMessage == null -> "메시지를 찾을 수 없습니다"
                        lastMessage.content.isDeleted -> "삭제된 메시지입니다"
                        lastMessage.content.text.isNotBlank() -> {
                            // 긴 메시지는 요약
                            if (lastMessage.content.text.length > 30) {
                                "${lastMessage.content.text.take(30)}..."
                            } else {
                                lastMessage.content.text
                            }
                        }

                        lastMessage.content.attachments.isNotEmpty() -> "첨부파일이 포함된 메시지"
                        else -> "내용 없는 메시지"
                    }
                } catch (e: Exception) {
                    logger.warn(e) { "마지막 메시지 조회 실패: $lastMessageId" }
                    "최근 메시지"
                }
            } ?: "메시지가 없습니다"

            if (updatedMessageIds.isNotEmpty()) {
                // WebSocket을 통해 읽음 완료처리된 메시지 id를 실시간 알림
                val messageIdVos = updatedMessageIds.map { MessageId.from(it) }
                sendBulkReadNotification(roomId, messageIdVos, userId)

                // 읽지 않은 메시지 수 업데이트 이벤트 발행
                publishEvent(roomId, userId, lastMessageText)
            }

            logger.info { "채팅방 모든 메시지 읽음 처리 완료: roomId=$roomId, userId=$userId, 메시지 수=${updatedMessageIds.size}" }
        } catch (e: ResourceNotFoundException) {
            logger.error(e) { "채팅방 메시지 읽음 처리 실패 (리소스 없음): roomId=$roomId, userId=$userId" }
            throw e
        } catch (e: IllegalArgumentException) {
            logger.error(e) { "채팅방 메시지 읽음 처리 실패 (잘못된 요청): roomId=$roomId, userId=$userId" }
            throw e
        } catch (e: Exception) {
            logger.error(e) { "채팅방 메시지 읽음 처리 중 예상치 못한 오류 발생: roomId=$roomId, userId=$userId" }
            throw e
        }
    }

    /**
     * 읽지 않은 메시지들을 일괄 처리합니다.
     *
     * @param unreadMessages 읽지 않은 메시지 목록
     * @param userId 사용자 ID
     * @return 업데이트된 메시지 ID 목록
     */
    private fun processUnreadMessages(
        unreadMessages: List<ChatMessage>,
        userId: UserId
    ): List<MessageId> {
        if (unreadMessages.isEmpty()) {
            return emptyList()
        }

        // 도메인 객체의 메서드를 사용하여 모든 메시지를 한 번에 업데이트하도록 최적화
        val markedMessages = unreadMessages.map { message ->
            message.markAsRead(userId)
        }

        // 일괄 저장으로 DB 쿼리 최소화
        return saveMessagePort.saveAll(markedMessages)
            .mapNotNull { it.id }
            .also { messageIds ->
                logger.debug { "일괄 읽음 처리 완료: ${messageIds.size}개 메시지, userId=$userId" }
            }
    }


    /**
     * 채팅방의 모든 참여자에게 읽음 상태 업데이트를 WebSocket을 통해 알립니다.
     *
     * @param roomId 채팅방 ID
     * @param messageIds 읽은 메시지 ID 목록
     * @param userId 사용자 ID
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
     * 채팅방의 모든 참여자에게 읽음 상태 업데이트 이벤트를 발행합니다.
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @param lastMessage 마지막 메시지 내용
     */
    private fun publishEvent(
        roomId: ChatRoomId,
        userId: UserId,
        lastMessage: String
    ) {
        try {
            val event = MessageUnreadCountUpdatedEvent.create(
                roomId = roomId,
                unreadCounts = mapOf(userId to 0),
                lastMessage = lastMessage
            )

            eventPublisher.publish(event)
            logger.debug { "읽지 않은 메시지 수 업데이트 이벤트 발행 완료: roomId=${roomId.value}, userId=${userId.value}" }
        } catch (e: Exception) {
            logger.error(e) { "읽지 않은 메시지 수 업데이트 이벤트 발행 실패: roomId=${roomId.value}, userId=${userId.value}" }
            // 이벤트 발행 실패는 로깅만 하고 진행
        }
    }


}
