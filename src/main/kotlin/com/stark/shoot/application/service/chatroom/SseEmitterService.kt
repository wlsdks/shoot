package com.stark.shoot.application.service.chatroom

import com.stark.shoot.application.port.`in`.chatroom.SseEmitterUseCase
import com.stark.shoot.domain.chat.event.ChatRoomCreatedEvent
import com.stark.shoot.domain.chat.event.FriendAddedEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Service
class SseEmitterService : SseEmitterUseCase {

    private val emitters = ConcurrentHashMap<String, SseEmitter>()
    private val logger = KotlinLogging.logger {}

    /**
     * 새로운 SSE emitter를 생성합니다.
     *
     * @param userId 사용자 ID
     * @return 생성된 SSE emitter
     */
    override fun createEmitter(
        userId: String
    ): SseEmitter {
        val emitter = SseEmitter(0L)
        logger.info { "Creating new SSE emitter for user: $userId" }
        emitters[userId] = emitter

        emitter.onCompletion {
            logger.info { "SSE connection completed for user: $userId" }
            emitters.remove(userId)
        }
        emitter.onTimeout {
            logger.info { "SSE connection timeout for user: $userId" }
            emitters.remove(userId)
        }
        emitter.onError {
            logger.error { "SSE connection error for user: $userId, error: ${it.message}" }
            emitters.remove(userId)
        }

        // Heartbeat 전송
        sendSseHeartBeat(emitter, userId)
        return emitter
    }

    /**
     * 사용자에게 업데이트를 전송합니다.
     *
     * @param userId 사용자 ID
     * @param roomId 채팅방 ID
     * @param unreadCount 읽지 않은 메시지 수
     * @param lastMessage 마지막 메시지
     */
    override fun sendUpdate(
        userId: String,
        roomId: String,
        unreadCount: Int,
        lastMessage: String?
    ) {
        emitters[userId]?.let { emitter ->
            try {
                logger.info { "Sending update to user: $userId, roomId: $roomId, unreadCount: $unreadCount, message: $lastMessage" }

                // 데이터 구조를 명확하게 하여 JSON 형식으로 보냄
                val data = mapOf(
                    "roomId" to roomId,
                    "unreadCount" to unreadCount,
                    "lastMessage" to (lastMessage ?: "")
                )

                emitter.send(SseEmitter.event().data(data))
            } catch (e: Exception) {
                logger.error { "Failed to send update to user: $userId, error: ${e.message}" }
                emitters.remove(userId)
            }
        } ?: logger.warn { "No SSE emitter found for user: $userId" }
    }

    /**
     * 사용자에게 채팅방 생성 이벤트를 전송합니다.
     *
     * @param event 이벤트
     */
    override fun sendChatRoomCreatedEvent(
        event: ChatRoomCreatedEvent
    ) {
        emitters[event.userId]?.let { emitter ->
            try {
                emitter.send(SseEmitter.event().name("chatRoomCreated").data(mapOf("roomId" to event.roomId)))
            } catch (e: Exception) {
                emitters.remove(event.userId)
            }
        }
    }

    /**
     * 사용자에게 친구 추가 이벤트를 전송합니다.
     *
     * @param event 이벤트
     */
    override fun sendFriendAddedEvent(
        event: FriendAddedEvent
    ) {
        emitters[event.userId]?.let { emitter ->
            try {
                emitter.send(SseEmitter.event().name("friendAdded").data(mapOf("friendId" to event.friendId)))
            } catch (e: Exception) {
                emitters.remove(event.userId)
            }
        }
    }

    /**
     * SSE emitter에 Heartbeat를 주기적으로 전송합니다.
     *
     * @param emitter SSE emitter
     * @param userId 사용자 ID
     */
    private fun sendSseHeartBeat(
        emitter: SseEmitter,
        userId: String
    ) {
        // Heartbeat 전송 스케줄러 생성
        val scheduler = Executors.newSingleThreadScheduledExecutor()

        // 15초마다 Heartbeat 전송
        scheduler.scheduleAtFixedRate({
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data("ping"))
                logger.debug { "Heartbeat sent to user: $userId" }
            } catch (e: Exception) {
                logger.error { "Failed to send heartbeat to user: $userId, error: ${e.message}" }
                emitters.remove(userId)
                scheduler.shutdown()
            }
        }, 15, 15, TimeUnit.SECONDS)
    }

}