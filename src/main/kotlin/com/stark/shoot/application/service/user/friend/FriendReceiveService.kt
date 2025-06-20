package com.stark.shoot.application.service.user.friend

import com.stark.shoot.application.port.`in`.user.friend.FriendReceiveUseCase
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.application.port.out.user.friend.UpdateFriendPort
import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.domain.common.vo.UserId
import com.stark.shoot.domain.service.user.FriendDomainService
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.InvalidInputException
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class FriendReceiveService(
    private val findUserPort: FindUserPort,
    private val updateFriendPort: UpdateFriendPort,
    private val eventPublisher: EventPublisher,
    private val friendDomainService: FriendDomainService,
    private val friendCacheManager: FriendCacheManager
) : FriendReceiveUseCase {

    /**
     * 친구 요청을 수락합니다.
     *
     * @param currentUserId 현재 사용자 ID
     * @param requesterId 친구 요청을 보낸 사용자 ID
     */
    override fun acceptFriendRequest(
        currentUserId: UserId,
        requesterId: UserId
    ) {
        // 사용자 조회 및 유효성 검증
        val (currentUser, requester) = retrieveAndValidateUsers(
            currentUserId,
            requesterId,
            "친구 요청 수락 유효성 검증 실패"
        )

        // 도메인 서비스를 사용하여 친구 요청 수락 처리
        val result = friendDomainService.processFriendAccept(
            currentUser = currentUser,
            requester = requester,
            requesterId = requesterId
        )

        // 업데이트된 사용자 정보 저장
        updateFriendPort.removeIncomingFriendRequest(currentUserId, requesterId)
        updateFriendPort.removeOutgoingFriendRequest(requesterId, currentUserId)
        updateFriendPort.addFriendRelation(currentUserId, requesterId)
        updateFriendPort.addFriendRelation(requesterId, currentUserId)

        // 이벤트 발행
        result.events.forEach { event ->
            eventPublisher.publish(event)
        }

        // 캐시 무효화
        friendCacheManager.invalidateFriendshipCaches(currentUserId, requesterId)
    }

    /**
     * 친구 요청을 거절합니다.
     *
     * @param currentUserId 현재 사용자 ID
     * @param requesterId 친구 요청을 보낸 사용자 ID
     */
    override fun rejectFriendRequest(
        currentUserId: UserId,
        requesterId: UserId
    ) {
        // 사용자 조회 및 유효성 검증
        val (currentUser, requester) = retrieveAndValidateUsers(
            currentUserId,
            requesterId,
            "친구 요청 거절 유효성 검증 실패"
        )

        // 도메인 서비스를 사용하여 친구 요청 거절 처리
        val result = friendDomainService.processFriendReject(
            currentUser = currentUser,
            requester = requester,
            requesterId = requesterId
        )

        // 업데이트된 사용자 정보 저장
        updateFriendPort.removeIncomingFriendRequest(currentUserId, requesterId)
        updateFriendPort.removeOutgoingFriendRequest(requesterId, currentUserId)

        // 캐시 무효화
        friendCacheManager.invalidateFriendshipCaches(currentUserId, requesterId)
    }

    /**
     * 사용자 조회 및 유효성 검증을 수행하는 공통 메서드
     *
     * @param currentUserId 현재 사용자 ID
     * @param requesterId 요청자 ID
     * @param validationErrorMessage 유효성 검증 실패 시 표시할 메시지
     * @return Pair<User, User> 현재 사용자와 요청자 객체 쌍
     */
    private fun retrieveAndValidateUsers(
        currentUserId: UserId,
        requesterId: UserId,
        validationErrorMessage: String
    ): Pair<User, User> {
        // 사용자 조회 (친구 요청 정보 포함)
        val currentUser = findUserPort.findUserWithFriendRequestsById(currentUserId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $currentUserId")

        val requester = findUserPort.findUserById(requesterId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $requesterId")

        // 도메인 서비스를 사용하여 친구 요청 유효성 검증
        try {
            friendDomainService.validateFriendAccept(currentUser, requesterId)
        } catch (e: IllegalArgumentException) {
            throw InvalidInputException(e.message ?: validationErrorMessage)
        }

        return Pair(currentUser, requester)
    }

}
