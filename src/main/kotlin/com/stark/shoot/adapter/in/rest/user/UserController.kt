package com.stark.shoot.adapter.`in`.rest.user

import com.stark.shoot.adapter.`in`.rest.dto.ResponseDto
import com.stark.shoot.adapter.`in`.rest.dto.user.CreateUserRequest
import com.stark.shoot.adapter.`in`.rest.dto.user.UserResponse
import com.stark.shoot.adapter.`in`.rest.dto.user.toResponse
import com.stark.shoot.application.port.`in`.user.UserCreateUseCase
import com.stark.shoot.application.port.`in`.user.UserDeleteUseCase
import com.stark.shoot.application.port.`in`.user.command.CreateUserCommand
import com.stark.shoot.application.port.`in`.user.command.DeleteUserCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@Tag(name = "사용자", description = "사용자 관련 API")
@RequestMapping("/api/v1/users")
@RestController
class UserController(
    private val userCreateUseCase: UserCreateUseCase,
    private val userDeleteUseCase: UserDeleteUseCase
) {

    @Operation(
        summary = "사용자 생성 (회원가입)",
        description = "새로운 사용자를 생성합니다."
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createUser(@RequestBody request: CreateUserRequest): ResponseDto<UserResponse> {
        val command = CreateUserCommand.of(request)
        val user = userCreateUseCase.createUser(command)
        return ResponseDto.success(user.toResponse(), "회원가입이 완료되었습니다.")
    }

    @Operation(
        summary = "회원 탈퇴",
        description = """
           - 현재 사용자를 탈퇴 처리합니다.
             - Authentication 객체를 매개변수로 받는 이유는, 이 API가 현재 로그인한 사용자의 정보를 기반으로 동작해야 하기 때문입니다. 
             - 여기서 Authentication은 Spring Security가 제공하는 인터페이스로, 인증된 사용자의 세부 정보(예: 사용자 ID, 권한 등)를 담고 있습니다.
        """
    )
    @DeleteMapping("/me")
    fun deleteUser(
        authentication: Authentication
    ): ResponseDto<Unit> {
        val userId = authentication.name.toLong()
        val command = DeleteUserCommand.of(userId)
        userDeleteUseCase.deleteUser(command)
        return ResponseDto.success(Unit, "회원 탈퇴가 완료되었습니다.")
    }

}
