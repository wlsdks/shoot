package com.stark.shoot.application.service.chatroom

import com.stark.shoot.application.port.`in`.chatroom.SseEmitterUseCase
import com.stark.shoot.domain.chat.event.ChatRoomCreatedEvent
import com.stark.shoot.domain.chat.event.FriendAddedEvent
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Service
class SseEmitterService : SseEmitterUseCase {

    private val emitters = ConcurrentHashMap<String, SseEmitter>()

    override fun createEmitter(
        userId: String
    ): SseEmitter {
        // 타임아웃 0은 무제한. 하지만 장기 연결 시 주기적인 heartbeat 전송으로 연결 상태를 유지합니다.
        val emitter = SseEmitter(0L)
        emitters[userId] = emitter                        // 연결 저장
        emitter.onCompletion { emitters.remove(userId) }  // 연결 종료시 제거
        emitter.onTimeout { emitters.remove(userId) }     // 타임아웃시 제거
        emitter.onError { emitters.remove(userId) }
        // Heartbeat 전송 (스레드 풀 사용)
        val scheduler = Executors.newSingleThreadScheduledExecutor()
        scheduler.scheduleAtFixedRate({
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data("ping"))
            } catch (e: Exception) {
                emitters.remove(userId)
                scheduler.shutdown()
            }
        }, 15, 15, TimeUnit.SECONDS)

        return emitter
    }

    override fun sendUpdate(
        userId: String,
        roomId: String,
        unreadCount: Int,
        lastMessage: String?
    ) {
        emitters[userId]?.let { emitter ->
            try {
                // 에러 발생 시 간단한 문자열 메시지로 전송
                emitter.send(
                    SseEmitter.event()
                        .data("""{"roomId": "$roomId", "unreadCount": $unreadCount, "lastMessage": "$lastMessage"}""")
                )
            } catch (e: Exception) {
                emitters.remove(userId)
            }
        }
    }

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

}