package com.stark.shoot.domain.service.user.block

import com.stark.shoot.domain.chat.user.User
import org.springframework.stereotype.Service

@Service
class UserBlockDomainService {
    fun block(currentUser: User, targetId: Long): User {
        require(currentUser.id != targetId) { "자신을 차단할 수 없습니다." }
        return currentUser.blockUser(targetId)
    }

    fun unblock(currentUser: User, targetId: Long): User {
        return currentUser.unblockUser(targetId)
    }
}
