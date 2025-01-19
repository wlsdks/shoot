package com.stark.shoot.infrastructure.config.kafka

// 토픽 상수는 별도 object 파일로 분리
object KafkaTopics {
    const val CHAT_MESSAGES = "chat-messages"
    const val CHAT_NOTIFICATIONS = "chat-notifications"
    const val CHAT_EVENTS = "chat-events"
}