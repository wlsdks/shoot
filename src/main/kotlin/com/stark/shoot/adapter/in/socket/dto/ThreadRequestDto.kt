package com.stark.shoot.adapter.`in`.socket.dto

/**
 * 스레드 관련 STOMP 요청 DTO 모음
 */
data class ThreadListRequestDto(
    val roomId: Long,
    val userId: Long,
    val lastThreadId: String? = null,
    val limit: Int = 20,
)

data class ThreadMessagesRequestDto(
    val threadId: String,
    val userId: Long,
    val lastMessageId: String? = null,
    val limit: Int = 20,
)

data class ThreadDetailRequestDto(
    val threadId: String,
    val userId: Long,
    val lastMessageId: String? = null,
    val limit: Int = 20,
)
