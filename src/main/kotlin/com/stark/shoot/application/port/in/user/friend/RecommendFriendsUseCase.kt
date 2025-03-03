package com.stark.shoot.application.port.`in`.user.friend

import com.stark.shoot.adapter.`in`.web.dto.user.FriendResponse
import org.bson.types.ObjectId

interface RecommendFriendsUseCase {
    fun findBFSRecommendedUsers(userId: ObjectId, maxDepth: Int, skip: Int, limit: Int): List<FriendResponse>
}