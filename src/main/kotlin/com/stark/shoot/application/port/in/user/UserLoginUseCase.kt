package com.stark.shoot.application.port.`in`.user

interface UserLoginUseCase {
    fun login(username: String, password: String): String
}