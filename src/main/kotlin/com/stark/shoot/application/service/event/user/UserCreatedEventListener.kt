package com.stark.shoot.application.service.event.user

import com.stark.shoot.domain.event.UserCreatedEvent
import com.stark.shoot.infrastructure.annotation.ApplicationEventListener
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 사용자 생성 이벤트를 수신하여 후속 작업을 처리하는 리스너 클래스입니다.
 *
 * 이 클래스는 사용자 생성 이벤트를 수신하여 다음과 같은 작업을 수행합니다:
 * 1. 웰컴 이메일 전송 (향후 구현)
 * 2. 초기 설정 생성 (향후 구현)
 * 3. 분석 이벤트 전송 (향후 구현)
 * 4. 외부 시스템 연동 (향후 구현)
 */
@ApplicationEventListener
class UserCreatedEventListener {

    private val logger = KotlinLogging.logger {}

    /**
     * 사용자 생성 이벤트를 수신하여 후속 작업을 시작하는 메서드입니다.
     * 트랜잭션 커밋 후 실행되어 데이터 일관성을 보장합니다.
     *
     * @param event 처리할 사용자 생성 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleUserCreated(event: UserCreatedEvent) {
        try {
            logger.info {
                "New user created: userId=${event.userId.value}, " +
                "username=${event.username}, nickname=${event.nickname}"
            }

            // TODO: 향후 구현할 기능들
            // 1. sendWelcomeEmail(event)
            // 2. initializeUserSettings(event)
            // 3. sendAnalyticsEvent(event)
            // 4. notifyExternalSystems(event)

        } catch (e: Exception) {
            logger.error(e) { "사용자 생성 이벤트 처리 중 오류가 발생했습니다: ${e.message}" }
        }
    }

    // 향후 구현할 메서드들

    /**
     * 웰컴 이메일을 전송합니다.
     */
    // private fun sendWelcomeEmail(event: UserCreatedEvent) {
    //     // TODO: 이메일 서비스 연동
    // }

    /**
     * 사용자 초기 설정을 생성합니다.
     */
    // private fun initializeUserSettings(event: UserCreatedEvent) {
    //     // TODO: 기본 설정 생성 (알림 설정, 프라이버시 설정 등)
    // }

    /**
     * 분석 시스템에 이벤트를 전송합니다.
     */
    // private fun sendAnalyticsEvent(event: UserCreatedEvent) {
    //     // TODO: Google Analytics, Mixpanel 등 연동
    // }

    /**
     * 외부 시스템에 알립니다.
     */
    // private fun notifyExternalSystems(event: UserCreatedEvent) {
    //     // TODO: CRM, 마케팅 도구 등 연동
    // }
}
