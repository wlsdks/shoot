package com.stark.shoot.domain.social

import com.stark.shoot.domain.social.vo.FriendGroupName
import com.stark.shoot.domain.shared.UserId
import java.time.Instant

/**
 * 친구를 그룹화하여 관리하기 위한 애그리게이트
 */
data class FriendGroup(
    val id: Long? = null,
    val ownerId: UserId,
    var name: FriendGroupName,
    var description: String? = null,
    var memberIds: Set<UserId> = emptySet(),
    var createdAt: Instant = Instant.now(),
    var updatedAt: Instant? = null,
) {
    /** 그룹 이름 변경 */
    fun rename(newName: String) {
        val nameVo = FriendGroupName.from(newName)
        name = nameVo
        updatedAt = Instant.now()
    }

    /** 그룹 설명 업데이트 */
    fun updateDescription(newDescription: String?) {
        description = newDescription
        updatedAt = Instant.now()
    }

    /** 그룹에 멤버 추가 */
    fun addMember(userId: UserId): Boolean {
        if (memberIds.contains(userId)) {
            return false
        }
        val updatedMembers = memberIds.toMutableSet()
        updatedMembers.add(userId)
        memberIds = updatedMembers
        updatedAt = Instant.now()
        return true
    }

    /** 그룹에서 멤버 제거 */
    fun removeMember(userId: UserId): Boolean {
        if (!memberIds.contains(userId)) {
            return false
        }
        val updatedMembers = memberIds.toMutableSet()
        updatedMembers.remove(userId)
        memberIds = updatedMembers
        updatedAt = Instant.now()
        return true
    }

}
