package com.stark.shoot.adapter.out.persistence.mongodb.adapter.notification

import com.stark.shoot.adapter.out.persistence.mongodb.document.notification.NotificationDocument
import com.stark.shoot.adapter.out.persistence.mongodb.repository.NotificationMongoRepository
import com.stark.shoot.application.port.out.notification.SaveNotificationPort
import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.infrastructure.annotation.Adapter

@Adapter
class SaveNotificationMongoAdapter(
    private val notificationMongoRepository: NotificationMongoRepository
) : SaveNotificationPort {

    /**
     * 알림을 저장합니다.
     *
     * @param notification 알림 객체
     * @return 저장된 알림 객체
     */
    override fun saveNotification(notification: Notification): Notification {
        val document = NotificationDocument.fromDomain(notification)
        val savedDocument = notificationMongoRepository.save(document)
        return savedDocument.toDomain()
    }

    /**
     * 알림 목록을 저장합니다.
     *
     * @param notifications 알림 객체 목록
     * @return 저장된 알림 객체 목록
     */
    override fun saveNotifications(notifications: List<Notification>): List<Notification> {
        val documents = notifications.map { NotificationDocument.fromDomain(it) }
        val savedDocuments = notificationMongoRepository.saveAll(documents)
        return savedDocuments.map { it.toDomain() }
    }

}