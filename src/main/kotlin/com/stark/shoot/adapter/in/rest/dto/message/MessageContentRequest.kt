package com.stark.shoot.adapter.`in`.rest.dto.message

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.stark.shoot.domain.chat.message.type.MessageType

@JsonIgnoreProperties(ignoreUnknown = true)
data class MessageContentRequest @JsonCreator constructor(
    @JsonProperty("text") val text: String,
    @JsonProperty("type") val type: MessageType,
    @JsonProperty("attachments") val attachments: List<String> = emptyList(),
    @JsonProperty("isEdited") val isEdited: Boolean = false,
    @JsonProperty("isDeleted") val isDeleted: Boolean = false,
    @JsonProperty("urlPreview") var urlPreview: UrlPreviewDto? = null
)