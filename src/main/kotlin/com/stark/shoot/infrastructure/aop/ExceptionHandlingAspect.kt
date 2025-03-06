package com.stark.shoot.infrastructure.aop

import com.stark.shoot.adapter.`in`.web.dto.ApiException
import com.stark.shoot.adapter.`in`.web.dto.ErrorCode
import com.stark.shoot.infrastructure.exception.web.*
import io.github.oshai.kotlinlogging.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.util.concurrent.ConcurrentHashMap

/**
 * 모든 원시 예외를 ApiException으로 변환하고 로깅하는 Aspect
 *
 * @constructor Create empty Exception handling aspect
 */
@Aspect
@Component
@Order(1) // 우선순위를 1순위로 지정
class ExceptionHandlingAspect {

    private val logger = KotlinLogging.logger {}

    private val errorCountMap = ConcurrentHashMap<String, Int>()

    @Around("@within(org.springframework.web.bind.annotation.RestController) || @within(org.springframework.web.bind.annotation.ControllerAdvice)")
    fun handleControllerExceptions(joinPoint: ProceedingJoinPoint): Any? {
        return try {
            joinPoint.proceed()
        } catch (e: Exception) {
            val signature = joinPoint.signature as MethodSignature
            val methodName = signature.method.name
            val className = signature.declaringType.simpleName
            val endpoint = "$className.$methodName"

            // 에러 발생 빈도 측정
            errorCountMap.compute(endpoint) { _, count -> (count ?: 0) + 1 }

            val paramNames = signature.parameterNames
            val args = joinPoint.args
            val params = paramNames.zip(args)
                .filter { (name, _) -> !name.contains("password", ignoreCase = true) }
                .joinToString(", ") { (name, value) -> "$name: ${value?.toString()?.take(100)}" }

            logger.error { "Exception in $endpoint[$params]: ${e.message}" }

            // 예외 처리 로직
            when (e) {
                // 입력 데이터 관련 예외
                is MethodArgumentNotValidException -> {
                    val errors = e.bindingResult.fieldErrors.joinToString("; ") {
                        "${it.field}: ${it.defaultMessage}"
                    }
                    throw ApiException("유효하지 않은 입력: $errors", ErrorCode.INVALID_INPUT, e)
                }

                is MethodArgumentTypeMismatchException ->
                    throw ApiException("잘못된 형식의 파라미터: ${e.name} = ${e.value}", ErrorCode.INVALID_INPUT, e)

                // 비즈니스 로직 예외
                is IllegalStateException -> {
                    when {
                        e.message?.contains("최대 핀") == true ->
                            throw ApiException(e.message!!, ErrorCode.TOO_MANY_PINNED_ROOMS, e)

                        e.message?.contains("친구") == true && e.message?.contains("이미") == true ->
                            throw ApiException(e.message!!, ErrorCode.ALREADY_FRIENDS, e)

                        else ->
                            throw ApiException(e.message ?: "상태 오류", ErrorCode.INVALID_INPUT, e)
                    }
                }

                // 리소스 관련 예외
                is ResourceNotFoundException ->
                    throw ApiException(e.message ?: "리소스를 찾을 수 없습니다", ErrorCode.RESOURCE_NOT_FOUND, e)

                // 입력 검증 예외
                is InvalidInputException ->
                    throw ApiException(e.message ?: "유효하지 않은 입력", ErrorCode.INVALID_INPUT, e)

                // 인증 관련 예외
                is UnauthorizedException ->
                    throw ApiException(e.message ?: "인증이 필요합니다", ErrorCode.UNAUTHORIZED, e)

                is JwtAuthenticationException ->
                    throw ApiException(e.message ?: "토큰이 유효하지 않습니다", ErrorCode.INVALID_TOKEN, e)

                is InvalidRefreshTokenException ->
                    throw ApiException(e.message ?: "리프레시 토큰이 유효하지 않습니다", ErrorCode.TOKEN_EXPIRED, e)

                // 웹소켓 예외
                is WebSocketException ->
                    throw ApiException(e.message ?: "웹소켓 오류", ErrorCode.INVALID_INPUT, e)

                // Kafka 관련 예외
                is KafkaPublishException ->
                    throw ApiException(e.message ?: "메시지 발행 실패", ErrorCode.EXTERNAL_SERVICE_ERROR, e)

                // Spring Security 예외
                is AccessDeniedException ->
                    throw ApiException("접근 권한이 없습니다", ErrorCode.ACCESS_DENIED, e)

                // 기타 예외
                else -> {
                    // 예외 유형 로깅 (추후 분석용)
                    logger.error { "Unhandled exception type: ${e.javaClass.name} in $endpoint" }

                    throw ApiException(
                        "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                        ErrorCode.EXTERNAL_SERVICE_ERROR,
                        e
                    )
                }
            }
        }
    }

    // 에러 통계 제공 메서드 (필요시 API로 노출하거나 모니터링 시스템과 통합)
    fun getErrorStatistics(): Map<String, Int> = errorCountMap.toMap()

}