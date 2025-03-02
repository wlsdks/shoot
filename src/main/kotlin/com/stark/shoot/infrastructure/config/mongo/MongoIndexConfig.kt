package com.stark.shoot.infrastructure.config.mongo

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.ChatMessageDocument
import com.stark.shoot.adapter.out.persistence.mongodb.document.room.ChatRoomDocument
import com.stark.shoot.adapter.out.persistence.mongodb.document.user.UserDocument
import org.bson.Document
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.index.IndexFilter
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

@Configuration
class MongoIndexConfig {

    private class SimpleIndexFilter(private val filter: Document) : IndexFilter {
        override fun getFilterObject(): Document = filter
    }

    @Bean
    fun mongoIndexInitializer(mongoTemplate: MongoTemplate): CommandLineRunner {
        return CommandLineRunner {
            cleanupEmptyUserCodes(mongoTemplate)
            createMessageIndexes(mongoTemplate)
            createChatRoomIndexes(mongoTemplate)
            createUserIndexes(mongoTemplate)
        }
    }

    private fun cleanupEmptyUserCodes(mongoTemplate: MongoTemplate) {
        mongoTemplate.updateMulti(
            Query(Criteria.where("userCode").`is`("")),
            Update().set("userCode", null),
            UserDocument::class.java
        )
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

    private fun createChatRoomIndexes(mongoTemplate: MongoTemplate) {
        mongoTemplate.indexOps(ChatRoomDocument::class.java).ensureIndex(
            Index().on("participants", Sort.Direction.ASC)
                .on("lastActiveAt", Sort.Direction.DESC)
                .named("participant_activity_idx")
        )

        // 기존 pinned_rooms_idx 인덱스 삭제
        try {
            mongoTemplate.indexOps(ChatRoomDocument::class.java).dropIndex("pinned_rooms_idx")
        } catch (e: Exception) {
            // 인덱스가 존재하지 않을 경우 에러 무시
        }
        // 새로운 pinned_rooms_idx 인덱스 생성
        mongoTemplate.indexOps(ChatRoomDocument::class.java).ensureIndex(
            CompoundIndexDefinition(
                Document()
                    .append("participants", 1)
                    .append("metadata.participantsMetadata.isPinned", 1)
                    .append("lastActiveAt", -1)
            ).named("pinned_rooms_idx")
        )

        // 기존 room_type_idx 인덱스 삭제 (충돌 방지)
        try {
            mongoTemplate.indexOps(ChatRoomDocument::class.java).dropIndex("room_type_idx")
        } catch (e: Exception) {
            // 인덱스가 존재하지 않을 경우 에러 무시
        }
        // 새로운 room_type_idx 인덱스 생성
        mongoTemplate.indexOps(ChatRoomDocument::class.java).ensureIndex(
            Index().on("metadata.type", Sort.Direction.ASC)
                .on("lastActiveAt", Sort.Direction.DESC)
                .named("room_type_idx")
        )

        mongoTemplate.indexOps(ChatRoomDocument::class.java).ensureIndex(
            CompoundIndexDefinition(
                Document()
                    .append("participants", 1)
                    .append("metadata.type", 1)
            ).named("direct_chat_idx")
        )
    }

    private fun createUserIndexes(mongoTemplate: MongoTemplate) {
        mongoTemplate.indexOps(UserDocument::class.java).ensureIndex(
            Index().on("username", Sort.Direction.ASC)
                .unique()
                .named("username_idx")
        )

        val userCodeIndex = CompoundIndexDefinition(
            Document().append("userCode", 1)
        ).named("user_code_idx").unique()
        userCodeIndex.partial(
            SimpleIndexFilter(Document("userCode", Document("\$exists", true)))
        )
        mongoTemplate.indexOps(UserDocument::class.java).ensureIndex(userCodeIndex)

        mongoTemplate.indexOps(UserDocument::class.java).ensureIndex(
            Index().on("friends", Sort.Direction.ASC)
                .named("friends_idx")
        )

        mongoTemplate.indexOps(UserDocument::class.java).ensureIndex(
            Index().on("incomingFriendRequests", Sort.Direction.ASC)
                .named("incoming_requests_idx")
        )
        mongoTemplate.indexOps(UserDocument::class.java).ensureIndex(
            Index().on("outgoingFriendRequests", Sort.Direction.ASC)
                .named("outgoing_requests_idx")
        )

        // 텍스트 인덱스 생성
        mongoTemplate.indexOps(UserDocument::class.java).ensureIndex(
            org.springframework.data.mongodb.core.index.TextIndexDefinition.builder()
                .onField("username", 3f) // username 가중치 3
                .onField("nickname", 2f) // nickname 가중치 2
                .named("user_text_search_idx")
                .build()
        )
    }

}