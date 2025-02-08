package com.stark.shoot.infrastructure.config.security

import com.stark.shoot.infrastructure.config.jwt.JwtProvider
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthFilter,
    private val jwtProvider: JwtProvider
) {

    /**
     * principal이 String(= JWT 토큰) 형태라고 가정하고,
     * validateToken()만 통과하면 인증을 허용하는 간단한 AuthenticationManager 예시.
     */
    @Bean
    fun authenticationManager(): AuthenticationManager {
        return AuthenticationManager { authentication ->
            val principal = authentication.principal
            if (principal is String) {
                // 여기서 토큰이 유효한지 다시 한번 확인
                jwtProvider.isTokenValid(principal)
                authentication.isAuthenticated = true
                return@AuthenticationManager authentication
            }
            throw AuthenticationServiceException("Unsupported authentication type")
        }
    }

    /**
     * CORS 설정
     * - 프론트엔드 URL을 허용
     * - 허용할 HTTP 메서드 설정
     * - 모든 헤더를 허용
     * - 자격 증명(쿠키, Authorization 헤더 등)을 허용
     */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            // 프론트엔드 URL을 허용합니다.
            allowedOrigins = listOf("http://localhost:3000")
            // 허용할 HTTP 메서드 설정
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
            // 모든 헤더를 허용합니다.
            allowedHeaders = listOf("*")
            // 자격 증명(쿠키, Authorization 헤더 등)을 허용합니다.
            allowCredentials = true
        }
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    /**
     * JWT 토큰 검증 필터를 SecurityFilterChain에 등록합니다.
     * - CSRF 보안 기능 비활성화
     * - 세션 관리 정책: STATELESS
     * - 인증이 필요한 요청에 대해 JWT 토큰 검증 필터를 추가
     * - 인증 실패 시 401 Unauthorized 응답 반환
     * - WebSocket 연결 엔드포인트는 인증 없이 접근 가능
     * - JWT 토큰 검증 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
     * - 인증 실패 처리: 401 Unauthorized 응답 반환
     */
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                // 인증이 필요하지 않은 경로
                // 로그인, 회원가입 등은 permitAll
                it.requestMatchers("/api/v1/auth/login", "/api/v1/users").permitAll()
                it.requestMatchers("/ws/**").permitAll() // 웹소켓 핸드셰이크
                // 그 외 모든 요청은 인증 필요
                it.anyRequest().authenticated()
            }
            // JwtAuthFilter를 UsernamePasswordAuthenticationFilter보다 앞단에 추가
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            .exceptionHandling { ex ->
                // 인증 실패 시 401 반환
                ex.authenticationEntryPoint { _, response, authException ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.message)
                }
            }
            .build()
    }

}