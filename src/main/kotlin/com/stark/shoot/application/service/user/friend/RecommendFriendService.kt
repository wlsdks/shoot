package com.stark.shoot.application.service.user.friend

import com.stark.shoot.adapter.`in`.web.dto.user.FriendResponse
import com.stark.shoot.application.port.`in`.user.friend.RecommendFriendsUseCase
import com.stark.shoot.application.port.out.user.friend.RecommendFriendPort
import org.bson.types.ObjectId

//@Service
class RecommendFriendService(
    private val recommendFriendPort: RecommendFriendPort
) : RecommendFriendsUseCase {

    /**
     * BFS를 이용한 친구 추천
     * - maxDepth: 친구 네트워크 탐색 최대 깊이 (예: 2단계까지)
     * - skip: 이미 반환한 결과 수 (페이지네이션)
     * - 내부적으로 MongoDB 집계 파이프라인($graphLookup)으로 최적화된 쿼리를 수행
     */
    override fun findBFSRecommendedUsers(
        userId: ObjectId,
        maxDepth: Int,
        skip: Int,
        limit: Int
    ): List<FriendResponse> {
        return recommendFriendPort.findBFSRecommendedUsers(userId, maxDepth, skip, limit).map {
            FriendResponse(id = it.id.toString(), username = it.username)
        }
    }

}