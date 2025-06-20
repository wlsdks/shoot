package com.stark.shoot.application.service.user.friend

import com.stark.shoot.adapter.`in`.web.dto.user.FriendResponse
import com.stark.shoot.application.port.`in`.user.friend.FindFriendUseCase
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException

@UseCase
class FindFriendService(
    private val findUserPort: FindUserPort
) : FindFriendUseCase {

    /**
     * 친구 목록 조회
     *
     * @param currentUserId 현재 사용자 ID (Long 타입)
     * @return 친구 정보를 담은 FriendResponse 목록
     */
    override fun getFriends(
        currentUserId: UserId
    ): List<FriendResponse> {
        // 현재 사용자 조회 (친구 관계 정보 포함)
        val user = findUserPort.findUserWithFriendshipsById(currentUserId)
            ?: throw ResourceNotFoundException("User not found")

        // 친구 목록은 도메인 User 객체의 friendIds 필드로 관리한다고 가정합니다.
        return user.friendIds.map { friendId ->
            val friend = findUserPort.findUserById(friendId)
                ?: throw ResourceNotFoundException("Friend not found: $friendId")

            FriendResponse(
                id = friend.id?.value ?: 0L,
                username = friend.username.value,
                nickname = friend.nickname.value,
                profileImageUrl = friend.profileImageUrl?.value
            )
        }
    }

    /**
     * 받은 친구 요청 목록 조회
     *
     * @param currentUserId 현재 사용자 ID
     * @return 받은 친구 요청 정보를 담은 FriendResponse 목록
     */
    override fun getIncomingFriendRequests(
        currentUserId: UserId
    ): List<FriendResponse> {
        // 현재 사용자 조회 (친구 요청 정보 포함)
        val user = findUserPort.findUserWithFriendRequestsById(currentUserId)
            ?: throw ResourceNotFoundException("User not found")

        // incomingFriendRequestIds는 받은 친구 요청 대상 User ID들의 집합으로 가정합니다.
        return user.incomingFriendRequestIds.map { requesterId ->
            val requester = findUserPort.findUserById(requesterId)
                ?: throw ResourceNotFoundException("Requester not found: $requesterId")

            FriendResponse(
                id = requester.id?.value ?: 0L,
                username = requester.username.value,
                nickname = requester.nickname.value,
                profileImageUrl = requester.profileImageUrl?.value
            )
        }
    }

    /**
     * 보낸 친구 요청 목록 조회
     *
     * @param currentUserId 현재 사용자 ID
     * @return 보낸 친구 요청 정보를 담은 FriendResponse 목록
     */
    override fun getOutgoingFriendRequests(
        currentUserId: UserId
    ): List<FriendResponse> {
        // 현재 사용자 조회 (친구 요청 정보 포함)
        val user = findUserPort.findUserWithFriendRequestsById(currentUserId)
            ?: throw ResourceNotFoundException("User not found")

        // outgoingFriendRequestIds는 보낸 친구 요청 대상 User ID들의 집합으로 가정합니다.
        return user.outgoingFriendRequestIds.map { targetId ->
            val target = findUserPort.findUserById(targetId)
                ?: throw ResourceNotFoundException("Target not found: $targetId")

            FriendResponse(
                id = target.id?.value ?: 0L,
                username = target.username.value,
                nickname = target.nickname.value,
                profileImageUrl = target.profileImageUrl?.value
            )
        }
    }

}
