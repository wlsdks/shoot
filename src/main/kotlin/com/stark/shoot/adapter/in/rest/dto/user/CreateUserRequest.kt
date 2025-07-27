package com.stark.shoot.adapter.`in`.rest.dto.user

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import org.springframework.web.multipart.MultipartFile

data class CreateUserRequest(
    val username: String,
    val nickname: String,
    val password: String,
    val email: String,
    val bio: String? = null,
    @field:Parameter(content = [Content(mediaType = "multipart/form-data")])
    val profileImage: MultipartFile? = null
)