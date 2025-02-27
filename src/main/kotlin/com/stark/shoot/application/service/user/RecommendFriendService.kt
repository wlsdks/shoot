package com.stark.shoot.application.service.user

import com.stark.shoot.adapter.`in`.web.dto.user.FriendResponse
import com.stark.shoot.application.port.`in`.user.RecommendFriendsUseCase
import com.stark.shoot.application.port.out.user.RecommendFriendPort
import com.stark.shoot.application.port.out.user.RetrieveUserPort
import org.bson.types.ObjectId
import org.springframework.stereotype.Service

@Service
class RecommendFriendService(
    private val recommendFriendPort: RecommendFriendPort
) : RecommendFriendsUseCase {

    /**
     * BFS를 이용한 친구 추천
     * - maxDepth: 친구 네트워크 탐색 최대 깊이 (예: 2단계까지)
     * - 내부적으로 MongoDB 집계 파이프라인($graphLookup)으로 최적화된 쿼리를 수행
     */
    override fun findBFSRecommendedUsers(
        userId: ObjectId,
        maxDepth: Int,
        limit: Int
    ): List<FriendResponse> {
        // maxDepth는 필요에 따라 조정 (여기서는 2단계까지)
        return recommendFriendPort.findBFSRecommendedUsers(userId, maxDepth, limit).map {
            FriendResponse(id = it.id.toString(), username = it.username)
        }
    }

}