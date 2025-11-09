package com.stark.shoot.adapter.persistence.mongodb.adapter.message

import com.stark.shoot.adapter.out.persistence.mongodb.adapter.message.MessageReactionPersistenceAdapter
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.reaction.MessageReactionDocument
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.MessageReactionMapper
import com.stark.shoot.adapter.out.persistence.mongodb.repository.MessageReactionRepository
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.reaction.MessageReaction
import com.stark.shoot.domain.chat.reaction.type.ReactionType
import com.stark.shoot.domain.shared.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition
import org.springframework.data.mongodb.core.index.IndexOperations
import org.springframework.data.domain.Sort
import org.bson.Document
import java.time.Instant

/**
 * MessageReactionPersistenceAdapter MongoDB 통합 테스트
 *
 * @DataMongoTest를 사용하여 실제 embedded MongoDB와 통합 테스트
 */
@DataMongoTest
@Import(MessageReactionMapper::class, MessageReactionPersistenceAdapter::class)
@DisplayName("MessageReaction MongoDB 통합 테스트")
class MessageReactionPersistenceAdapterIntegrationTest {

    @Autowired
    private lateinit var adapter: MessageReactionPersistenceAdapter

    @Autowired
    private lateinit var repository: MessageReactionRepository

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    @BeforeEach
    fun setUp() {
        repository.deleteAll()

        // Manually create unique index for embedded MongoDB
        // Note: Embedded MongoDB doesn't auto-create indexes from annotations
        val indexOps: IndexOperations = mongoTemplate.indexOps(MessageReactionDocument::class.java)
        val indexDefinition = CompoundIndexDefinition(
            Document()
                .append("messageId", 1)
                .append("userId", 1)
        ).unique()

        try {
            @Suppress("DEPRECATION")
            indexOps.ensureIndex(indexDefinition)
        } catch (e: Exception) {
            // Index might already exist, ignore
        }
    }

    @AfterEach
    fun tearDown() {
        repository.deleteAll()
    }

    @Nested
    @DisplayName("리액션 저장 테스트")
    inner class SaveReactionTest {

        @Test
        @DisplayName("[happy] 새로운 리액션을 저장할 수 있다")
        fun `새로운 리액션을 저장할 수 있다`() {
            // given
            val reaction = MessageReaction.create(
                messageId = MessageId.from("msg123"),
                userId = UserId.from(1L),
                reactionType = ReactionType.LIKE
            )

            // when
            val saved = adapter.save(reaction)

            // then
            assertThat(saved.id).isNotNull
            assertThat(saved.messageId).isEqualTo(MessageId.from("msg123"))
            assertThat(saved.userId).isEqualTo(UserId.from(1L))
            assertThat(saved.reactionType).isEqualTo(ReactionType.LIKE)
            assertThat(saved.createdAt).isNotNull

            // MongoDB에서 직접 조회하여 검증
            val documents = repository.findAllByMessageId("msg123")
            assertThat(documents).hasSize(1)
            assertThat(documents[0].userId).isEqualTo(1L)
            assertThat(documents[0].reactionType).isEqualTo("LIKE")
        }

        @Test
        @DisplayName("[happy] 기존 리액션을 수정할 수 있다")
        fun `기존 리액션을 수정할 수 있다`() {
            // given - 먼저 LIKE 리액션 저장
            val reaction = MessageReaction.create(
                messageId = MessageId.from("msg456"),
                userId = UserId.from(2L),
                reactionType = ReactionType.LIKE
            )
            val saved = adapter.save(reaction)

            // when - SAD로 변경
            val updated = saved.copy(
                reactionType = ReactionType.SAD,
                updatedAt = Instant.now()
            )
            val result = adapter.save(updated)

            // then
            assertThat(result.id).isEqualTo(saved.id)
            assertThat(result.reactionType).isEqualTo(ReactionType.SAD)
            assertThat(result.updatedAt).isNotNull

            // MongoDB에서 검증
            val documents = repository.findAllByMessageId("msg456")
            assertThat(documents).hasSize(1)
            assertThat(documents[0].reactionType).isEqualTo("SAD")
        }

        @Test
        @DisplayName("[happy] 동일 메시지에 여러 사용자가 리액션을 추가할 수 있다")
        fun `동일 메시지에 여러 사용자가 리액션을 추가할 수 있다`() {
            // given
            val messageId = MessageId.from("msg789")
            val reactions = listOf(
                MessageReaction.create(messageId, UserId.from(1L), ReactionType.LIKE),
                MessageReaction.create(messageId, UserId.from(2L), ReactionType.SAD),
                MessageReaction.create(messageId, UserId.from(3L), ReactionType.ANGRY)
            )

            // when
            reactions.forEach { adapter.save(it) }

            // then
            val allReactions = adapter.findAllByMessageId(messageId)
            assertThat(allReactions).hasSize(3)
            assertThat(allReactions.map { it.userId }).containsExactlyInAnyOrder(
                UserId.from(1L),
                UserId.from(2L),
                UserId.from(3L)
            )
        }
    }

    @Nested
    @DisplayName("리액션 조회 테스트")
    inner class FindReactionTest {

        @Test
        @DisplayName("[happy] 메시지 ID와 사용자 ID로 리액션을 조회할 수 있다")
        fun `메시지 ID와 사용자 ID로 리액션을 조회할 수 있다`() {
            // given
            val messageId = MessageId.from("msg100")
            val userId = UserId.from(10L)
            val reaction = MessageReaction.create(messageId, userId, ReactionType.SURPRISED)
            adapter.save(reaction)

            // when
            val found = adapter.findByMessageIdAndUserId(messageId, userId)

            // then
            assertThat(found).isNotNull
            assertThat(found!!.messageId).isEqualTo(messageId)
            assertThat(found.userId).isEqualTo(userId)
            assertThat(found.reactionType).isEqualTo(ReactionType.SURPRISED)
        }

        @Test
        @DisplayName("[happy] 존재하지 않는 리액션 조회 시 null을 반환한다")
        fun `존재하지 않는 리액션 조회 시 null을 반환한다`() {
            // when
            val found = adapter.findByMessageIdAndUserId(
                MessageId.from("nonexistent"),
                UserId.from(999L)
            )

            // then
            assertThat(found).isNull()
        }

        @Test
        @DisplayName("[happy] 메시지의 모든 리액션을 조회할 수 있다")
        fun `메시지의 모든 리액션을 조회할 수 있다`() {
            // given
            val messageId = MessageId.from("msg200")
            adapter.save(MessageReaction.create(messageId, UserId.from(1L), ReactionType.LIKE))
            adapter.save(MessageReaction.create(messageId, UserId.from(2L), ReactionType.LIKE))
            adapter.save(MessageReaction.create(messageId, UserId.from(3L), ReactionType.SAD))

            // when
            val allReactions = adapter.findAllByMessageId(messageId)

            // then
            assertThat(allReactions).hasSize(3)
        }

        @Test
        @DisplayName("[happy] 특정 타입의 리액션만 조회할 수 있다")
        fun `특정 타입의 리액션만 조회할 수 있다`() {
            // given
            val messageId = MessageId.from("msg300")
            adapter.save(MessageReaction.create(messageId, UserId.from(1L), ReactionType.LIKE))
            adapter.save(MessageReaction.create(messageId, UserId.from(2L), ReactionType.LIKE))
            adapter.save(MessageReaction.create(messageId, UserId.from(3L), ReactionType.SAD))

            // when
            val likeReactions = adapter.findAllByMessageIdAndReactionType(messageId, ReactionType.LIKE)

            // then
            assertThat(likeReactions).hasSize(2)
            assertThat(likeReactions.all { it.reactionType == ReactionType.LIKE }).isTrue()
        }
    }

    @Nested
    @DisplayName("리액션 개수 조회 테스트")
    inner class CountReactionTest {

        @Test
        @DisplayName("[happy] 메시지의 전체 리액션 개수를 조회할 수 있다")
        fun `메시지의 전체 리액션 개수를 조회할 수 있다`() {
            // given
            val messageId = MessageId.from("msg400")
            adapter.save(MessageReaction.create(messageId, UserId.from(1L), ReactionType.LIKE))
            adapter.save(MessageReaction.create(messageId, UserId.from(2L), ReactionType.SAD))
            adapter.save(MessageReaction.create(messageId, UserId.from(3L), ReactionType.ANGRY))

            // when
            val count = adapter.countByMessageId(messageId)

            // then
            assertThat(count).isEqualTo(3L)
        }

        @Test
        @DisplayName("[happy] 특정 타입의 리액션 개수를 조회할 수 있다")
        fun `특정 타입의 리액션 개수를 조회할 수 있다`() {
            // given
            val messageId = MessageId.from("msg500")
            adapter.save(MessageReaction.create(messageId, UserId.from(1L), ReactionType.LIKE))
            adapter.save(MessageReaction.create(messageId, UserId.from(2L), ReactionType.LIKE))
            adapter.save(MessageReaction.create(messageId, UserId.from(3L), ReactionType.SAD))

            // when
            val likeCount = adapter.countByMessageIdAndReactionType(messageId, ReactionType.LIKE)

            // then
            assertThat(likeCount).isEqualTo(2L)
        }

        @Test
        @DisplayName("[happy] 리액션 요약 정보를 조회할 수 있다")
        fun `리액션 요약 정보를 조회할 수 있다`() {
            // given
            val messageId = MessageId.from("msg600")
            adapter.save(MessageReaction.create(messageId, UserId.from(1L), ReactionType.LIKE))
            adapter.save(MessageReaction.create(messageId, UserId.from(2L), ReactionType.LIKE))
            adapter.save(MessageReaction.create(messageId, UserId.from(3L), ReactionType.LIKE))
            adapter.save(MessageReaction.create(messageId, UserId.from(4L), ReactionType.SAD))
            adapter.save(MessageReaction.create(messageId, UserId.from(5L), ReactionType.SAD))
            adapter.save(MessageReaction.create(messageId, UserId.from(6L), ReactionType.ANGRY))

            // when
            val summary = adapter.getReactionSummary(messageId)

            // then
            assertThat(summary).hasSize(3)
            assertThat(summary[ReactionType.LIKE]).isEqualTo(3L)
            assertThat(summary[ReactionType.SAD]).isEqualTo(2L)
            assertThat(summary[ReactionType.ANGRY]).isEqualTo(1L)
        }
    }

    @Nested
    @DisplayName("리액션 삭제 테스트")
    inner class DeleteReactionTest {

        @Test
        @DisplayName("[happy] 메시지 ID와 사용자 ID로 리액션을 삭제할 수 있다")
        fun `메시지 ID와 사용자 ID로 리액션을 삭제할 수 있다`() {
            // given
            val messageId = MessageId.from("msg700")
            val userId = UserId.from(7L)
            adapter.save(MessageReaction.create(messageId, userId, ReactionType.LIKE))

            // when
            adapter.deleteByMessageIdAndUserId(messageId, userId)

            // then
            val found = adapter.findByMessageIdAndUserId(messageId, userId)
            assertThat(found).isNull()
        }

        @Test
        @DisplayName("[happy] 메시지의 모든 리액션을 삭제할 수 있다")
        fun `메시지의 모든 리액션을 삭제할 수 있다`() {
            // given
            val messageId = MessageId.from("msg800")
            adapter.save(MessageReaction.create(messageId, UserId.from(1L), ReactionType.LIKE))
            adapter.save(MessageReaction.create(messageId, UserId.from(2L), ReactionType.SAD))
            adapter.save(MessageReaction.create(messageId, UserId.from(3L), ReactionType.ANGRY))

            // when
            adapter.deleteAllByMessageId(messageId)

            // then
            val allReactions = adapter.findAllByMessageId(messageId)
            assertThat(allReactions).isEmpty()
        }
    }

    @Nested
    @DisplayName("Unique 제약 테스트")
    inner class UniqueConstraintTest {

        @Test
        @DisplayName("[edge] 같은 사용자는 메시지당 1개의 리액션만 가질 수 있다")
        fun `같은 사용자는 메시지당 1개의 리액션만 가질 수 있다`() {
            // given
            val messageId = "msg999"
            val userId = 99L
            val document1 = MessageReactionDocument(
                messageId = messageId,
                userId = userId,
                reactionType = "LIKE"
            )
            repository.save(document1)

            // when & then - 같은 messageId, userId로 저장하면 unique constraint violation
            val document2 = MessageReactionDocument(
                messageId = messageId,
                userId = userId,
                reactionType = "SAD"
            )

            assertThrows<org.springframework.dao.DuplicateKeyException> {
                repository.save(document2)
            }
        }
    }
}
