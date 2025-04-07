package com.stark.shoot.infrastructure.config.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtProvider(
    @Value("\${jwt.secret}")
    private val secret: String,

    @Value("\${jwt.expiration:3600}") // 기본값 1시간(3600초)
    private val expiration: Long,

    @Value("\${jwt.refresh-token.expiration:43200}") // 기본값 30일(43200분)
    private val refreshExpiration: Long
) {

    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    /**
     * 액세스 토큰 생성
     *
     * @param userId   사용자 ID
     * @param username 사용자명
     * @param expiresInMillis 만료 시간 (밀리초)
     * @return JWT 토큰
     */
    fun generateToken(
        userId: String,
        username: String,
    ): String {
        val now = System.currentTimeMillis()
        val expirationTime = now + expiration * 60 * 1000 // 분 단위를 밀리초로 변환

        return Jwts.builder()
            .subject(userId) // sub에 id 설정
            .claim("username", username) // username 추가
            .issuedAt(Date(now))
            .expiration(Date(expirationTime))
            .signWith(secretKey, Jwts.SIG.HS256) // 알고리즘 명시 (HS256)
            .compact()
    }

    /**
     * 리프레시 토큰 생성
     *
     * @param userId     사용자 ID
     * @param username   사용자명
     * @param expiresInMinutes 만료 시간 (분)
     * @return JWT 리프레시 토큰
     */
    fun generateRefreshToken(
        userId: String,
        username: String,
        expirationMinutes: Long = refreshExpiration
    ): String {
        val now = System.currentTimeMillis()
        val expirationTime = now + expirationMinutes * 60 * 1000 // 분 단위를 밀리초로 변환

        return Jwts.builder()
            .subject(userId) // sub에 id 설정
            .claim("username", username) // username 추가
            .claim("tokenType", "refresh") // 리프레시 토큰임을 명시
            .issuedAt(Date(now))
            .expiration(Date(expirationTime))
            .signWith(secretKey, Jwts.SIG.HS256) // 알고리즘 명시
            .compact()
    }

    /**
     * 토큰에서 ID 추출
     */
    fun extractId(token: String): String {
        return extractClaim(token) { claims ->
            claims.subject
        }
    }

    /**
     * 토큰에서 사용자명 추출
     */
    fun extractUsername(token: String): String {
        return extractClaim(token) { claims ->
            claims.get("username", String::class.java)
        }
    }

    /**
     * 특정 클레임 추출
     */
    private fun <T> extractClaim(
        token: String,
        claimsResolver: (Claims) -> T
    ): T {
        val claims = extractAllClaims(token)
        return claimsResolver(claims)
    }

    /**
     * 모든 클레임 추출
     */
    private fun extractAllClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    /**
     * 토큰 만료 여부 확인
     */
    private fun isTokenExpired(token: String): Boolean {
        try {
            val expiration = extractClaim(token) { claims -> claims.expiration }
            return expiration.before(Date())
        } catch (e: ExpiredJwtException) {
            return true
        }
    }

    /**
     * 액세스 토큰 유효성 검증
     */
    fun isTokenValid(token: String): Boolean {
        try {
            val claims = extractAllClaims(token)
            return !isTokenExpired(token) && !claims.containsKey("tokenType")
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * 리프레시 토큰 유효성 검증
     *
     * 리프레시 토큰은 다음 조건을 만족해야 함:
     * 1. 만료되지 않음
     * 2. 올바른 서명을 포함
     * 3. "tokenType" 클레임이 "refresh"인지 확인
     */
    fun isRefreshTokenValid(token: String): Boolean {
        try {
            val claims = extractAllClaims(token)
            val tokenType = claims.get("tokenType", String::class.java)

            return !isTokenExpired(token) && tokenType == "refresh"
        } catch (e: Exception) {
            return false
        }
    }

}