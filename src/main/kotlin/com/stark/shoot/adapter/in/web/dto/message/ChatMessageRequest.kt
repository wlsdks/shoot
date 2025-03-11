package com.stark.shoot.adapter.`in`.web.dto.message

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ChatMessageRequest(
    @JsonProperty("id") val id: String? = null,
    @JsonProperty("tempId") var tempId: String? = null,
    @JsonProperty("roomId") val roomId: String,
    @JsonProperty("senderId") val senderId: String,
    @JsonProperty("content") val content: MessageContentRequest,
    @JsonProperty("status") var status: String? = null,
    @JsonProperty("readBy") val readBy: Map<String, Boolean>? = null,
    @JsonProperty("metadata") var metadata: MutableMap<String, Any> = mutableMapOf()
)