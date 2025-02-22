package com.stark.shoot.application.service.chatroom

import com.stark.shoot.application.port.`in`.chatroom.SseEmitterUseCase
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

}