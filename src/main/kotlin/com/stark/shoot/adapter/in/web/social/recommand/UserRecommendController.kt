package com.stark.shoot.adapter.`in`.web.social.recommand

import com.stark.shoot.adapter.`in`.web.dto.user.FriendResponse
import com.stark.shoot.application.port.`in`.user.friend.RecommendFriendsUseCase
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
    private val recommendFriendsUseCase: RecommendFriendsUseCase
) {

    @Operation(
        summary = "BFS 추천",
        description = "BFS를 통해 친구 네트워크를 탐색하여 추천 후보를 반환 (자기 자신, 기존 친구, 친구 요청 제외)"
    )
    @GetMapping("/recommend/bfs")
    fun recommendFriendsBFS(
        @RequestParam userId: String,
        @RequestParam(defaultValue = "10") limit: Int,
        @RequestParam(defaultValue = "2") maxDepth: Int,
        @RequestParam(defaultValue = "0") skip: Int
    ): List<FriendResponse> {
        // 입력받은 userId를 ObjectId로 변환하여 BFS 탐색 후 추천 친구 목록을 반환
        return recommendFriendsUseCase.findBFSRecommendedUsers(userId.toObjectId(), maxDepth, skip, limit)
    }
    
}