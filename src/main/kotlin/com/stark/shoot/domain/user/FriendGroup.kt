package com.stark.shoot.domain.user

import com.stark.shoot.domain.user.vo.FriendGroupName
import com.stark.shoot.domain.user.vo.UserId
import java.time.Instant

/**
 * 친구를 그룹화하여 관리하기 위한 애그리게이트
 */
data class FriendGroup(
    val id: Long? = null,
    val ownerId: UserId,
    val name: FriendGroupName,
    val description: String? = null,
    val memberIds: Set<UserId> = emptySet(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant? = null,
) {
    /** 그룹 이름 변경 */
    fun rename(newName: String): FriendGroup {
        val nameVo = FriendGroupName.from(newName)
        return copy(name = nameVo, updatedAt = Instant.now())
    }

    /** 그룹 설명 업데이트 */
    fun updateDescription(newDescription: String?): FriendGroup {
        return copy(description = newDescription, updatedAt = Instant.now())
    }

    /** 그룹에 멤버 추가 */
    fun addMember(userId: UserId): FriendGroup {
        if (memberIds.contains(userId)) {
            return this
        }
        val updatedMembers = memberIds.toMutableSet()
        updatedMembers.add(userId)
        return copy(memberIds = updatedMembers, updatedAt = Instant.now())
    }

    /** 그룹에서 멤버 제거 */
    fun removeMember(userId: UserId): FriendGroup {
        if (!memberIds.contains(userId)) {
            return this
        }
        val updatedMembers = memberIds.toMutableSet()
        updatedMembers.remove(userId)
        return copy(memberIds = updatedMembers, updatedAt = Instant.now())
    }

}
