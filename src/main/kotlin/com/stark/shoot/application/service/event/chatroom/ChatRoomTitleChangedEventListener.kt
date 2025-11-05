package com.stark.shoot.application.service.event.chatroom

import com.stark.shoot.adapter.`in`.socket.WebSocketMessageBroker
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.application.port.out.user.UserQueryPort
import com.stark.shoot.domain.shared.event.ChatRoomTitleChangedEvent
import com.stark.shoot.infrastructure.annotation.ApplicationEventListener
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@ApplicationEventListener
class ChatRoomTitleChangedEventListener(
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val userQueryPort: UserQueryPort,
    private val webSocketMessageBroker: WebSocketMessageBroker
) {

    private val logger = KotlinLogging.logger {}

    /**
     * 채팅방 제목 변경 이벤트 처리
     * - 채팅방 내 모든 참여자에게 제목 변경 알림
     * - 채팅방 목록 업데이트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleTitleChanged(event: ChatRoomTitleChangedEvent) {
        logger.info { 
            "Processing title changed event: roomId=${event.roomId.value}, " +
            "oldTitle=${event.oldTitle}, newTitle=${event.newTitle}" 
        }

        val chatRoom = chatRoomQueryPort.findById(event.roomId)
        if (chatRoom == null) {
            logger.warn { "Chat room not found for title changed event: ${event.roomId.value}" }
            return
        }

        val changedByName = try {
            userQueryPort.findUserById(event.changedBy)?.nickname?.value ?: "사용자"
        } catch (e: Exception) {
            "사용자"
        }

        val titleChangeNotification = mapOf(
            "type" to "TITLE_CHANGED",
            "roomId" to event.roomId.value,
            "oldTitle" to event.oldTitle,
            "newTitle" to event.newTitle,
            "changedBy" to event.changedBy.value,
            "changedByName" to changedByName,
            "timestamp" to event.occurredOn
        )

        // 모든 참여자에게 제목 변경 알림 브로드캐스트
        var successCount = 0
        var failureCount = 0

        chatRoom.participants.forEach { participantId ->
            // 채팅방 내 제목 변경 알림
            webSocketMessageBroker.sendMessage(
                "/topic/chat/${event.roomId.value}/title",
                titleChangeNotification,
                retryCount = 1
            ).whenComplete { success, throwable ->
                if (success) {
                    successCount++
                } else {
                    failureCount++
                    logger.warn { "Failed to send title change notification to user ${participantId.value}: ${throwable?.message}" }
                }
            }

            // 채팅방 목록의 제목 업데이트
            val chatRoomListUpdate = mapOf(
                "roomId" to event.roomId.value,
                "title" to event.newTitle,
                "lastActiveAt" to chatRoom.lastActiveAt.toString()
            )

            webSocketMessageBroker.sendMessage(
                "/topic/user/${participantId.value}/chatrooms/update",
                chatRoomListUpdate,
                retryCount = 2
            ).whenComplete { success, throwable ->
                if (!success) {
                    logger.warn { "Failed to update chat room list for user ${participantId.value}: ${throwable?.message}" }
                }
            }
        }

        logger.info { 
            "Title changed event processed: roomId=${event.roomId.value}, " +
            "notifications sent: $successCount success, $failureCount failure" 
        }
    }
}