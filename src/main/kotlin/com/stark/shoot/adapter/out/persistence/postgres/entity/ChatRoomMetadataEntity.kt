package com.stark.shoot.adapter.out.persistence.postgres.entity

import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Embeddable
class ChatRoomMetadataEntity(
    displayTitle: String?,
    type: ChatRoomType,
    settings: ChatRoomSettingsEntity,
    announcement: String?
) {
    var displayTitle: String? = displayTitle
        protected set

    @Enumerated(EnumType.STRING)
    var type: ChatRoomType = type
        protected set

    @Embedded
    var settings: ChatRoomSettingsEntity = settings
        protected set

    var announcement: String? = announcement
        protected set
}