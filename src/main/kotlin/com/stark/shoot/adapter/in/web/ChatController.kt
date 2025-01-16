package com.stark.shoot.adapter.`in`.web

import com.stark.shoot.application.port.`in`.CreateChatRoomUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/chatrooms")
@RestController
class ChatController(
    private val createChatRoomUseCase: CreateChatRoomUseCase
) {

    @PostMapping("/create")
    fun createCharRoom(): ResponseEntity<Unit> {
        return createChatRoomUseCase.create(null, setOf()).let {
            ResponseEntity.ok().build()
        }
    }

}