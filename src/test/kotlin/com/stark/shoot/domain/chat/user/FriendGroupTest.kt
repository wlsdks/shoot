package com.stark.shoot.domain.chat.user

import com.stark.shoot.domain.user.FriendGroup
import com.stark.shoot.domain.user.vo.FriendGroupName
import com.stark.shoot.domain.user.vo.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("FriendGroup 애그리게이트")
class FriendGroupTest {

    @Nested
    @DisplayName("그룹 정보 업데이트")
    inner class UpdateGroupInfo {

        @Test
        fun `그룹 이름을 변경할 수 있다`() {
            val group = FriendGroup(ownerId = UserId.from(1L), name = FriendGroupName.from("친구들"))
            val updated = group.rename("베프")
            assertThat(updated.name.value).isEqualTo("베프")
            assertThat(updated.updatedAt).isNotNull()
        }

        @Test
        fun `그룹 설명을 수정할 수 있다`() {
            val group = FriendGroup(ownerId = UserId.from(1L), name = FriendGroupName.from("친구들"))
            val updated = group.updateDescription("오래된 친구")
            assertThat(updated.description).isEqualTo("오래된 친구")
            assertThat(updated.updatedAt).isNotNull()
        }
    }

    @Nested
    @DisplayName("멤버 관리")
    inner class ManageMembers {

        @Test
        fun `멤버를 추가할 수 있다`() {
            val group = FriendGroup(ownerId = UserId.from(1L), name = FriendGroupName.from("친구들"))
            val updated = group.addMember(UserId.from(2L))
            assertThat(updated.memberIds).contains(UserId.from(2L))
        }

        @Test
        fun `이미 있는 멤버를 추가하면 변경이 없다`() {
            val group = FriendGroup(
                ownerId = UserId.from(1L),
                name = FriendGroupName.from("친구들"),
                memberIds = setOf(UserId.from(2L))
            )
            val updated = group.addMember(UserId.from(2L))
            assertThat(updated).isEqualTo(group)
        }

        @Test
        fun `멤버를 제거할 수 있다`() {
            val group = FriendGroup(
                ownerId = UserId.from(1L),
                name = FriendGroupName.from("친구들"),
                memberIds = setOf(UserId.from(2L), UserId.from(3L))
            )
            val updated = group.removeMember(UserId.from(2L))
            assertThat(updated.memberIds).doesNotContain(UserId.from(2L))
        }

        @Test
        fun `존재하지 않는 멤버를 제거하면 변경이 없다`() {
            val group =
                FriendGroup(
                    ownerId = UserId.from(1L),
                    name = FriendGroupName.from("친구들"),
                    memberIds = setOf(UserId.from(2L))
                )
            val updated = group.removeMember(UserId.from(3L))
            assertThat(updated).isEqualTo(group)
        }
    }
}
