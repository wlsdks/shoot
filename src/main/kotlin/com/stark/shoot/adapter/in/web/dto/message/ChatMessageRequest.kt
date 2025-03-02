package com.stark.shoot.adapter.`in`.web.dto.message

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ChatMessageRequest(
    @JsonProperty("roomId") val roomId: String,
    @JsonProperty("senderId") val senderId: String,
    @JsonProperty("content") val content: MessageContentRequest,
    @JsonProperty("tempId") var tempId: String? = null,
    @JsonProperty("status") var status: String? = null,
    @JsonProperty("metadata") var metadata: MutableMap<String, Any> = mutableMapOf(),
    @JsonProperty("id") val id: String? = null, // 추가
    @JsonProperty("readBy") val readBy: Map<String, Boolean>? = null // 추가
)