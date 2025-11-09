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
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition
import org.springframework.data.mongodb.core.index.IndexOperations
import org.bson.Document
import java.time.Instant

/**
 * MessageReaction Persistence Layer 오류/엣지케이스 테스트
 *
 * 테스트 범위:
 * - Invalid input 처리
 * - Null/Empty 값 처리
 * - Duplicate key violation
 * - Large data sets
 * - Concurrent modifications
 * - MongoDB constraints
 */
@DataMongoTest
@Import(MessageReactionMapper::class, MessageReactionPersistenceAdapter::class)
@DisplayName("MessageReaction 오류/엣지케이스 테스트")
class MessageReactionErrorCaseTest {

    @Autowired
    private lateinit var adapter: MessageReactionPersistenceAdapter

    @Autowired
    private lateinit var repository: MessageReactionRepository

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    @BeforeEach
    fun setUp() {
        repository.deleteAll()

        // Create unique index
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
            // Index might already exist
        }
    }

    @AfterEach
    fun tearDown() {
        repository.deleteAll()
    }

    @Nested
    @DisplayName("Invalid Input 테스트")
    inner class InvalidInputTest {

        @Test
        @DisplayName("[error] 빈 messageId는 예외 발생")
        fun `빈 messageId는 예외 발생`() {
            // given - Blank messageId
            val invalidIds = listOf(
                "",
                "   ",
                "\t",
                "\n"
            )

            // when & then - MessageId.from validates isNotBlank()
            invalidIds.forEach { invalidId ->
                assertThatThrownBy {
                    MessageId.from(invalidId)
                }.isInstanceOf(IllegalArgumentException::class.java)
                    .hasMessageContaining("메시지 ID는 비어있을 수 없습니다")
            }
        }

        @Test
        @DisplayName("[edge] 잘못된 형식의 messageId도 저장 가능 (format 검증 없음)")
        fun `잘못된 형식의 messageId도 저장 가능 (format 검증 없음)`() {
            // given - MessageId doesn't validate ObjectId format, only blank check
            val nonStandardIds = listOf(
                "invalid",
                "12345",
                "not-an-objectid",
                "ZZZZZZZZZZZZZZZZZZZZZZZZ"
            )

            // when & then - These should be allowed
            nonStandardIds.forEach { nonStandardId ->
                val messageId = MessageId.from(nonStandardId)
                val reaction = MessageReaction.create(
                    messageId = messageId,
                    userId = UserId.from(1L),
                    reactionType = ReactionType.LIKE
                )

                val saved = adapter.save(reaction)
                assertThat(saved.messageId.value).isEqualTo(nonStandardId)
            }
        }

        @Test
        @DisplayName("[error] 음수 userId는 도메인에서 검증하여 예외 발생")
        fun `음수 userId는 도메인에서 검증하여 예외 발생`() {
            // given
            val negativeUserIds = listOf(-1L, -999L, Long.MIN_VALUE)

            // when & then - UserId.from validates value > 0
            negativeUserIds.forEach { negativeId ->
                assertThatThrownBy {
                    UserId.from(negativeId)
                }.isInstanceOf(IllegalArgumentException::class.java)
                    .hasMessageContaining("사용자 ID는 양수여야 합니다")
            }
        }

        @Test
        @DisplayName("[error] 0인 userId도 검증하여 예외 발생")
        fun `0인 userId도 검증하여 예외 발생`() {
            // given & when & then - UserId.from validates value > 0
            assertThatThrownBy {
                UserId.from(0L)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("사용자 ID는 양수여야 합니다")
        }

        @Test
        @DisplayName("[edge] 매우 큰 userId 값 처리")
        fun `매우 큰 userId 값 처리`() {
            // given
            val messageId = MessageId.from("507f1f77bcf86cd799439011")
            val maxUserId = Long.MAX_VALUE

            // when
            val reaction = MessageReaction.create(
                messageId = messageId,
                userId = UserId.from(maxUserId),
                reactionType = ReactionType.LIKE
            )
            val saved = adapter.save(reaction)

            // then
            assertThat(saved.userId.value).isEqualTo(maxUserId)

            val found = adapter.findByMessageIdAndUserId(messageId, UserId.from(maxUserId))
            assertThat(found).isNotNull
            assertThat(found!!.userId.value).isEqualTo(maxUserId)
        }
    }

    @Nested
    @DisplayName("Duplicate Key Violation 테스트")
    inner class DuplicateKeyTest {

        @Test
        @DisplayName("[error] 동일 messageId + userId로 저장 시 DuplicateKeyException")
        fun `동일 messageId + userId로 저장 시 DuplicateKeyException`() {
            // given
            val messageId = "507f1f77bcf86cd799439011"
            val userId = 123L

            val doc1 = MessageReactionDocument(
                messageId = messageId,
                userId = userId,
                reactionType = "LIKE"
            )
            repository.save(doc1)

            // when & then
            val doc2 = MessageReactionDocument(
                messageId = messageId,
                userId = userId,
                reactionType = "SAD"
            )

            assertThatThrownBy {
                repository.save(doc2)
            }.isInstanceOf(DuplicateKeyException::class.java)
        }

        @Test
        @DisplayName("[edge] 같은 messageId에 다른 userId는 허용")
        fun `같은 messageId에 다른 userId는 허용`() {
            // given
            val messageId = MessageId.from("507f1f77bcf86cd799439011")

            // when - 100명의 다른 사용자가 같은 메시지에 리액션
            val reactions = (1L..100L).map { userId ->
                MessageReaction.create(
                    messageId = messageId,
                    userId = UserId.from(userId),
                    reactionType = ReactionType.values().random()
                )
            }

            reactions.forEach { adapter.save(it) }

            // then
            val allReactions = adapter.findAllByMessageId(messageId)
            assertThat(allReactions).hasSize(100)
            assertThat(allReactions.map { it.userId.value }).containsExactlyInAnyOrderElementsOf(
                (1L..100L).toList()
            )
        }

        @Test
        @DisplayName("[edge] 같은 userId가 다른 messageId에 리액션 가능")
        fun `같은 userId가 다른 messageId에 리액션 가능`() {
            // given
            val userId = UserId.from(999L)

            // when - 한 사용자가 100개의 다른 메시지에 리액션
            val messageIds = (1..100).map {
                "507f1f77bcf86cd7994390${String.format("%02d", it % 100)}"
            }.distinct()

            messageIds.forEach { msgId ->
                val reaction = MessageReaction.create(
                    messageId = MessageId.from(msgId),
                    userId = userId,
                    reactionType = ReactionType.LIKE
                )
                adapter.save(reaction)
            }

            // then - 저장 성공 확인
            messageIds.forEach { msgId ->
                val found = adapter.findByMessageIdAndUserId(MessageId.from(msgId), userId)
                assertThat(found).isNotNull
            }
        }
    }

    @Nested
    @DisplayName("Large Data Set 테스트")
    inner class LargeDataSetTest {

        @Test
        @DisplayName("[edge] 한 메시지에 1000개 리액션 처리")
        fun `한 메시지에 1000개 리액션 처리`() {
            // given
            val messageId = MessageId.from("507f1f77bcf86cd799439011")
            val reactionCount = 1000

            // when
            (1L..reactionCount).forEach { userId ->
                val reaction = MessageReaction.create(
                    messageId = messageId,
                    userId = UserId.from(userId),
                    reactionType = ReactionType.values()[(userId % ReactionType.values().size).toInt()]
                )
                adapter.save(reaction)
            }

            // then
            val allReactions = adapter.findAllByMessageId(messageId)
            assertThat(allReactions).hasSize(reactionCount)

            val summary = adapter.getReactionSummary(messageId)
            val totalCount = summary.values.sum()
            assertThat(totalCount).isEqualTo(reactionCount.toLong())
        }

        @Test
        @DisplayName("[edge] 특정 타입 리액션이 매우 많을 때 집계 성능")
        fun `특정 타입 리액션이 매우 많을 때 집계 성능`() {
            // given
            val messageId = MessageId.from("507f1f77bcf86cd799439012")
            val likeCount = 500

            // when - 500개 LIKE, 나머지 타입 각 10개씩
            (1L..likeCount).forEach { userId ->
                val reaction = MessageReaction.create(
                    messageId = messageId,
                    userId = UserId.from(userId),
                    reactionType = ReactionType.LIKE
                )
                adapter.save(reaction)
            }

            var userId = likeCount + 1L
            ReactionType.values().filter { it != ReactionType.LIKE }.forEach { type ->
                repeat(10) {
                    val reaction = MessageReaction.create(
                        messageId = messageId,
                        userId = UserId.from(userId++),
                        reactionType = type
                    )
                    adapter.save(reaction)
                }
            }

            // then
            val summary = adapter.getReactionSummary(messageId)
            assertThat(summary[ReactionType.LIKE]).isEqualTo(likeCount.toLong())

            val likeReactions = adapter.findAllByMessageIdAndReactionType(messageId, ReactionType.LIKE)
            assertThat(likeReactions).hasSize(likeCount)
        }
    }

    @Nested
    @DisplayName("Query Edge Case 테스트")
    inner class QueryEdgeCaseTest {

        @Test
        @DisplayName("[edge] 존재하지 않는 messageId 조회 시 빈 리스트")
        fun `존재하지 않는 messageId 조회 시 빈 리스트`() {
            // given
            val nonexistentMessageId = MessageId.from("000000000000000000000000")

            // when
            val reactions = adapter.findAllByMessageId(nonexistentMessageId)

            // then
            assertThat(reactions).isEmpty()
        }

        @Test
        @DisplayName("[edge] 존재하지 않는 userId 조회 시 null")
        fun `존재하지 않는 userId 조회 시 null`() {
            // given
            val messageId = MessageId.from("507f1f77bcf86cd799439011")
            adapter.save(MessageReaction.create(messageId, UserId.from(1L), ReactionType.LIKE))

            // when
            val found = adapter.findByMessageIdAndUserId(messageId, UserId.from(99999L))

            // then
            assertThat(found).isNull()
        }

        @Test
        @DisplayName("[edge] 리액션이 없는 메시지의 개수 조회는 0")
        fun `리액션이 없는 메시지의 개수 조회는 0`() {
            // given
            val messageId = MessageId.from("507f1f77bcf86cd799439011")

            // when
            val count = adapter.countByMessageId(messageId)

            // then
            assertThat(count).isEqualTo(0L)
        }

        @Test
        @DisplayName("[edge] 특정 타입 리액션이 없을 때 개수는 0")
        fun `특정 타입 리액션이 없을 때 개수는 0`() {
            // given
            val messageId = MessageId.from("507f1f77bcf86cd799439011")
            adapter.save(MessageReaction.create(messageId, UserId.from(1L), ReactionType.LIKE))

            // when
            val sadCount = adapter.countByMessageIdAndReactionType(messageId, ReactionType.SAD)

            // then
            assertThat(sadCount).isEqualTo(0L)
        }

        @Test
        @DisplayName("[edge] 리액션 요약 조회 시 0인 타입은 포함되지 않음")
        fun `리액션 요약 조회 시 0인 타입은 포함되지 않음`() {
            // given
            val messageId = MessageId.from("507f1f77bcf86cd799439011")
            adapter.save(MessageReaction.create(messageId, UserId.from(1L), ReactionType.LIKE))
            adapter.save(MessageReaction.create(messageId, UserId.from(2L), ReactionType.LIKE))

            // when
            val summary = adapter.getReactionSummary(messageId)

            // then
            assertThat(summary).hasSize(1)
            assertThat(summary[ReactionType.LIKE]).isEqualTo(2L)
            assertThat(summary).doesNotContainKey(ReactionType.SAD)
            assertThat(summary).doesNotContainKey(ReactionType.ANGRY)
        }
    }

    @Nested
    @DisplayName("Delete Edge Case 테스트")
    inner class DeleteEdgeCaseTest {

        @Test
        @DisplayName("[edge] 존재하지 않는 리액션 삭제는 오류 없이 성공")
        fun `존재하지 않는 리액션 삭제는 오류 없이 성공`() {
            // given
            val messageId = MessageId.from("507f1f77bcf86cd799439011")
            val userId = UserId.from(999L)

            // when & then - Should not throw exception
            adapter.deleteByMessageIdAndUserId(messageId, userId)
        }

        @Test
        @DisplayName("[edge] 이미 삭제된 리액션 재삭제 시도")
        fun `이미 삭제된 리액션 재삭제 시도`() {
            // given
            val messageId = MessageId.from("507f1f77bcf86cd799439011")
            val userId = UserId.from(1L)

            val reaction = MessageReaction.create(messageId, userId, ReactionType.LIKE)
            adapter.save(reaction)

            // when - 첫 번째 삭제
            adapter.deleteByMessageIdAndUserId(messageId, userId)

            // then - 두 번째 삭제 시도도 오류 없이 성공해야 함
            adapter.deleteByMessageIdAndUserId(messageId, userId)

            val found = adapter.findByMessageIdAndUserId(messageId, userId)
            assertThat(found).isNull()
        }

        @Test
        @DisplayName("[edge] 메시지의 일부 리액션 삭제 후 나머지 조회")
        fun `메시지의 일부 리액션 삭제 후 나머지 조회`() {
            // given
            val messageId = MessageId.from("507f1f77bcf86cd799439011")
            (1L..10L).forEach { userId ->
                adapter.save(MessageReaction.create(messageId, UserId.from(userId), ReactionType.LIKE))
            }

            // when - 홀수 userId만 삭제
            (1L..10L step 2).forEach { userId ->
                adapter.deleteByMessageIdAndUserId(messageId, UserId.from(userId))
            }

            // then - 짝수 userId만 남아있어야 함
            val remaining = adapter.findAllByMessageId(messageId)
            assertThat(remaining).hasSize(5)
            assertThat(remaining.map { it.userId.value }).containsExactlyInAnyOrder(
                2L, 4L, 6L, 8L, 10L
            )
        }

        @Test
        @DisplayName("[edge] 존재하지 않는 메시지의 전체 리액션 삭제")
        fun `존재하지 않는 메시지의 전체 리액션 삭제`() {
            // given
            val nonexistentMessageId = MessageId.from("000000000000000000000000")

            // when & then - Should not throw exception
            adapter.deleteAllByMessageId(nonexistentMessageId)
        }
    }

    @Nested
    @DisplayName("Update/Modify 시나리오 테스트")
    inner class UpdateScenarioTest {

        @Test
        @DisplayName("[edge] 같은 ID로 리액션 타입 변경 (덮어쓰기)")
        fun `같은 ID로 리액션 타입 변경 (덮어쓰기)`() {
            // given
            val messageId = MessageId.from("507f1f77bcf86cd799439011")
            val userId = UserId.from(1L)

            val initialReaction = MessageReaction.create(messageId, userId, ReactionType.LIKE)
            val saved = adapter.save(initialReaction)

            // when - 같은 사용자가 리액션 타입 변경 (LIKE -> SAD)
            val updatedReaction = saved.copy(
                reactionType = ReactionType.SAD,
                updatedAt = Instant.now()
            )
            adapter.save(updatedReaction)

            // then
            val found = adapter.findByMessageIdAndUserId(messageId, userId)
            assertThat(found).isNotNull
            assertThat(found!!.reactionType).isEqualTo(ReactionType.SAD)
            assertThat(found.updatedAt).isNotNull

            // 전체 개수는 여전히 1개
            val count = adapter.countByMessageId(messageId)
            assertThat(count).isEqualTo(1L)
        }

        @Test
        @DisplayName("[edge] 여러 번 리액션 타입 변경")
        fun `여러 번 리액션 타입 변경`() {
            // given
            val messageId = MessageId.from("507f1f77bcf86cd799439011")
            val userId = UserId.from(1L)

            var reaction = MessageReaction.create(messageId, userId, ReactionType.LIKE)
            reaction = adapter.save(reaction)

            // when - LIKE -> SAD -> ANGRY -> SURPRISED
            val types = listOf(ReactionType.SAD, ReactionType.ANGRY, ReactionType.SURPRISED)
            types.forEach { type ->
                reaction = reaction.copy(reactionType = type, updatedAt = Instant.now())
                reaction = adapter.save(reaction)
            }

            // then
            val found = adapter.findByMessageIdAndUserId(messageId, userId)
            assertThat(found!!.reactionType).isEqualTo(ReactionType.SURPRISED)

            // 전체 개수는 여전히 1개
            val count = adapter.countByMessageId(messageId)
            assertThat(count).isEqualTo(1L)
        }
    }

    @Nested
    @DisplayName("Timestamp 테스트")
    inner class TimestampTest {

        @Test
        @DisplayName("[edge] createdAt은 자동 설정됨")
        fun `createdAt은 자동 설정됨`() {
            // given
            val before = Instant.now().minusSeconds(1)
            val messageId = MessageId.from("507f1f77bcf86cd799439011")
            val userId = UserId.from(1L)

            // when
            val reaction = MessageReaction.create(messageId, userId, ReactionType.LIKE)
            val saved = adapter.save(reaction)
            val after = Instant.now().plusSeconds(1)

            // then
            assertThat(saved.createdAt).isNotNull
            assertThat(saved.createdAt).isBetween(before, after)
        }

        @Test
        @DisplayName("[edge] updatedAt은 수정 시에만 설정됨")
        fun `updatedAt은 수정 시에만 설정됨`() {
            // given
            val messageId = MessageId.from("507f1f77bcf86cd799439011")
            val userId = UserId.from(1L)

            val reaction = MessageReaction.create(messageId, userId, ReactionType.LIKE)
            val saved = adapter.save(reaction)

            // then - 최초 저장 시 updatedAt은 null
            assertThat(saved.updatedAt).isNull()

            // when - 수정
            val updated = saved.copy(
                reactionType = ReactionType.SAD,
                updatedAt = Instant.now()
            )
            val savedUpdated = adapter.save(updated)

            // then
            assertThat(savedUpdated.updatedAt).isNotNull
        }
    }

    @Nested
    @DisplayName("Boundary Value 테스트")
    inner class BoundaryValueTest {

        @Test
        @DisplayName("[edge] MessageId 최소 유효 길이")
        fun `MessageId 최소 유효 길이`() {
            // given - MongoDB ObjectId는 24자
            val validObjectId = "507f1f77bcf86cd799439011"
            assertThat(validObjectId).hasSize(24)

            // when
            val messageId = MessageId.from(validObjectId)
            val reaction = MessageReaction.create(
                messageId = messageId,
                userId = UserId.from(1L),
                reactionType = ReactionType.LIKE
            )

            // then
            val saved = adapter.save(reaction)
            assertThat(saved.messageId.value).isEqualTo(validObjectId)
        }

        @Test
        @DisplayName("[edge] UserId 유효 경계값 (1, MAX)")
        fun `UserId 유효 경계값 (1, MAX)`() {
            // given - UserId must be > 0, so valid boundaries are 1 and Long.MAX_VALUE
            val messageId = MessageId.from("507f1f77bcf86cd799439011")
            val validBoundaryValues = listOf(1L, Long.MAX_VALUE)

            // when & then
            validBoundaryValues.forEach { userId ->
                val reaction = MessageReaction.create(
                    messageId = messageId,
                    userId = UserId.from(userId),
                    reactionType = ReactionType.LIKE
                )

                val saved = adapter.save(reaction)
                assertThat(saved.userId.value).isEqualTo(userId)

                // Cleanup for next iteration
                adapter.deleteByMessageIdAndUserId(messageId, UserId.from(userId))
            }
        }
    }
}
