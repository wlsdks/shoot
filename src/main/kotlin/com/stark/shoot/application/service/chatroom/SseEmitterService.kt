package com.stark.shoot.application.service.chatroom

import com.stark.shoot.application.port.`in`.chatroom.SseEmitterUseCase
import com.stark.shoot.domain.chat.event.ChatRoomCreatedEvent
import com.stark.shoot.domain.chat.event.FriendAddedEvent
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap

@Service
class SseEmitterService : SseEmitterUseCase {

    private val emitters = ConcurrentHashMap<String, SseEmitter>()

    override fun createEmitter(
        userId: String
    ): SseEmitter {
        val emitter = SseEmitter(0L)              // 타임아웃 무제한
        emitters[userId] = emitter                        // 연결 저장
        emitter.onCompletion { emitters.remove(userId) }  // 연결 종료시 제거
        emitter.onTimeout { emitters.remove(userId) }     // 타임아웃시 제거
        // Heartbeat 추가 → 연결 유지
        Thread {
            while (true) {
                try {
                    emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
                    Thread.sleep(15000); // 15초마다 Heartbeat
                } catch (e: Exception) {
                    emitters.remove(userId)
                    break
                }
            }
        }.start();

        return emitter
    }

    override fun sendUpdate(
        userId: String,
        roomId: String,
        unreadCount: Int,
        lastMessage: String?
    ) {
        emitters[userId]?.let {
            try {
                // 클라이언트에 데이터 전송
                it.send(
                    SseEmitter.event()
                        .data("""{"roomId": "$roomId", "unreadCount": $unreadCount, "lastMessage": "$lastMessage"}""")
                )
            } catch (e: Exception) {
                // 에러 발생시 연결 제거
                emitters.remove(userId)
            }
        }
    }

    override fun sendChatRoomCreatedEvent(
        event: ChatRoomCreatedEvent
    ) {
        emitters[event.userId]?.let {
            try {
                // 채팅방 생성 이벤트 전송
                it.send(
                    SseEmitter.event()
                        .name("chatRoomCreated")
                        .data(mapOf("roomId" to event.roomId))
                )
            } catch (e: Exception) {
                emitters.remove(event.userId)
            }
        }
    }

    override fun sendFriendAddedEvent(
        event: FriendAddedEvent
    ) {
        emitters[event.userId]?.let {
            try {
                // 친구 추가 이벤트 전송
                it.send(
                    SseEmitter.event()
                        .name("friendAdded")
                        .data(mapOf("friendId" to event.friendId))
                )
            } catch (e: Exception) {
                emitters.remove(event.userId)
            }
        }
    }

}