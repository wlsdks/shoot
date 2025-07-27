package com.stark.shoot.adapter.`in`.rest.social.group

import com.stark.shoot.adapter.`in`.rest.dto.ResponseDto
import com.stark.shoot.adapter.`in`.rest.dto.social.group.AddMemberInGroupRequest
import com.stark.shoot.adapter.`in`.rest.dto.social.group.CreateGroupRequest
import com.stark.shoot.adapter.`in`.rest.dto.user.FriendGroupResponse
import com.stark.shoot.adapter.`in`.rest.dto.user.toResponse
import com.stark.shoot.application.port.`in`.user.group.FindFriendGroupUseCase
import com.stark.shoot.application.port.`in`.user.group.ManageFriendGroupUseCase
import com.stark.shoot.application.port.`in`.user.group.command.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "친구그룹", description = "친구 그룹 관리 API")
@RestController
@RequestMapping("/api/v1/friends/groups")
class FriendGroupController(
    private val manageUseCase: ManageFriendGroupUseCase,
    private val findUseCase: FindFriendGroupUseCase,
) {

    @Operation(summary = "그룹 생성")
    @PostMapping
    fun createGroup(@RequestBody request: CreateGroupRequest): ResponseDto<FriendGroupResponse> {
        val command = CreateGroupCommand.of(request)
        val group = manageUseCase.createGroup(command)
        return ResponseDto.success(group.toResponse())
    }

    @Operation(summary = "그룹 이름 변경")
    @PutMapping("/{groupId}/name")
    fun renameGroup(
        @PathVariable groupId: Long,
        @RequestParam name: String
    ): ResponseDto<FriendGroupResponse> {
        val command = RenameGroupCommand.of(groupId, name)
        val group = manageUseCase.renameGroup(command)
        return ResponseDto.success(group.toResponse())
    }

    @Operation(summary = "그룹 설명 수정")
    @PutMapping("/{groupId}/description")
    fun updateDescription(
        @PathVariable groupId: Long,
        @RequestParam description: String?
    ): ResponseDto<FriendGroupResponse> {
        val command = UpdateDescriptionCommand.of(groupId, description)
        val group = manageUseCase.updateDescription(command)
        return ResponseDto.success(group.toResponse())
    }

    @Operation(summary = "멤버 추가")
    @PostMapping("/{groupId}/members/{memberId}")
    fun addMember(@RequestBody request: AddMemberInGroupRequest): ResponseDto<FriendGroupResponse> {
        val command = AddMemberCommand.of(request)
        val group = manageUseCase.addMember(command)
        return ResponseDto.success(group.toResponse())
    }

    @Operation(summary = "멤버 제거")
    @DeleteMapping("/{groupId}/members/{memberId}")
    fun removeMember(
        @PathVariable groupId: Long,
        @PathVariable memberId: Long
    ): ResponseDto<FriendGroupResponse> {
        val command = RemoveMemberCommand.of(groupId, memberId)
        val group = manageUseCase.removeMember(command)
        return ResponseDto.success(group.toResponse())
    }

    @Operation(summary = "그룹 삭제")
    @DeleteMapping("/{groupId}")
    fun deleteGroup(@PathVariable groupId: Long): ResponseDto<Unit> {
        val command = DeleteGroupCommand.of(groupId)
        manageUseCase.deleteGroup(command)
        return ResponseDto.success(Unit)
    }

    @Operation(summary = "그룹 단건 조회")
    @GetMapping("/{groupId}")
    fun getGroup(@PathVariable groupId: Long): ResponseDto<FriendGroupResponse?> {
        val command = GetGroupCommand.of(groupId)
        val group = findUseCase.getGroup(command)?.toResponse()
        return ResponseDto.success(group)
    }

    @Operation(summary = "내 그룹 목록")
    @GetMapping
    fun getGroups(@RequestParam ownerId: Long): ResponseDto<List<FriendGroupResponse>> {
        val command = GetGroupsCommand.of(ownerId)
        val groups = findUseCase.getGroups(command)
            .map { it.toResponse() }
        return ResponseDto.success(groups)
    }

}
