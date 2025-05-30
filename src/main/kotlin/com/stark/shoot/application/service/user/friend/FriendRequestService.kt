package com.stark.shoot.application.service.user.friend

import com.stark.shoot.application.port.`in`.user.friend.FriendRequestUseCase
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.application.port.out.user.friend.FriendCachePort
import com.stark.shoot.application.port.out.user.friend.UpdateFriendPort
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.InvalidInputException
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class FriendRequestService(
    private val findUserPort: FindUserPort,
    private val updateFriendPort: UpdateFriendPort,
    private val friendDomainService: com.stark.shoot.domain.service.user.FriendDomainService,
    private val friendCacheManager: FriendCacheManager
) : FriendRequestUseCase {

    /**
     * 친구 요청을 보냅니다.
     *
     * @param currentUserId 현재 사용자 ID
     * @param targetUserId 친구 요청을 받을 사용자 ID
     */
    override fun sendFriendRequest(
        currentUserId: Long,
        targetUserId: Long
    ) {
        // 사용자 존재 여부 확인
        if (!findUserPort.existsById(currentUserId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $currentUserId")
        }

        if (!findUserPort.existsById(targetUserId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $targetUserId")
        }

        // 도메인 서비스를 사용하여 친구 요청 유효성 검증
        try {
            friendDomainService.validateFriendRequest(
                currentUserId = currentUserId,
                targetUserId = targetUserId,
                isFriend = findUserPort.checkFriendship(currentUserId, targetUserId),
                hasOutgoingRequest = findUserPort.checkOutgoingFriendRequest(currentUserId, targetUserId),
                hasIncomingRequest = findUserPort.checkIncomingFriendRequest(currentUserId, targetUserId)
            )
        } catch (e: IllegalArgumentException) {
            throw InvalidInputException(e.message ?: "친구 요청 유효성 검증 실패")
        }

        // 친구 요청 추가
        updateFriendPort.addOutgoingFriendRequest(currentUserId, targetUserId)

        // 캐시 무효화 (FriendCacheManager 사용)
        friendCacheManager.invalidateFriendshipCaches(currentUserId, targetUserId)
    }

    /**
     * 친구 요청을 취소합니다.
     *
     * @param currentUserId 현재 사용자 ID
     * @param targetUserId 친구 요청을 받은 사용자 ID
     */
    override fun cancelFriendRequest(
        currentUserId: Long,
        targetUserId: Long
    ) {
        // 사용자 조회 (친구 요청 정보 포함)
        val currentUser = findUserPort.findUserWithFriendRequestsById(currentUserId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $currentUserId")

        val targetUser = findUserPort.findUserById(targetUserId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $targetUserId")

        // 친구 요청 존재 여부 확인
        if (!findUserPort.checkOutgoingFriendRequest(currentUserId, targetUserId)) {
            throw InvalidInputException("해당 친구 요청이 존재하지 않습니다.")
        }

        // 도메인 서비스를 사용하여 친구 요청 거절 처리
        val result = friendDomainService.processFriendReject(
            currentUser = targetUser, // 대상 사용자가 현재 사용자의 요청을 거절하는 것과 동일
            requester = currentUser,  // 현재 사용자가 요청자
            requesterId = currentUserId
        )

        // 업데이트된 사용자 정보 저장
        updateFriendPort.updateFriends(result.updatedCurrentUser)
        updateFriendPort.updateFriends(result.updatedRequester)

        // 실제 친구 요청 데이터 삭제
        updateFriendPort.removeOutgoingFriendRequest(currentUserId, targetUserId)

        // 캐시 무효화 (FriendCacheManager 사용)
        friendCacheManager.invalidateFriendshipCaches(currentUserId, targetUserId)
    }


}
