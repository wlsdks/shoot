package com.stark.shoot.application.port.out.notification

import com.stark.shoot.domain.event.NotificationEvent
import com.stark.shoot.infrastructure.exception.web.KafkaPublishException

interface PublishNotificationEventPort {

    /**
     * 알림 이벤트를 발행합니다.
     *
     * @param event 알림 이벤트 객체
     * @throws KafkaPublishException 이벤트 발행 실패 시 발생
     */
    fun publishEvent(event: NotificationEvent)

}
