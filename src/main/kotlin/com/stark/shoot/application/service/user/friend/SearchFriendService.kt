package com.stark.shoot.application.service.user.friend

import com.stark.shoot.adapter.`in`.web.dto.user.FriendResponse
import com.stark.shoot.application.port.`in`.user.friend.SearchFriendUseCase
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.bson.types.ObjectId

@UseCase
class SearchFriendService(
    private val findUserPort: FindUserPort
) : SearchFriendUseCase {

    /**
     * 잠재적 친구 검색
     *
     * @param currentUserId 현재 사용자 ID
     * @param query 검색어
     * @return 친구 목록
     */
    override fun searchPotentialFriends(
        currentUserId: ObjectId,
        query: String
    ): List<FriendResponse> {
        // 현재 사용자 조회
        val currentUser = findUserPort.findUserById(currentUserId)
            ?: throw ResourceNotFoundException("User not found: $currentUserId")

        // 제외할 사용자 목록: 본인, 이미 친구, 받은/보낸 친구 요청 대상
        val excludedIds = mutableSetOf<ObjectId>().apply {
            add(currentUserId)
            addAll(currentUser.friends)
            addAll(currentUser.incomingFriendRequests)
            addAll(currentUser.outgoingFriendRequests)
        }

        // 모든 사용자 조회 (대규모 DB의 경우 효율적 쿼리로 대체 필요)
        val allUsers = findUserPort.findAll()

        // username 또는 nickname에 검색어(query)가 포함된 사용자 필터링 (대소문자 무시)
        return allUsers.filter { user ->
            user.id != null &&
                    !excludedIds.contains(user.id) &&
                    (user.username.contains(query, ignoreCase = true) || user.nickname.contains(
                        query,
                        ignoreCase = true
                    ))
        }.map { FriendResponse(id = it.id.toString(), username = it.username) }
    }

}