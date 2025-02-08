package com.stark.shoot.adapter.`in`.web.user

import com.stark.shoot.application.port.`in`.user.RetrieveUserUseCase
import com.stark.shoot.infrastructure.common.util.toObjectId
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/friends")
class UserRecommendController(
    private val retrieveUserUseCase: RetrieveUserUseCase
) {

    @Operation(summary = "친구 추천", description = "랜덤으로 N명 추출 (자기 자신 제외)")
    @GetMapping("/recommend")
    fun recommendFriends(
        @RequestParam userId: String,
        @RequestParam(defaultValue = "3") limit: Int
    ): List<String> {
        // exclude 자기자신
        val randomUsers = retrieveUserUseCase.findRandomUsers(userId.toObjectId(), limit)
        return randomUsers.map { it.username }  // 혹은 userCode/nickname 등 원하는 필드로 응답
    }

}