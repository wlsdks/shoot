package com.stark.shoot.application.service.user.friend

import com.stark.shoot.adapter.`in`.web.dto.user.FriendResponse
import com.stark.shoot.application.port.`in`.user.friend.FindFriendUseCase
import com.stark.shoot.application.port.`in`.user.friend.command.GetFriendsCommand
import com.stark.shoot.application.port.`in`.user.friend.command.GetIncomingFriendRequestsCommand
import com.stark.shoot.application.port.`in`.user.friend.command.GetOutgoingFriendRequestsCommand
import com.stark.shoot.application.port.out.user.UserQueryPort
import com.stark.shoot.application.port.out.user.friend.relate.FriendshipQueryPort
import com.stark.shoot.application.port.out.user.friend.request.FriendRequestQueryPort
import com.stark.shoot.domain.user.type.FriendRequestStatus
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException

@UseCase
class FindFriendService(
    private val userQueryPort: UserQueryPort,
    private val friendshipQueryPort: FriendshipQueryPort,
    private val friendRequestQueryPort: FriendRequestQueryPort
) : FindFriendUseCase {

    /**
     * 친구 목록 조회
     *
     * @param command 친구 목록 조회 커맨드
     * @return 친구 정보를 담은 FriendResponse 목록
     */
    override fun getFriends(
        command: GetFriendsCommand
    ): List<FriendResponse> {
        val currentUserId = command.currentUserId

        // 사용자 존재 여부 확인
        if (!userQueryPort.existsById(currentUserId)) {
            throw ResourceNotFoundException("User not found: $currentUserId")
        }

        // 친구 관계 조회
        val friendships = friendshipQueryPort.findAllFriendships(currentUserId)

        // 친구 정보 조회 및 응답 생성
        return friendships.map { friendship ->
            val friendId = friendship.friendId
            val friend = userQueryPort.findUserById(friendId)
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
     * @param command 받은 친구 요청 목록 조회 커맨드
     * @return 받은 친구 요청 정보를 담은 FriendResponse 목록
     */
    override fun getIncomingFriendRequests(
        command: GetIncomingFriendRequestsCommand
    ): List<FriendResponse> {
        val currentUserId = command.currentUserId

        // 사용자 존재 여부 확인
        if (!userQueryPort.existsById(currentUserId)) {
            throw ResourceNotFoundException("User not found: $currentUserId")
        }

        // 받은 친구 요청 조회
        val incomingRequests = friendRequestQueryPort.findAllReceivedRequests(
            receiverId = currentUserId,
            status = FriendRequestStatus.PENDING
        )

        // 요청자 정보 조회 및 응답 생성
        return incomingRequests.map { request ->
            val requesterId = request.senderId
            val requester = userQueryPort.findUserById(requesterId)
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
     * @param command 보낸 친구 요청 목록 조회 커맨드
     * @return 보낸 친구 요청 정보를 담은 FriendResponse 목록
     */
    override fun getOutgoingFriendRequests(
        command: GetOutgoingFriendRequestsCommand
    ): List<FriendResponse> {
        val currentUserId = command.currentUserId

        // 사용자 존재 여부 확인
        if (!userQueryPort.existsById(currentUserId)) {
            throw ResourceNotFoundException("User not found: $currentUserId")
        }

        // 보낸 친구 요청 조회
        val outgoingRequests = friendRequestQueryPort.findAllSentRequests(
            senderId = currentUserId,
            status = FriendRequestStatus.PENDING
        )

        // 대상자 정보 조회 및 응답 생성
        return outgoingRequests.map { request ->
            val targetId = request.receiverId
            val target = userQueryPort.findUserById(targetId)
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
