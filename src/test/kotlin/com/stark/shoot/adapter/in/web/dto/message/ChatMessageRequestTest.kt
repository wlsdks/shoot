package com.stark.shoot.adapter.`in`.web.dto.message

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class ChatMessageRequestTest {

    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    @Test
    fun `should deserialize JSON without metadata field`() {
        // Given: JSON without metadata field
        val json = """
            {
                "roomId": 123,
                "senderId": 456,
                "content": {
                    "type": "TEXT",
                    "text": "Hello, world!"
                }
            }
        """.trimIndent()

        // When: Deserializing the JSON
        val chatMessageRequest = objectMapper.readValue(json, ChatMessageRequest::class.java)

        // Then: The object should be created with default metadata
        assertNotNull(chatMessageRequest)
        assertNotNull(chatMessageRequest.metadata)
    }
}
