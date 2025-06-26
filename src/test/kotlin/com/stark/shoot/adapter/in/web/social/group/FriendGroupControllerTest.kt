package com.stark.shoot.adapter.`in`.web.social.group

import com.stark.shoot.application.port.`in`.user.group.FindFriendGroupUseCase
import com.stark.shoot.application.port.`in`.user.group.ManageFriendGroupUseCase
import com.stark.shoot.domain.user.FriendGroup
import com.stark.shoot.domain.user.vo.FriendGroupName
import com.stark.shoot.domain.user.vo.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.time.Instant

@DisplayName("FriendGroupController 단위 테스트")
class FriendGroupControllerTest {

    private val manageUseCase = mock(ManageFriendGroupUseCase::class.java)
    private val findUseCase = mock(FindFriendGroupUseCase::class.java)
    private val controller = FriendGroupController(manageUseCase, findUseCase)

    @Test
    @DisplayName("[happy] 친구 그룹을 생성한다")
    fun `친구 그룹을 생성한다`() {
        // given
        val ownerId = 1L
        val name = "친한 친구들"
        val description = "자주 만나는 친구들"
        
        val group = createFriendGroup(1L, ownerId, name, description)
        
        `when`(manageUseCase.createGroup(UserId.from(ownerId), name, description))
            .thenReturn(group)

        // when
        val response = controller.createGroup(ownerId, name, description)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data?.id).isEqualTo(1L)
        assertThat(response.data?.ownerId).isEqualTo(ownerId)
        assertThat(response.data?.name).isEqualTo(name)
        assertThat(response.data?.description).isEqualTo(description)
        assertThat(response.data?.memberIds).isEmpty()

        verify(manageUseCase).createGroup(UserId.from(ownerId), name, description)
    }

    @Test
    @DisplayName("[happy] 친구 그룹의 이름을 변경한다")
    fun `친구 그룹의 이름을 변경한다`() {
        // given
        val groupId = 1L
        val newName = "새로운 그룹 이름"
        
        val group = createFriendGroup(groupId, 1L, newName, "설명")
        
        `when`(manageUseCase.renameGroup(groupId, newName))
            .thenReturn(group)

        // when
        val response = controller.renameGroup(groupId, newName)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data?.id).isEqualTo(groupId)
        assertThat(response.data?.name).isEqualTo(newName)

        verify(manageUseCase).renameGroup(groupId, newName)
    }

    @Test
    @DisplayName("[happy] 친구 그룹의 설명을 수정한다")
    fun `친구 그룹의 설명을 수정한다`() {
        // given
        val groupId = 1L
        val newDescription = "새로운 설명"
        
        val group = createFriendGroup(groupId, 1L, "그룹명", newDescription)
        
        `when`(manageUseCase.updateDescription(groupId, newDescription))
            .thenReturn(group)

        // when
        val response = controller.updateDescription(groupId, newDescription)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data?.id).isEqualTo(groupId)
        assertThat(response.data?.description).isEqualTo(newDescription)

        verify(manageUseCase).updateDescription(groupId, newDescription)
    }

    @Test
    @DisplayName("[happy] 친구 그룹에 멤버를 추가한다")
    fun `친구 그룹에 멤버를 추가한다`() {
        // given
        val groupId = 1L
        val memberId = 2L
        
        val group = createFriendGroup(
            id = groupId,
            ownerId = 1L,
            name = "그룹명",
            description = "설명",
            memberIds = setOf(memberId)
        )
        
        `when`(manageUseCase.addMember(groupId, UserId.from(memberId)))
            .thenReturn(group)

        // when
        val response = controller.addMember(groupId, memberId)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data?.id).isEqualTo(groupId)
        assertThat(response.data?.memberIds).containsExactly(memberId)

        verify(manageUseCase).addMember(groupId, UserId.from(memberId))
    }

    @Test
    @DisplayName("[happy] 친구 그룹에서 멤버를 제거한다")
    fun `친구 그룹에서 멤버를 제거한다`() {
        // given
        val groupId = 1L
        val memberId = 2L
        
        val group = createFriendGroup(
            id = groupId,
            ownerId = 1L,
            name = "그룹명",
            description = "설명",
            memberIds = emptySet()
        )
        
        `when`(manageUseCase.removeMember(groupId, UserId.from(memberId)))
            .thenReturn(group)

        // when
        val response = controller.removeMember(groupId, memberId)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data?.id).isEqualTo(groupId)
        assertThat(response.data?.memberIds).isEmpty()

        verify(manageUseCase).removeMember(groupId, UserId.from(memberId))
    }

    @Test
    @DisplayName("[happy] 친구 그룹을 삭제한다")
    fun `친구 그룹을 삭제한다`() {
        // given
        val groupId = 1L

        // when
        val response = controller.deleteGroup(groupId)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()

        verify(manageUseCase).deleteGroup(groupId)
    }

    @Test
    @DisplayName("[happy] 친구 그룹을 조회한다")
    fun `친구 그룹을 조회한다`() {
        // given
        val groupId = 1L
        
        val group = createFriendGroup(
            id = groupId,
            ownerId = 1L,
            name = "그룹명",
            description = "설명",
            memberIds = setOf(2L, 3L)
        )
        
        `when`(findUseCase.getGroup(groupId))
            .thenReturn(group)

        // when
        val response = controller.getGroup(groupId)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data?.id).isEqualTo(groupId)
        assertThat(response.data?.name).isEqualTo("그룹명")
        assertThat(response.data?.description).isEqualTo("설명")
        assertThat(response.data?.memberIds).containsExactlyInAnyOrder(2L, 3L)

        verify(findUseCase).getGroup(groupId)
    }

    @Test
    @DisplayName("[happy] 사용자의 친구 그룹 목록을 조회한다")
    fun `사용자의 친구 그룹 목록을 조회한다`() {
        // given
        val ownerId = 1L
        
        val groups = listOf(
            createFriendGroup(1L, ownerId, "첫 번째 그룹", "첫 번째 설명", setOf(2L, 3L)),
            createFriendGroup(2L, ownerId, "두 번째 그룹", "두 번째 설명", setOf(4L, 5L))
        )
        
        `when`(findUseCase.getGroups(UserId.from(ownerId)))
            .thenReturn(groups)

        // when
        val response = controller.getGroups(ownerId)

        // then
        assertThat(response).isNotNull
        assertThat(response.success).isTrue()
        assertThat(response.data).hasSize(2)
        
        // 첫 번째 그룹 검증
        assertThat(response.data?.get(0)?.id).isEqualTo(1L)
        assertThat(response.data?.get(0)?.name).isEqualTo("첫 번째 그룹")
        assertThat(response.data?.get(0)?.memberIds).containsExactlyInAnyOrder(2L, 3L)
        
        // 두 번째 그룹 검증
        assertThat(response.data?.get(1)?.id).isEqualTo(2L)
        assertThat(response.data?.get(1)?.name).isEqualTo("두 번째 그룹")
        assertThat(response.data?.get(1)?.memberIds).containsExactlyInAnyOrder(4L, 5L)

        verify(findUseCase).getGroups(UserId.from(ownerId))
    }

    // 테스트용 FriendGroup 객체 생성 헬퍼 메서드
    private fun createFriendGroup(
        id: Long,
        ownerId: Long,
        name: String,
        description: String?,
        memberIds: Set<Long> = emptySet()
    ): FriendGroup {
        return FriendGroup(
            id = id,
            ownerId = UserId.from(ownerId),
            name = FriendGroupName.from(name),
            description = description,
            memberIds = memberIds.map { UserId.from(it) }.toSet(),
            createdAt = Instant.now()
        )
    }
}