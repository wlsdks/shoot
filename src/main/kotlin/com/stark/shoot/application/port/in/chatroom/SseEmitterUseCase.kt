package com.stark.shoot.application.port.`in`.chatroom

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

interface SseEmitterUseCase {
    fun createEmitter(userId: String): SseEmitter
    fun sendUpdate(userId: String, roomId: String, unreadCount: Int, lastMessage: String?)
}