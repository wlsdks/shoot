package com.stark.shoot.adapter.`in`.web.social.group

import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.dto.user.FriendGroupResponse
import com.stark.shoot.adapter.`in`.web.dto.user.toResponse
import com.stark.shoot.application.port.`in`.user.group.FindFriendGroupUseCase
import com.stark.shoot.application.port.`in`.user.group.ManageFriendGroupUseCase
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
    fun createGroup(
        @RequestParam ownerId: Long,
        @RequestParam name: String,
        @RequestParam(required = false) description: String?
    ): ResponseDto<FriendGroupResponse> {
        val group = manageUseCase.createGroup(ownerId, name, description)
        return ResponseDto.success(group.toResponse())
    }

    @Operation(summary = "그룹 이름 변경")
    @PutMapping("/{groupId}/name")
    fun renameGroup(
        @PathVariable groupId: Long,
        @RequestParam name: String
    ): ResponseDto<FriendGroupResponse> {
        val group = manageUseCase.renameGroup(groupId, name)
        return ResponseDto.success(group.toResponse())
    }

    @Operation(summary = "그룹 설명 수정")
    @PutMapping("/{groupId}/description")
    fun updateDescription(
        @PathVariable groupId: Long,
        @RequestParam description: String?
    ): ResponseDto<FriendGroupResponse> {
        val group = manageUseCase.updateDescription(groupId, description)
        return ResponseDto.success(group.toResponse())
    }

    @Operation(summary = "멤버 추가")
    @PostMapping("/{groupId}/members/{memberId}")
    fun addMember(
        @PathVariable groupId: Long,
        @PathVariable memberId: Long
    ): ResponseDto<FriendGroupResponse> {
        val group = manageUseCase.addMember(groupId, memberId)
        return ResponseDto.success(group.toResponse())
    }

    @Operation(summary = "멤버 제거")
    @DeleteMapping("/{groupId}/members/{memberId}")
    fun removeMember(
        @PathVariable groupId: Long,
        @PathVariable memberId: Long
    ): ResponseDto<FriendGroupResponse> {
        val group = manageUseCase.removeMember(groupId, memberId)
        return ResponseDto.success(group.toResponse())
    }

    @Operation(summary = "그룹 삭제")
    @DeleteMapping("/{groupId}")
    fun deleteGroup(@PathVariable groupId: Long): ResponseDto<Unit> {
        manageUseCase.deleteGroup(groupId)
        return ResponseDto.success(Unit)
    }

    @Operation(summary = "그룹 단건 조회")
    @GetMapping("/{groupId}")
    fun getGroup(@PathVariable groupId: Long): ResponseDto<FriendGroupResponse?> {
        val group = findUseCase.getGroup(groupId)?.toResponse()
        return ResponseDto.success(group)
    }

    @Operation(summary = "내 그룹 목록")
    @GetMapping
    fun getGroups(@RequestParam ownerId: Long): ResponseDto<List<FriendGroupResponse>> {
        val groups = findUseCase.getGroups(ownerId).map { it.toResponse() }
        return ResponseDto.success(groups)
    }
}
