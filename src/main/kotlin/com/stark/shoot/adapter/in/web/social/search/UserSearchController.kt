package com.stark.shoot.adapter.`in`.web.social.search

import com.stark.shoot.adapter.`in`.web.dto.ApiException
import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.dto.user.FriendResponse
import com.stark.shoot.application.port.`in`.user.friend.FriendUseCase
import com.stark.shoot.infrastructure.util.toObjectId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "친구 검색", description = "친구 검색 API")
@RestController
@RequestMapping("/api/v1/friends")
class UserSearchController(
    private val friendUseCase: FriendUseCase
) {

    @Operation(summary = "친구 검색", description = "로그인 사용자의 친구 목록 중 검색어와 일치하는 친구들을 반환합니다.")
    @GetMapping("/search")
    fun searchFriends(
        @RequestParam userId: String,
        @RequestParam query: String
    ): ResponseDto<List<FriendResponse>> {
        return try {
            val friends = friendUseCase.searchPotentialFriends(userId.toObjectId(), query)
            ResponseDto.success(friends)
        } catch (e: Exception) {
            throw ApiException(
                "친구 검색에 실패했습니다: ${e.message}",
                ApiException.INVALID_INPUT,
                HttpStatus.BAD_REQUEST,
                e
            )
        }
    }

}