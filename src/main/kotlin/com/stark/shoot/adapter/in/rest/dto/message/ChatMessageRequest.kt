package com.stark.shoot.adapter.`in`.rest.dto.message

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.stark.shoot.domain.chat.message.type.MessageStatus

@JsonIgnoreProperties(ignoreUnknown = true)
data class ChatMessageRequest(
    @JsonProperty("id") val id: String? = null,
    @JsonProperty("tempId") var tempId: String? = null,
    @JsonProperty("roomId") val roomId: Long,
    @JsonProperty("senderId") val senderId: Long,
    @JsonProperty("content") val content: MessageContentRequest,
    @JsonProperty("threadId") val threadId: String? = null,
    @JsonProperty("status") var status: MessageStatus? = null,
    @JsonProperty("readBy") val readBy: Map<String, Boolean>? = null,
    @JsonProperty("metadata") var metadata: ChatMessageMetadataRequest = ChatMessageMetadataRequest()
)
