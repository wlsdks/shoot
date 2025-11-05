package com.stark.shoot.domain.chat.constants

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Chat Context 전용 상수
 */
@ConfigurationProperties(prefix = "app.domain.message")
data class MessageConstants(
    val maxContentLength: Int = 4000,
    val maxAttachmentSize: Long = 50 * 1024 * 1024, // 50MB
    val batchSize: Int = 100
)
