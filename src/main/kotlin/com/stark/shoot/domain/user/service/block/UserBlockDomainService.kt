package com.stark.shoot.domain.user.service.block

import com.stark.shoot.domain.user.User
import com.stark.shoot.domain.user.vo.UserId

class UserBlockDomainService {
    fun block(currentUser: User, targetId: UserId): User {
        require(currentUser.id != targetId) { "자신을 차단할 수 없습니다." }
        return currentUser.blockUser(targetId)
    }

    fun unblock(currentUser: User, targetId: UserId): User {
        return currentUser.unblockUser(targetId)
    }
}
