package com.stark.shoot.domain.shared.exception

/**
 * 즐겨찾기(핀) 개수 제한을 초과했을 때 발생하는 예외
 */
class FavoriteLimitExceededException(
    message: String = "최대 핀 채팅방 개수를 초과했습니다.",
    errorCode: String = "FAVORITE_LIMIT_EXCEEDED"
) : DomainException(message, errorCode)