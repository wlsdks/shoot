package com.stark.shoot.adapter.`in`.web

import com.stark.shoot.adapter.`in`.web.dto.user.CreateUserRequest
import com.stark.shoot.adapter.`in`.web.dto.user.UserResponse
import com.stark.shoot.application.port.`in`.user.UserCreateUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/users")
@RestController
class UserController(
    private val userCreateUseCase: UserCreateUseCase
) {

    @PostMapping
    fun createUser(@RequestBody request: CreateUserRequest): ResponseEntity<UserResponse> {
        val user = userCreateUseCase.createUser(request.username, request.nickname)
        val userResponse = UserResponse(user.id.toString(), user.username, user.nickname)
        return ResponseEntity.ok(userResponse)
    }

}
