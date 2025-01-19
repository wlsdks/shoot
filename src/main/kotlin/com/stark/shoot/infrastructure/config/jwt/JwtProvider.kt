package com.stark.shoot.infrastructure.config.jwt

import com.stark.shoot.infrastructure.common.exception.UnauthorizedException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*

@Component
class JwtProvider(
    @Value("\${jwt.secret}") private val secretKey: String
) {

    private val key = Keys.hmacShaKeyFor(secretKey.toByteArray())

    /**
     * subject(예: userId 또는 username)를 기반으로 JWT를 생성합니다.
     * 만료 시간(expiresInMillis)은 예시로 1시간(3600_000) 등 원하는 대로 설정하세요.
     */
    fun generateToken(subject: String, expiresInMillis: Long = 3600_000): String {
        val now = Date()
        val expiryDate = Date(now.time + expiresInMillis)

        return Jwts.builder()
            .subject(subject)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key) // todo: 여기에 알고리즘 설정 추가해야하는거 아닌지 확인이 필요합니다.
            .compact()
    }

    /**
     * JWT 토큰 문자열을 검증하고, 내부에 담긴 subject(예: userId)를 반환합니다.
     * 유효하지 않은 경우 UnauthorizedException 발생.
     */
    fun validateToken(token: String): String {
        return try {
            val claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseUnsecuredClaims(token)
                .payload

            claims.subject
        } catch (e: Exception) {
            throw UnauthorizedException("Invalid JWT token")
        }
    }

}