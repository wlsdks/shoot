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

    // 토큰 생성
    fun generateToken(
        subject: String,
        expiresInMillis: Long = 3600_000 // 1시간 기본값
    ): String {
        val now = Date()
        val expiryDate = Date(now.time + expiresInMillis)

        return Jwts.builder()
            .subject(subject)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key) // todo: 여기에 알고리즘 설정 추가해야하는거 아닌지 확인이 필요합니다.
            .compact()
    }

    // 리프레시 토큰 생성 (access 토큰보다 유효기간이 길어야 함)
    fun generateRefreshToken(
        subject: String?,
        expiresInMinutes: Long
    ): String {
        val now = Date()
        val expiryDate = Date(now.time + (expiresInMinutes * 60 * 1000)) // 한달 기본값
        return Jwts.builder()
            .subject(subject)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
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