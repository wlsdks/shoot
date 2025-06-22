package com.stark.shoot.application.service.user.friend

import com.stark.shoot.application.port.`in`.user.friend.FriendRequestUseCase
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.application.port.out.user.friend.FriendRequestCommandPort
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
class FriendRequestService(
    private val findUserPort: FindUserPort,
    private val friendRequestCommandPort: FriendRequestCommandPort,
    private val friendDomainService: FriendDomainService,
    private val friendCacheManager: FriendCacheManager
) : FriendRequestUseCase {

    /**
     * 친구 요청을 보냅니다.
     *
     * @param currentUserId 현재 사용자 ID
     * @param targetUserId 친구 요청을 받을 사용자 ID
     */
    override fun sendFriendRequest(
        currentUserId: UserId,
        targetUserId: UserId
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

        // 친구 요청 애그리게이트 생성 및 저장
        val request = FriendRequest(senderId = currentUserId, receiverId = targetUserId)
        friendRequestCommandPort.saveFriendRequest(request)

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
        currentUserId: UserId,
        targetUserId: UserId
    ) {
        // 두 사용자 존재 여부 확인
        if (!findUserPort.existsById(currentUserId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $currentUserId")
        }
        if (!findUserPort.existsById(targetUserId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $targetUserId")
        }

        // 친구 요청 존재 여부 확인
        if (!findUserPort.checkOutgoingFriendRequest(currentUserId, targetUserId)) {
            throw InvalidInputException("해당 친구 요청이 존재하지 않습니다.")
        }

        // 요청 상태를 취소로 변경
        friendRequestCommandPort.updateStatus(currentUserId, targetUserId, FriendRequestStatus.CANCELLED)

        // 캐시 무효화 (FriendCacheManager 사용)
        friendCacheManager.invalidateFriendshipCaches(currentUserId, targetUserId)
    }

}
