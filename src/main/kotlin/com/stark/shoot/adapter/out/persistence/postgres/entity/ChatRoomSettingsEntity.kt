package com.stark.shoot.adapter.out.persistence.postgres.entity

import jakarta.persistence.Embeddable

@Embeddable
class ChatRoomSettingsEntity(
    isNotificationEnabled: Boolean,
    retentionDays: Int?,
    isEncrypted: Boolean,
    customSettingsJson: String?
) {
    var isNotificationEnabled: Boolean = isNotificationEnabled
        protected set

    var retentionDays: Int? = retentionDays
        protected set

    var isEncrypted: Boolean = isEncrypted
        protected set

    var customSettingsJson: String? = customSettingsJson
        protected set
}