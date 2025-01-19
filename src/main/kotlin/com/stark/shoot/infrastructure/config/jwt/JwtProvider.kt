package com.stark.shoot.infrastructure.config.jwt

import io.jsonwebtoken.JwtException
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

    // 토큰에서 사용자 이름 추출
    fun extractUsername(token: String?): String {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
            .subject
    }

    // 토큰 유효성 검사
    fun isTokenValid(token: String?): Boolean {
        try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
            return true
        } catch (e: JwtException) {
            return false
        } catch (e: IllegalArgumentException) {
            return false
        }
    }

}