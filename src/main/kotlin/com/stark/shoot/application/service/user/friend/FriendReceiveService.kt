package com.stark.shoot.application.service.user.friend

import com.stark.shoot.application.port.`in`.user.friend.FriendReceiveUseCase
import com.stark.shoot.application.port.`in`.user.friend.command.AcceptFriendRequestCommand
import com.stark.shoot.application.port.`in`.user.friend.command.RejectFriendRequestCommand
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.application.port.out.user.friend.FriendRequestCommandPort
import com.stark.shoot.application.port.out.user.friend.FriendRequestQueryPort
import com.stark.shoot.application.port.out.user.friend.UpdateFriendPort
import com.stark.shoot.domain.user.FriendRequest
import com.stark.shoot.domain.user.service.FriendDomainService
import com.stark.shoot.domain.user.type.FriendRequestStatus
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.InvalidInputException
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class FriendReceiveService(
    private val findUserPort: FindUserPort,
    private val updateFriendPort: UpdateFriendPort,
    private val friendRequestQueryPort: FriendRequestQueryPort,
    private val friendRequestCommandPort: FriendRequestCommandPort,
    private val eventPublisher: EventPublisher,
    private val friendDomainService: FriendDomainService,
    private val friendCacheManager: FriendCacheManager
) : FriendReceiveUseCase {

    /**
     * 친구 요청을 수락합니다.
     *
     * @param command 친구 요청 수락 커맨드
     */
    override fun acceptFriendRequest(command: AcceptFriendRequestCommand) {
        val currentUserId = command.currentUserId
        val requesterId = command.requesterId

        // 친구 요청 조회 및 유효성 검사
        val friendRequest = findFriendRequest(currentUserId, requesterId)

        // 도메인 서비스를 사용하여 친구 요청 수락 처리
        val result = friendDomainService.processFriendAccept(friendRequest)

        // 친구 요청 상태 업데이트
        friendRequestCommandPort.updateStatus(requesterId, currentUserId, FriendRequestStatus.ACCEPTED)
        updateFriendPort.addFriendRelation(currentUserId, requesterId)
        updateFriendPort.addFriendRelation(requesterId, currentUserId)

        // 친구 관계 생성
        result.friendships.forEach { friendship ->
            updateFriendPort.addFriendRelation(friendship.userId, friendship.friendId)
        }

        // 이벤트 발행
        result.events.forEach { event -> eventPublisher.publish(event) }

        // 캐시 무효화
        friendCacheManager.invalidateFriendshipCaches(currentUserId, requesterId)
    }

    /**
     * 친구 요청을 거절합니다.
     *
     * @param command 친구 요청 거절 커맨드
     */
    override fun rejectFriendRequest(command: RejectFriendRequestCommand) {
        val currentUserId = command.currentUserId
        val requesterId = command.requesterId

        // 친구 요청 조회 및 유효성 검사
        val friendRequest = findFriendRequest(currentUserId, requesterId)

        // 친구 요청 상태 업데이트
        friendRequestCommandPort.updateStatus(requesterId, currentUserId, FriendRequestStatus.REJECTED)

        // 캐시 무효화
        friendCacheManager.invalidateFriendshipCaches(currentUserId, requesterId)
    }


    /**
     * 친구 요청을 조회합니다. (유효성 검사 포함)
     *
     * @param currentUserId 현재 사용자 ID
     * @param requesterId 친구 요청을 보낸 사용자 ID
     * @return 친구 요청 정보
     */
    private fun findFriendRequest(
        currentUserId: UserId,
        requesterId: UserId
    ): FriendRequest {
        // 사용자 존재 여부 확인
        if (!findUserPort.existsById(currentUserId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $currentUserId")
        }
        if (!findUserPort.existsById(requesterId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $requesterId")
        }

        // 친구 요청 조회
        return friendRequestQueryPort
            .findRequest(requesterId, currentUserId, FriendRequestStatus.PENDING)
            ?: throw InvalidInputException("해당 친구 요청이 존재하지 않습니다.")
    }

}
