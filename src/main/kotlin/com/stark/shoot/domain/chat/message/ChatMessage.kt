package com.stark.shoot.domain.chat.message

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageStatus
import java.time.Instant

data class ChatMessage(
    val id: String? = null,
    val roomId: String,
    val senderId: String,
    val content: MessageContent,
    val status: MessageStatus,
    val replyToMessageId: String? = null,
    val reactions: Map<String, Set<String>> = emptyMap(),
    val mentions: Set<String> = emptySet(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant? = null,
    val isDeleted: Boolean = false,
    val readBy: MutableMap<String, Boolean> = mutableMapOf() // 읽음 상태 추가
) {
    fun toJson(): String {
        val reactionsJson = reactions.entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
            """"$key": [${value.joinToString { "\"$it\"" }}]"""
        }
        val mentionsJson = mentions.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
        val readByJson = readBy.entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
            """"$key": $value"""
        }
        return """
            {
                "id": "${id ?: ""}",
                "roomId": "$roomId",
                "senderId": "$senderId",
                "content": {"text": "${content.text}", "type": "${content.type}"},
                "status": "$status",
                "replyToMessageId": "${replyToMessageId ?: ""}",
                "reactions": $reactionsJson,
                "mentions": $mentionsJson,
                "createdAt": "${createdAt.toString()}",
                "updatedAt": "${updatedAt?.toString() ?: ""}",
                "isDeleted": $isDeleted,
                "readBy": $readByJson
            }
        """.trimIndent().replace("\n", "").replace("\\s+".toRegex(), " ")
    }
}