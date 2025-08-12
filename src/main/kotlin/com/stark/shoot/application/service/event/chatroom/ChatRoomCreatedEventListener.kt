package com.stark.shoot.application.service.event.chatroom

import com.stark.shoot.adapter.`in`.socket.WebSocketMessageBroker
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.domain.event.ChatRoomCreatedEvent
import com.stark.shoot.infrastructure.annotation.ApplicationEventListener
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@ApplicationEventListener
class ChatRoomCreatedEventListener(
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val webSocketMessageBroker: WebSocketMessageBroker
) {

    private val logger = KotlinLogging.logger {}

    /**
     * 채팅방 생성 이벤트 처리
     * - 모든 참여자에게 새 채팅방 생성 알림
     * - 채팅방 목록 실시간 업데이트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleChatRoomCreated(event: ChatRoomCreatedEvent) {
        logger.info { "Processing chat room created event: roomId=${event.roomId.value}, userId=${event.userId.value}" }

        val chatRoom = chatRoomQueryPort.findById(event.roomId)
        if (chatRoom == null) {
            logger.warn { "Chat room not found for created event: ${event.roomId.value}" }
            return
        }

        val chatRoomInfo = mapOf(
            "roomId" to chatRoom.id?.value,
            "title" to chatRoom.title?.value,
            "type" to chatRoom.type.name,
            "participants" to chatRoom.participants.map { it.value },
            "participantCount" to chatRoom.participants.size,
            "createdBy" to event.userId.value,
            "createdAt" to chatRoom.createdAt.toString()
        )

        // 모든 참여자에게 새 채팅방 생성 알림 전송
        chatRoom.participants.forEach { participantId ->
            try {
                webSocketMessageBroker.sendMessage(
                    "/topic/user/${participantId.value}/chatrooms/new",
                    chatRoomInfo,
                    retryCount = 2
                ).whenComplete { success, throwable ->
                    if (success) {
                        logger.debug { "New chat room notification sent to user ${participantId.value}" }
                    } else {
                        logger.warn { "Failed to send new chat room notification to user ${participantId.value}: ${throwable?.message}" }
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "Failed to send chat room creation notification to user ${participantId.value}" }
            }
        }

        logger.info { "Chat room created event processed: roomId=${event.roomId.value}" }
    }
}