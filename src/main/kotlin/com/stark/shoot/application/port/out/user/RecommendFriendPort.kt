package com.stark.shoot.application.port.out.user

import com.stark.shoot.domain.chat.user.User
import org.bson.types.ObjectId

interface RecommendFriendPort {
    fun findBFSRecommendedUsers(userId: ObjectId, maxDepth: Int, limit: Int): List<User>
}