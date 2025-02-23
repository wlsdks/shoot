package com.stark.shoot.application.service.user

import com.stark.shoot.adapter.`in`.web.dto.user.FriendResponse
import com.stark.shoot.application.port.`in`.user.RetrieveUserUseCase
import com.stark.shoot.application.port.out.user.RetrieveUserPort
import com.stark.shoot.domain.chat.user.User
import org.bson.types.ObjectId
import org.springframework.stereotype.Service

@Service
class RetrieveUserService(
    private val retrieveUserPort: RetrieveUserPort
) : RetrieveUserUseCase {

    /**
     * 유저 조회
     */
    override fun findById(
        id: ObjectId
    ): User? {
        return retrieveUserPort.findById(id)
    }

    /**
     * 사용자명으로 사용자 조회
     */
    override fun findByUsername(
        username: String
    ): User? {
        return retrieveUserPort.findByUsername(username)
    }

    /**
     * 사용자 코드로 사용자 조회
     */
    override fun findByUserCode(
        userCode: String
    ): User? {
        return retrieveUserPort.findByUserCode(userCode)
    }

    /**
     * 자기 자신을 제외한 임의의 유저들 N명 조회
     */
    override fun findRandomUsers(
        excludeId: ObjectId,
        limit: Int
    ): List<User> {
        return retrieveUserPort.findRandomUsers(excludeId, limit)
    }

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
        return retrieveUserPort.findBFSRecommendedUsers(userId, maxDepth, limit).map {
            FriendResponse(id = it.id.toString(), username = it.username)
        }
    }

}