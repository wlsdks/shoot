package com.stark.shoot.infrastructure.aop

import com.stark.shoot.adapter.`in`.web.dto.ApiException
import com.stark.shoot.adapter.`in`.web.dto.ErrorCode
import com.stark.shoot.infrastructure.exception.web.*
import io.github.oshai.kotlinlogging.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

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

    @Around("@within(org.springframework.web.bind.annotation.RestController) || @within(org.springframework.web.bind.annotation.ControllerAdvice)")
    fun handleControllerExceptions(joinPoint: ProceedingJoinPoint): Any? {
        return try {
            joinPoint.proceed()
        } catch (e: Exception) {
            // 이미 ApiException 타입이면 그대로 전파
            if (e is ApiException) {
                throw e
            }

            // 로깅 - 중요한 예외만 ERROR 레벨로 로깅
            when (e) {
                is ResourceNotFoundException, is InvalidInputException ->
                    logger.info { "Expected exception in ${joinPoint.signature.declaringTypeName}.${joinPoint.signature.name}: ${e.message}" }

                else ->
                    logger.error(e) { "Unexpected exception in ${joinPoint.signature.declaringTypeName}.${joinPoint.signature.name}" }
            }

            // 예외 변환 - 맵을 사용하여 when 문 단순화
            val apiException = when (e) {
                is MethodArgumentNotValidException -> {
                    val errors = e.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
                    ApiException("유효하지 않은 입력: $errors", ErrorCode.INVALID_INPUT, e)
                }

                is MethodArgumentTypeMismatchException ->
                    ApiException("잘못된 형식의 파라미터: ${e.name} = ${e.value}", ErrorCode.INVALID_INPUT, e)

                is ResourceNotFoundException ->
                    ApiException(e.message ?: "리소스를 찾을 수 없습니다", ErrorCode.RESOURCE_NOT_FOUND, e)

                is InvalidInputException ->
                    ApiException(e.message ?: "유효하지 않은 입력", ErrorCode.INVALID_INPUT, e)

                is UnauthorizedException ->
                    ApiException(e.message ?: "인증이 필요합니다", ErrorCode.UNAUTHORIZED, e)

                is JwtAuthenticationException ->
                    ApiException(e.message ?: "토큰이 유효하지 않습니다", ErrorCode.INVALID_TOKEN, e)

                is InvalidRefreshTokenException ->
                    ApiException(e.message ?: "리프레시 토큰이 유효하지 않습니다", ErrorCode.TOKEN_EXPIRED, e)

                is WebSocketException ->
                    ApiException(e.message ?: "웹소켓 오류", ErrorCode.INVALID_INPUT, e)

                is KafkaPublishException ->
                    ApiException(e.message ?: "메시지 발행 실패", ErrorCode.EXTERNAL_SERVICE_ERROR, e)

                is AccessDeniedException ->
                    ApiException("접근 권한이 없습니다", ErrorCode.ACCESS_DENIED, e)

                is LockAcquisitionException ->
                    ApiException(e.message ?: "리소스 잠금 실패", ErrorCode.LOCK_ACQUIRE_FAILED, e)

                else -> ApiException(
                    "${e.message}",
                    ErrorCode.EXTERNAL_SERVICE_ERROR,
                    e
                )
            }

            throw apiException
        }
    }

}