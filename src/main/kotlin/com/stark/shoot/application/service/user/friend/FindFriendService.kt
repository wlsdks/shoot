package com.stark.shoot.application.service.user.friend

import com.stark.shoot.adapter.`in`.rest.dto.social.friend.FriendResponse
import com.stark.shoot.application.port.`in`.user.friend.FindFriendUseCase
import com.stark.shoot.application.port.`in`.user.friend.command.GetFriendsCommand
import com.stark.shoot.application.port.`in`.user.friend.command.GetIncomingFriendRequestsCommand
import com.stark.shoot.application.port.`in`.user.friend.command.GetOutgoingFriendRequestsCommand
import com.stark.shoot.application.port.out.user.UserQueryPort
import com.stark.shoot.application.port.out.user.friend.relate.FriendshipQueryPort
import com.stark.shoot.application.port.out.user.friend.request.FriendRequestQueryPort
import com.stark.shoot.domain.social.type.FriendRequestStatus
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.domain.exception.web.ResourceNotFoundException

@UseCase
class FindFriendService(
    private val userQueryPort: UserQueryPort,
    private val friendshipQueryPort: FriendshipQueryPort,
    private val friendRequestQueryPort: FriendRequestQueryPort
) : FindFriendUseCase {

    /**
     * 친구 목록 조회
     * N+1 쿼리 문제를 해결하기 위해 배치 조회를 사용합니다.
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
        if (friendships.isEmpty()) {
            return emptyList()
        }

        // 친구 ID 목록 추출
        val friendIds = friendships.map { it.friendId }

        // 배치 조회로 친구 정보 조회 (N+1 문제 해결)
        val friends = userQueryPort.findAllByIds(friendIds)
        val friendsMap = friends.associateBy { it.id }

        // 친구 정보 응답 생성
        return friendships.mapNotNull { friendship ->
            val friend = friendsMap[friendship.friendId] ?: return@mapNotNull null

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
     * N+1 쿼리 문제를 해결하기 위해 배치 조회를 사용합니다.
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
        if (incomingRequests.isEmpty()) {
            return emptyList()
        }

        // 요청자 ID 목록 추출
        val requesterIds = incomingRequests.map { it.senderId }

        // 배치 조회로 요청자 정보 조회 (N+1 문제 해결)
        val requesters = userQueryPort.findAllByIds(requesterIds)
        val requestersMap = requesters.associateBy { it.id }

        // 요청자 정보 응답 생성
        return incomingRequests.mapNotNull { request ->
            val requester = requestersMap[request.senderId] ?: return@mapNotNull null

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
     * N+1 쿼리 문제를 해결하기 위해 배치 조회를 사용합니다.
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
        if (outgoingRequests.isEmpty()) {
            return emptyList()
        }

        // 대상자 ID 목록 추출
        val targetIds = outgoingRequests.map { it.receiverId }

        // 배치 조회로 대상자 정보 조회 (N+1 문제 해결)
        val targets = userQueryPort.findAllByIds(targetIds)
        val targetsMap = targets.associateBy { it.id }

        // 대상자 정보 응답 생성
        return outgoingRequests.mapNotNull { request ->
            val target = targetsMap[request.receiverId] ?: return@mapNotNull null

            FriendResponse(
                id = target.id?.value ?: 0L,
                username = target.username.value,
                nickname = target.nickname.value,
                profileImageUrl = target.profileImageUrl?.value
            )
        }
    }

}
