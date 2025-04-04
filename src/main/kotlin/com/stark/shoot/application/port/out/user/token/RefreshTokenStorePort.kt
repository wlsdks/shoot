package com.stark.shoot.application.port.out.user.token

import org.bson.types.ObjectId

interface RefreshTokenStorePort {
    fun storeRefreshToken(userId: ObjectId, refreshToken: String)
    fun isValidRefreshToken(refreshToken: String): Boolean
}