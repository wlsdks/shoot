package com.stark.shoot.adapter.`in`.rest.social.search

import com.stark.shoot.adapter.`in`.rest.dto.ResponseDto
import com.stark.shoot.adapter.`in`.rest.dto.user.FriendResponse
import com.stark.shoot.application.port.`in`.user.friend.FriendSearchUseCase
import com.stark.shoot.application.port.`in`.user.friend.command.SearchFriendsCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "친구 검색", description = "친구 검색 API")
@RestController
@RequestMapping("/api/v1/friends")
class FriendSearchController(
    private val friendSearchUseCase: FriendSearchUseCase
) {

    @Operation(
        summary = "친구 검색",
        description = "로그인 사용자의 친구 목록 중 검색어와 일치하는 친구들을 반환합니다."
    )
    @GetMapping("/search")
    fun searchFriends(
        @RequestParam userId: Long,
        @RequestParam query: String
    ): ResponseDto<List<FriendResponse>> {
        val command = SearchFriendsCommand.of(userId, query)
        val friends = friendSearchUseCase.searchPotentialFriends(command)
        return ResponseDto.success(friends)
    }

}
