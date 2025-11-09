package com.stark.shoot.application.port.out.user.group

import com.stark.shoot.domain.social.FriendGroup

interface SaveFriendGroupPort {
    fun save(group: FriendGroup): FriendGroup
}
