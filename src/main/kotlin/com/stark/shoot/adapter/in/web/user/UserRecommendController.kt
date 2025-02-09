package com.stark.shoot.adapter.`in`.web.user

import com.stark.shoot.adapter.`in`.web.dto.user.RecommendedUserResponse
import com.stark.shoot.application.port.`in`.user.RetrieveUserUseCase
import com.stark.shoot.infrastructure.common.util.toObjectId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "친구 추천", description = "친구 추천 API")
@RestController
@RequestMapping("/api/v1/friends")
class UserRecommendController(
    private val retrieveUserUseCase: RetrieveUserUseCase
) {

    @Operation(summary = "친구 추천", description = "랜덤으로 N명을 추출 (자기 자신 제외)")
    @GetMapping("/recommend")
    fun recommendFriends(
        @RequestParam userId: String,
        @RequestParam(defaultValue = "3") limit: Int
    ): List<RecommendedUserResponse> {
        // 로그인한 사용자의 ObjectId로 변환
        val randomUsers = retrieveUserUseCase.findRandomUsers(userId.toObjectId(), limit)
        return randomUsers.map { user ->
            RecommendedUserResponse(
                id = user.id.toString(),
                username = user.username,
                nickname = user.nickname
            )
        }
    }

}