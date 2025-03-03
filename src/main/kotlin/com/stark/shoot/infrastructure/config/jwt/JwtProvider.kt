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
     * 토큰 생성
     *
     * @param id     사용자 ID
     * @param username 사용자명
     * @param expiresInMillis 만료 시간 (밀리초)
     * @return JWT 토큰
     */
    fun generateToken(
        id: String, // id를 sub로 사용
        username: String, // username을 별도 클레임으로 추가
        expiresInMillis: Long = 3600_000 // 1시간 기본값
    ): String {
        val now = Date()
        val expiryDate = Date(now.time + expiresInMillis)

        return Jwts.builder()
            .subject(id) // sub에 id 설정
            .claim("username", username) // username 추가
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key, Jwts.SIG.HS256) // 알고리즘 명시 (HS256)
            .compact()
    }

    /**
     * 리프레시 토큰 생성
     *
     * @param id          사용자 ID
     * @param username   사용자명
     * @param expiresInMinutes 만료 시간 (분)
     * @return JWT 리프레시 토큰
     */
    fun generateRefreshToken(
        id: String,
        username: String,
        expiresInMinutes: Long
    ): String {
        val now = Date()
        val expiryDate = Date(now.time + (expiresInMinutes * 60 * 1000)) // 분 단위 설정

        return Jwts.builder()
            .subject(id) // sub에 id 설정
            .claim("username", username) // username 추가
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key, Jwts.SIG.HS256) // 알고리즘 명시
            .compact()
    }

    // 토큰에서 사용자 이름 추출 (username 클레임)
    fun extractUsername(token: String?): String {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
            .get("username", String::class.java)
    }

    // 토큰에서 ID 추출 (sub)
    fun extractId(token: String?): String {
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