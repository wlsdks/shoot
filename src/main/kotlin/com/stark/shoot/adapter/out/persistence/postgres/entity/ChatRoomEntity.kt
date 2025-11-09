package com.stark.shoot.adapter.out.persistence.postgres.entity

import com.stark.shoot.domain.chatroom.type.ChatRoomType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant

@Entity
@Table(name = "chat_rooms")
class ChatRoomEntity(
    title: String?,
    type: ChatRoomType,
    announcement: String?,
    lastMessageId: Long?,
    lastActiveAt: Instant,
    isNotificationEnabled: Boolean = true,
    retentionDays: Int? = null,
    isEncrypted: Boolean = false,
    customSettings: String? = null
) : BaseEntity() {

    // BaseEntity에서 version 필드를 상속받으므로 별도 선언 불필요

    var title: String? = title
        protected set

    @Enumerated(EnumType.STRING)
    var type: ChatRoomType = type
        protected set

    var announcement: String? = announcement
        protected set

    var lastMessageId: Long? = lastMessageId
        protected set

    var lastActiveAt: Instant = lastActiveAt
        protected set

    // ChatRoomSettings fields (embedded as columns)
    var isNotificationEnabled: Boolean = isNotificationEnabled
        protected set

    var retentionDays: Int? = retentionDays
        protected set

    var isEncrypted: Boolean = isEncrypted
        protected set

    var customSettings: String? = customSettings
        protected set

    fun update(
        title: String? = this.title,
        type: ChatRoomType = this.type,
        announcement: String? = this.announcement,
        lastMessageId: Long? = this.lastMessageId,
        lastActiveAt: Instant = this.lastActiveAt,
        isNotificationEnabled: Boolean = this.isNotificationEnabled,
        retentionDays: Int? = this.retentionDays,
        isEncrypted: Boolean = this.isEncrypted,
        customSettings: String? = this.customSettings
    ) {
        this.title = title
        this.type = type
        this.announcement = announcement
        this.lastMessageId = lastMessageId
        this.lastActiveAt = lastActiveAt
        this.isNotificationEnabled = isNotificationEnabled
        this.retentionDays = retentionDays
        this.isEncrypted = isEncrypted
        this.customSettings = customSettings
    }

}
