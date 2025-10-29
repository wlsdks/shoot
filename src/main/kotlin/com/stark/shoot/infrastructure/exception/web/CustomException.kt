package com.stark.shoot.domain.exception.web

class UnauthorizedException(message: String) : RuntimeException(message)
class WebSocketException(message: String) : RuntimeException(message)
class InvalidInputException(message: String) : RuntimeException(message)
class ResourceNotFoundException(message: String) : RuntimeException(message)
class JwtAuthenticationException(message: String) : RuntimeException(message)
class KafkaPublishException(message: String, cause: Throwable) : RuntimeException(message, cause)
class KafkaConsumerException(message: String, cause: Throwable) : RuntimeException(message, cause)
class InvalidRefreshTokenException(message: String) : RuntimeException(message)
class LockAcquisitionException(message: String) : RuntimeException(message)
class RedisOperationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class MongoOperationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class NotificationProcessingException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
