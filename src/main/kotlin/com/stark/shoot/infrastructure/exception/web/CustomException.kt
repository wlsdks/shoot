package com.stark.shoot.infrastructure.exception.web

class UnauthorizedException(message: String) : RuntimeException(message)
class WebSocketException(message: String) : RuntimeException(message)
class InvalidInputException(message: String) : RuntimeException(message)
class ResourceNotFoundException(message: String) : RuntimeException(message)
class JwtAuthenticationException(message: String) : RuntimeException(message)
class KafkaPublishException(message: String, cause: Throwable) : RuntimeException(message, cause)
class InvalidRefreshTokenException(message: String) : RuntimeException(message)
class LockAcquisitionException(message: String) : RuntimeException(message)