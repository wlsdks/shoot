package com.stark.shoot.adapter.`in`.web.social.search

import com.stark.shoot.adapter.`in`.web.dto.user.FriendResponse
import com.stark.shoot.application.port.`in`.user.friend.UserFriendUseCase
import com.stark.shoot.infrastructure.util.toObjectId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "친구 검색", description = "친구 검색 API")
@RestController
@RequestMapping("/api/v1/friends")
class UserSearchController(
    private val userFriendUseCase: UserFriendUseCase
) {

    @Operation(summary = "친구 검색", description = "로그인 사용자의 친구 목록 중 검색어와 일치하는 친구들을 반환합니다.")
    @GetMapping("/search")
    fun searchFriends(
        @RequestParam userId: String,
        @RequestParam query: String
    ): ResponseEntity<List<FriendResponse>> {
        // 예시: friendUseCase 내에 검색 기능을 구현했다고 가정
        val friends = userFriendUseCase.searchPotentialFriends(userId.toObjectId(), query)
        return ResponseEntity.ok(friends)
    }

}