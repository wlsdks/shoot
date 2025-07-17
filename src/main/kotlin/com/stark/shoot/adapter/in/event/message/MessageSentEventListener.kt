package com.stark.shoot.adapter.`in`.event.message

import com.stark.shoot.adapter.`in`.web.mapper.ChatRoomResponseMapper
import com.stark.shoot.adapter.`in`.web.socket.WebSocketMessageBroker
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.domain.chatroom.service.ChatRoomDomainService
import com.stark.shoot.domain.event.MessageSentEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * Listens for [MessageSentEvent] and sends chat room update information
 * to all participants via WebSocket.
 */
@Component
class MessageSentEventListener(
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val chatRoomDomainService: ChatRoomDomainService,
    private val chatRoomResponseMapper: ChatRoomResponseMapper,
    private val webSocketMessageBroker: WebSocketMessageBroker
) {

    private val logger = KotlinLogging.logger {}

    @EventListener
    fun handleMessageSent(event: MessageSentEvent) {
        val roomId = event.message.roomId
        val chatRoom = chatRoomQueryPort.findById(roomId) ?: run {
            logger.warn { "Chat room not found for roomId=$roomId" }
            return
        }

        chatRoom.participants.forEach { participant ->
            val titles = chatRoomDomainService.prepareChatRoomTitles(listOf(chatRoom), participant)
            val lastMessages = chatRoomDomainService.prepareLastMessages(listOf(chatRoom))
            val timestamps = chatRoomDomainService.prepareTimestamps(listOf(chatRoom))
            val response = chatRoomResponseMapper.toResponse(
                chatRoom,
                participant,
                titles[roomId.value] ?: chatRoom.title?.value ?: "",
                lastMessages[roomId.value] ?: "",
                timestamps[roomId.value] ?: ""
            )
            webSocketMessageBroker.sendMessage("/topic/rooms/${participant.value}", response)
        }
    }

}
