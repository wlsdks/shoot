package com.stark.shoot.application.port.out.notification

import com.stark.shoot.domain.notification.event.NotificationEvent

interface PublishNotificationEventPort {

    fun publishEvent(event: NotificationEvent): Boolean

}