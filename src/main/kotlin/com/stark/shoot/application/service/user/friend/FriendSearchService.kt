package com.stark.shoot.application.service.user.friend

import com.stark.shoot.adapter.`in`.web.dto.user.FriendResponse
import com.stark.shoot.application.port.`in`.user.friend.FriendSearchUseCase
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException

@UseCase
class FriendSearchService(
    private val findUserPort: FindUserPort
) : FriendSearchUseCase {

    /**
     * 잠재적 친구 검색
     *
     * @param currentUserId 현재 사용자 ID
     * @param query 검색어
     * @return 친구 목록
     */
    override fun searchPotentialFriends(
        currentUserId: UserId,
        query: String
    ): List<FriendResponse> {
        // 현재 사용자 조회
        val currentUser = findUserPort.findUserWithAllRelationshipsById(currentUserId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $currentUserId")

        // 제외할 사용자 목록: 본인, 이미 친구, 받은/보낸 친구 요청 대상
        val excludedIds = mutableSetOf<UserId>().apply {
            add(currentUserId)
            addAll(currentUser.friendIds)
            addAll(currentUser.incomingFriendRequestIds)
            addAll(currentUser.outgoingFriendRequestIds)
        }

        // 검색어로 사용자 검색 (DB에서 검색)
        val allUsers = findUserPort.findAll()

        // 필터링된 사용자 목록
        return allUsers.filter { user ->
            user.id != null && !excludedIds.contains(user.id) &&
                    (user.username.value.contains(query, ignoreCase = true) ||
                            user.nickname.value.contains(query, ignoreCase = true))
        }.map { user ->
            FriendResponse(
                id = user.id?.value ?: 0L,
                username = user.username.value,
                nickname = user.nickname.value,
                profileImageUrl = user.profileImageUrl?.value
            )
        }
    }

}