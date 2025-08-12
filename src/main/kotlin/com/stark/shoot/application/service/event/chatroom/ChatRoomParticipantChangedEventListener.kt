package com.stark.shoot.application.service.event.chatroom

import com.stark.shoot.adapter.`in`.socket.WebSocketMessageBroker
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.application.port.out.user.UserQueryPort
import com.stark.shoot.domain.event.ChatRoomParticipantChangedEvent
import com.stark.shoot.infrastructure.annotation.ApplicationEventListener
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@ApplicationEventListener
class ChatRoomParticipantChangedEventListener(
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val userQueryPort: UserQueryPort,
    private val webSocketMessageBroker: WebSocketMessageBroker
) {

    private val logger = KotlinLogging.logger {}

    /**
     * 채팅방 참여자 변경 이벤트 처리
     * - 채팅방 내 모든 참여자에게 변경 사항 알림
     * - 추가/제거된 참여자들에게 개별 알림
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleParticipantChanged(event: ChatRoomParticipantChangedEvent) {
        logger.info { 
            "Processing participant changed event: roomId=${event.roomId.value}, " +
            "added=${event.participantsAdded.size}, removed=${event.participantsRemoved.size}" 
        }

        val chatRoom = chatRoomQueryPort.findById(event.roomId)
        if (chatRoom == null) {
            logger.warn { "Chat room not found for participant changed event: ${event.roomId.value}" }
            return
        }

        val changedByName = try {
            userQueryPort.findUserById(event.changedBy)?.nickname?.value ?: "사용자"
        } catch (e: Exception) {
            "사용자"
        }

        // 채팅방 내 모든 현재 참여자에게 변경 사항 브로드캐스트
        broadcastToCurrentParticipants(chatRoom, event, changedByName)

        // 추가된 참여자들에게 환영 메시지
        sendWelcomeMessagesToNewParticipants(event, chatRoom, changedByName)

        // 제거된 참여자들에게 알림
        sendGoodbyeMessagesToRemovedParticipants(event, chatRoom, changedByName)

        logger.info { "Participant changed event processed: roomId=${event.roomId.value}" }
    }

    private fun broadcastToCurrentParticipants(
        chatRoom: com.stark.shoot.domain.chatroom.ChatRoom,
        event: ChatRoomParticipantChangedEvent,
        changedByName: String
    ) {
        val participantUpdate = mapOf(
            "roomId" to event.roomId.value,
            "type" to "PARTICIPANT_CHANGED",
            "participantsAdded" to event.participantsAdded.map { it.value },
            "participantsRemoved" to event.participantsRemoved.map { it.value },
            "changedBy" to event.changedBy.value,
            "changedByName" to changedByName,
            "currentParticipants" to chatRoom.participants.map { it.value },
            "participantCount" to chatRoom.participants.size,
            "timestamp" to event.occurredOn
        )

        chatRoom.participants.forEach { participantId ->
            webSocketMessageBroker.sendMessage(
                "/topic/chat/${event.roomId.value}/participants",
                participantUpdate,
                retryCount = 1
            ).whenComplete { success, throwable ->
                if (!success) {
                    logger.warn { "Failed to send participant update to user ${participantId.value}: ${throwable?.message}" }
                }
            }
        }
    }

    private fun sendWelcomeMessagesToNewParticipants(
        event: ChatRoomParticipantChangedEvent,
        chatRoom: com.stark.shoot.domain.chatroom.ChatRoom,
        changedByName: String
    ) {
        event.participantsAdded.forEach { newParticipantId ->
            val welcomeMessage = mapOf(
                "type" to "WELCOME_TO_GROUP",
                "roomId" to event.roomId.value,
                "title" to chatRoom.title?.value,
                "participantCount" to chatRoom.participants.size,
                "addedBy" to changedByName,
                "timestamp" to event.occurredOn
            )

            webSocketMessageBroker.sendMessage(
                "/topic/user/${newParticipantId.value}/notifications",
                welcomeMessage,
                retryCount = 2
            ).whenComplete { success, throwable ->
                if (success) {
                    logger.debug { "Welcome message sent to new participant ${newParticipantId.value}" }
                } else {
                    logger.warn { "Failed to send welcome message to ${newParticipantId.value}: ${throwable?.message}" }
                }
            }
        }
    }

    private fun sendGoodbyeMessagesToRemovedParticipants(
        event: ChatRoomParticipantChangedEvent,
        chatRoom: com.stark.shoot.domain.chatroom.ChatRoom,
        changedByName: String
    ) {
        event.participantsRemoved.forEach { removedParticipantId ->
            val isRemovedBySelf = removedParticipantId == event.changedBy
            val goodbyeMessage = mapOf(
                "type" to "REMOVED_FROM_GROUP",
                "roomId" to event.roomId.value,
                "title" to chatRoom.title?.value,
                "removedBy" to if (isRemovedBySelf) null else changedByName,
                "isVoluntary" to isRemovedBySelf,
                "timestamp" to event.occurredOn
            )

            webSocketMessageBroker.sendMessage(
                "/topic/user/${removedParticipantId.value}/notifications",
                goodbyeMessage,
                retryCount = 1
            ).whenComplete { success, throwable ->
                if (success) {
                    logger.debug { "Goodbye message sent to removed participant ${removedParticipantId.value}" }
                } else {
                    logger.warn { "Failed to send goodbye message to ${removedParticipantId.value}: ${throwable?.message}" }
                }
            }
        }
    }
}