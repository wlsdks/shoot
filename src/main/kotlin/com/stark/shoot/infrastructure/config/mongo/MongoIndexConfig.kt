package com.stark.shoot.infrastructure.config.mongo

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.ChatMessageDocument
import org.bson.Document
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.index.IndexFilter

@Configuration
class MongoIndexConfig {

    private class SimpleIndexFilter(private val filter: Document) : IndexFilter {
        override fun getFilterObject(): Document = filter
    }

    @Bean
    fun mongoIndexInitializer(mongoTemplate: MongoTemplate): CommandLineRunner {
        return CommandLineRunner {
            createMessageIndexes(mongoTemplate)
        }
    }

    private fun createMessageIndexes(mongoTemplate: MongoTemplate) {
        mongoTemplate.indexOps(ChatMessageDocument::class.java).ensureIndex(
            Index().on("roomId", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
                .on("_id", Sort.Direction.DESC)
                .named("room_message_combined_idx")
        )

        val unreadIndex = CompoundIndexDefinition(
            Document()
                .append("roomId", 1)
                .append("readBy", 1)
        ).named("unread_messages_idx")
        unreadIndex.partial(SimpleIndexFilter(Document("readBy", Document("\$exists", true))))
        mongoTemplate.indexOps(ChatMessageDocument::class.java).ensureIndex(unreadIndex)

        mongoTemplate.indexOps(ChatMessageDocument::class.java).ensureIndex(
            Index().on("senderId", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
                .named("sender_date_idx")
        )

        mongoTemplate.indexOps(ChatMessageDocument::class.java).ensureIndex(
            Index().on("createdAt", Sort.Direction.ASC)
                .expire(365 * 24 * 60 * 60) // 1년 후 자동 만료
                .named("message_ttl_idx")
        )
    }

}