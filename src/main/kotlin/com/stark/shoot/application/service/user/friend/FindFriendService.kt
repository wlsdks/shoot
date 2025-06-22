package com.stark.shoot.application.service.user.friend

import com.stark.shoot.adapter.`in`.web.dto.user.FriendResponse
import com.stark.shoot.application.port.`in`.user.friend.FindFriendUseCase
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.application.port.out.user.friend.FriendRequestPort
import com.stark.shoot.application.port.out.user.friend.FriendshipPort
import com.stark.shoot.domain.user.type.FriendRequestStatus
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException

@UseCase
class FindFriendService(
    private val findUserPort: FindUserPort,
    private val friendshipPort: FriendshipPort,
    private val friendRequestPort: FriendRequestPort
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
        // 사용자 존재 여부 확인
        if (!findUserPort.existsById(currentUserId)) {
            throw ResourceNotFoundException("User not found: $currentUserId")
        }

        // 친구 관계 조회
        val friendships = friendshipPort.findAllFriendships(currentUserId)

        // 친구 정보 조회 및 응답 생성
        return friendships.map { friendship ->
            val friendId = friendship.friendId
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
        // 사용자 존재 여부 확인
        if (!findUserPort.existsById(currentUserId)) {
            throw ResourceNotFoundException("User not found: $currentUserId")
        }

        // 받은 친구 요청 조회
        val incomingRequests = friendRequestPort.findAllReceivedRequests(
            receiverId = currentUserId,
            status = FriendRequestStatus.PENDING
        )

        // 요청자 정보 조회 및 응답 생성
        return incomingRequests.map { request ->
            val requesterId = request.senderId
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
        // 사용자 존재 여부 확인
        if (!findUserPort.existsById(currentUserId)) {
            throw ResourceNotFoundException("User not found: $currentUserId")
        }

        // 보낸 친구 요청 조회
        val outgoingRequests = friendRequestPort.findAllSentRequests(
            senderId = currentUserId,
            status = FriendRequestStatus.PENDING
        )

        // 대상자 정보 조회 및 응답 생성
        return outgoingRequests.map { request ->
            val targetId = request.receiverId
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
