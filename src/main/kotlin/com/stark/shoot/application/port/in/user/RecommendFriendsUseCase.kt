package com.stark.shoot.application.port.`in`.user

import com.stark.shoot.adapter.`in`.web.dto.user.FriendResponse
import org.bson.types.ObjectId

interface RecommendFriendsUseCase {
    fun findBFSRecommendedUsers(userId: ObjectId, maxDepth: Int, limit: Int): List<FriendResponse>
}