package com.stark.shoot.application.service.user

import com.stark.shoot.adapter.`in`.web.dto.user.LoginResponse
import com.stark.shoot.application.port.`in`.user.UserLoginUseCase
import com.stark.shoot.application.port.out.user.RetrieveUserPort
import com.stark.shoot.infrastructure.config.jwt.JwtProvider
import org.springframework.stereotype.Service

@Service
class UserLoginService(
    private val retrieveUserPort: RetrieveUserPort,
    private val jwtProvider: JwtProvider
) : UserLoginUseCase {

    /**
     * username, password를 받아서 로그인 처리합니다.
     * - DB에서 사용자 조회
     * - 비밀번호 검증 (여기서는 예시로 'password' 필드가 있다 가정)
     * - JWT 생성 후 반환
     */
    override fun login(
        username: String,
        password: String
    ): LoginResponse {
        // 포트에서 null이 반환될 수 있음
        val user = retrieveUserPort.findByUsername(username)
            ?: throw IllegalArgumentException("해당 username의 사용자를 찾을 수 없습니다.")

        // 비밀번호 검증
//        if (user.password != password) {
//            throw IllegalArgumentException("비밀번호가 올바르지 않습니다.")
//        }

        // JWT 생성
        val generatedAccessToken = jwtProvider.generateToken(subject = user.username)

        // 사용자 ID와 생성된 토큰 반환 (ObjectId를 String으로 변환해서 반환)
        return LoginResponse(
            userId = user.id.toString(),
            accessToken = generatedAccessToken
        )
    }

}