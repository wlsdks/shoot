package com.stark.shoot.adapter.out.persistence.mongodb.adapter.user.token

import com.stark.shoot.adapter.out.persistence.mongodb.document.user.UserDocument
import com.stark.shoot.application.port.out.user.token.RefreshTokenStorePort
import com.stark.shoot.infrastructure.annotation.Adapter
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import java.time.Instant

@Adapter
class RefreshTokenStoreMongoAdapter(
    private val mongoTemplate: MongoTemplate
) : RefreshTokenStorePort {

    /**
     * RefreshToken 저장
     *
     * @param userId 유저의 ObjectId (몽고 DB의 _id)
     * @param refreshToken 리프레시 토큰
     */
    override fun storeRefreshToken(
        userId: ObjectId,
        refreshToken: String
    ) {
        val query = Query(Criteria.where("_id").`is`(userId))

        val update = Update()
            .set("refreshToken", refreshToken)
            .set("refreshTokenExpiration", Instant.now().plusSeconds(30 * 24 * 60 * 60)) // 30일

        mongoTemplate.updateFirst(query, update, UserDocument::class.java)
    }

    /**
     * Is valid refresh token
     *
     * @param refreshToken refresh token
     * @return 유효한 리프레시 토큰이면 true, 아니면 false
     */
    override fun isValidRefreshToken(
        refreshToken: String
    ): Boolean {
        val query = Query(
            Criteria.where("refreshToken").`is`(refreshToken)
                .and("refreshTokenExpiration").gt(Instant.now())
        )

        return mongoTemplate.exists(query, UserDocument::class.java)
    }

}