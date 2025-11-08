package com.stark.shoot.infrastructure.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * OpenAPI/Swagger UI Configuration for MSA API Documentation
 *
 * 각 마이크로서비스별로 그룹화된 API 문서를 제공합니다.
 */
@Configuration
class OpenApiConfig {

    /**
     * 전체 애플리케이션 OpenAPI 설정
     */
    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Shoot API")
                    .version("1.0.0")
                    .description(
                        """
                        실시간 채팅 애플리케이션 Shoot의 API 문서입니다.

                        ## 개요
                        - MSA 마이그레이션을 위한 서비스별 API 정의
                        - Event-Driven Architecture 기반 설계
                        - JWT 인증 사용

                        ## 서비스
                        - **User Service**: 사용자 인증 및 프로필 관리
                        - **Friend Service**: 친구 관계 및 친구 추천
                        - **Chat Service**: 채팅방, 메시지, 리액션
                        - **Notification Service**: 실시간 알림 관리

                        ## 인증
                        모든 API는 JWT Bearer Token 인증을 사용합니다. (회원가입/로그인 제외)

                        Authorization 헤더에 `Bearer <token>` 형식으로 토큰을 전달하세요.
                        """.trimIndent()
                    )
                    .contact(
                        Contact()
                            .name("Shoot Team")
                            .email("support@shoot.com")
                    )
                    .license(
                        License()
                            .name("MIT License")
                            .url("https://opensource.org/licenses/MIT")
                    )
            )
            .servers(
                listOf(
                    Server()
                        .url("http://localhost:8080")
                        .description("Local Development Server"),
                    Server()
                        .url("https://api.shoot.com")
                        .description("Production Server")
                )
            )
            .components(
                Components()
                    .addSecuritySchemes(
                        "bearerAuth",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("JWT 토큰을 입력하세요. 'Bearer ' 접두사는 자동으로 추가됩니다.")
                    )
            )
            .addSecurityItem(
                SecurityRequirement().addList("bearerAuth")
            )
    }

    /**
     * User Service API 그룹
     */
    @Bean
    fun userServiceApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("1. User Service")
            .pathsToMatch("/api/v1/auth/**", "/api/v1/users/**")
            .addOpenApiCustomizer { openApi ->
                openApi.info(
                    Info()
                        .title("User Service API")
                        .version("1.0.0")
                        .description(
                            """
                            사용자 인증 및 프로필 관리 서비스

                            ## 주요 기능
                            - 회원가입 / 로그인 / 로그아웃
                            - JWT 토큰 발급 및 갱신
                            - 사용자 프로필 조회 및 수정
                            - 사용자 검색 (닉네임, UserCode)
                            - 계정 삭제

                            ## 엔드포인트 (10개)
                            - 인증: 4개
                            - 사용자 관리: 6개
                            """.trimIndent()
                        )
                )
            }
            .build()
    }

    /**
     * Friend Service API 그룹
     */
    @Bean
    fun friendServiceApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("2. Friend Service")
            .pathsToMatch("/api/v1/friends/**")
            .addOpenApiCustomizer { openApi ->
                openApi.info(
                    Info()
                        .title("Friend Service API")
                        .version("1.0.0")
                        .description(
                            """
                            친구 관계 및 친구 추천 서비스

                            ## 주요 기능
                            - 친구 목록 조회 및 관리
                            - 친구 요청 전송 / 수락 / 거절
                            - 공통 친구 조회
                            - BFS 알고리즘 기반 친구 추천

                            ## 엔드포인트 (15개)
                            - 친구 관계: 6개
                            - 친구 요청: 7개
                            - 친구 추천: 2개

                            ## 비즈니스 규칙
                            - 최대 친구 수: 1,000명
                            - 추천 최대 depth: 3 (BFS)
                            - 양방향 친구 관계
                            """.trimIndent()
                        )
                )
            }
            .build()
    }

    /**
     * Chat Service API 그룹
     */
    @Bean
    fun chatServiceApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("3. Chat Service")
            .pathsToMatch("/api/v1/chatrooms/**")
            .addOpenApiCustomizer { openApi ->
                openApi.info(
                    Info()
                        .title("Chat Service API")
                        .version("1.0.0")
                        .description(
                            """
                            채팅방 관리 및 메시지 서비스

                            ## 주요 기능
                            - 채팅방 생성 / 조회 / 삭제
                            - 참여자 관리 (추가/제거)
                            - 메시지 전송 / 수정 / 삭제
                            - 리액션 (LIKE, LOVE, HAHA, WOW, SAD, ANGRY)
                            - 스케줄 메시지
                            - 메시지 고정 (최대 5개)
                            - 메시지 전달 및 읽음 처리

                            ## 엔드포인트 (30개)
                            - 채팅방 관리: 12개
                            - 메시지 관리: 5개
                            - 리액션: 2개
                            - 스케줄 메시지: 5개
                            - 메시지 고정: 3개
                            - 메시지 전달 & 읽음: 3개

                            ## 비즈니스 규칙
                            - 1:1 채팅: 정확히 2명
                            - 그룹 채팅: 2-100명
                            - 메시지 최대 길이: 4,000자
                            - 메시지 수정 제한: 생성 후 24시간
                            - 첨부파일 크기: 최대 50MB
                            """.trimIndent()
                        )
                )
            }
            .build()
    }

    /**
     * Notification Service API 그룹
     */
    @Bean
    fun notificationServiceApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("4. Notification Service")
            .pathsToMatch("/api/v1/notifications/**")
            .addOpenApiCustomizer { openApi ->
                openApi.info(
                    Info()
                        .title("Notification Service API")
                        .version("1.0.0")
                        .description(
                            """
                            알림 생성 및 관리 서비스

                            ## 주요 기능
                            - 알림 목록 조회 및 상세 조회
                            - 읽음 처리 (개별/전체)
                            - 알림 삭제 (개별/전체)
                            - 읽지 않은 알림 개수 조회
                            - 알림 설정 관리 (알림음, 진동 등)

                            ## 엔드포인트 (11개)
                            - 알림 관리: 7개
                            - 알림 설정: 4개

                            ## 알림 타입
                            - MESSAGE: 새 메시지
                            - FRIEND_REQUEST: 친구 요청
                            - MENTION: 멘션
                            - REACTION: 리액션
                            - CHATROOM_INVITE: 채팅방 초대
                            - SYSTEM: 시스템 알림
                            """.trimIndent()
                        )
                )
            }
            .build()
    }

    /**
     * All APIs 그룹 (전체 엔드포인트)
     */
    @Bean
    fun allApis(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("5. All APIs")
            .pathsToMatch("/api/**")
            .build()
    }

    /**
     * Internal APIs 그룹 (헬스체크, 모니터링 등)
     */
    @Bean
    fun internalApis(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("6. Internal APIs")
            .pathsToMatch("/actuator/**", "/health/**", "/metrics/**")
            .build()
    }
}
