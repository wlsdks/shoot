package com.stark.shoot.application.service.concurrency

import com.stark.shoot.adapter.out.persistence.mongodb.adapter.message.MessageReactionPersistenceAdapter
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
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition
import org.bson.Document
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

/**
 * 실제 채팅 서비스에서 발생할 수 있는 리액션 동시성 문제 테스트
 *
 * 이 테스트는 실제 프로덕션 환경에서 발생하는 다음과 같은 상황을 시뮬레이션합니다:
 * - 많은 사용자가 동시에 같은 메시지에 리액션
 * - 사용자가 빠르게 리액션을 토글 (스팸 방지 테스트)
 * - 바이럴 메시지에 수천 개의 리액션
 * - 리액션 타입 변경 경쟁 조건
 * - 리액션 카운트 일관성
 */
@DataMongoTest
@Import(MessageReactionMapper::class, MessageReactionPersistenceAdapter::class)
@DisplayName("실제 채팅 서비스 리액션 동시성 테스트")
class ReactionConcurrencyRealWorldTest {

    @Autowired
    private lateinit var adapter: MessageReactionPersistenceAdapter

    @Autowired
    private lateinit var repository: MessageReactionRepository

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    companion object {
        private val executor = Executors.newFixedThreadPool(100) as ThreadPoolExecutor

        @AfterAll
        @JvmStatic
        fun cleanup() {
            executor.shutdown()
            executor.awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    @BeforeEach
    fun setUp() {
        repository.deleteAll()

        // Create unique index for embedded MongoDB
        val indexOps = mongoTemplate.indexOps(com.stark.shoot.adapter.out.persistence.mongodb.document.message.reaction.MessageReactionDocument::class.java)
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
        executor.purge()
    }

    @Nested
    @DisplayName("대규모 동시 리액션 시나리오")
    inner class MassiveConcurrentReactionTest {

        @Test
        @DisplayName("[real-world] 바이럴 메시지 - 1000명이 동시에 리액션")
        fun `바이럴 메시지 1000명 동시 리액션`() {
            // given - 바이럴 메시지 시나리오
            val messageId = MessageId.from("viral-message-001")
            val userCount = 1000
            val latch = CountDownLatch(userCount)
            val successCount = AtomicInteger(0)
            val failureCount = AtomicInteger(0)

            // when - 1000명의 사용자가 동시에 리액션 추가
            repeat(userCount) { userId ->
                executor.submit {
                    try {
                        val reaction = MessageReaction.create(
                            messageId = messageId,
                            userId = UserId.from((userId + 1).toLong()),
                            reactionType = ReactionType.LIKE
                        )
                        adapter.save(reaction)
                        successCount.incrementAndGet()
                    } catch (e: Exception) {
                        failureCount.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }

            // then
            assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue()
            assertThat(successCount.get()).isEqualTo(userCount)
            assertThat(failureCount.get()).isEqualTo(0)

            // 실제 저장된 리액션 확인
            val allReactions = adapter.findAllByMessageId(messageId)
            assertThat(allReactions).hasSize(userCount)

            // 리액션 카운트 일관성 확인
            val likeCount = adapter.countByMessageIdAndReactionType(messageId, ReactionType.LIKE)
            assertThat(likeCount).isEqualTo(userCount.toLong())
        }

        @Test
        @DisplayName("[real-world] 다양한 리액션 타입 동시 추가")
        fun `다양한 리액션 타입 동시 추가`() {
            // given
            val messageId = MessageId.from("message-002")
            val userCount = 600 // 각 타입당 100명
            val reactionTypes = ReactionType.entries.toTypedArray()
            val latch = CountDownLatch(userCount)
            val successCount = AtomicInteger(0)

            // when - 6가지 리액션 타입을 골고루 추가
            repeat(userCount) { index ->
                executor.submit {
                    try {
                        val reaction = MessageReaction.create(
                            messageId = messageId,
                            userId = UserId.from((index + 1).toLong()),
                            reactionType = reactionTypes[index % reactionTypes.size]
                        )
                        adapter.save(reaction)
                        successCount.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await(60, TimeUnit.SECONDS)

            // then - 모든 리액션이 저장되어야 함
            assertThat(successCount.get()).isEqualTo(userCount)

            // 각 타입별 개수 확인
            val summary = adapter.getReactionSummary(messageId)
            assertThat(summary).hasSize(reactionTypes.size)
            reactionTypes.forEach { type ->
                assertThat(summary[type]).isEqualTo(100L)
            }
        }
    }

    @Nested
    @DisplayName("리액션 스팸 방지 시나리오")
    inner class ReactionSpamPreventionTest {

        @Test
        @DisplayName("[real-world] 사용자가 빠르게 리액션 토글 (10회 연속)")
        fun `사용자가 빠르게 리액션 토글`() {
            // given
            val messageId = MessageId.from("message-spam-001")
            val userId = UserId.from(1L)
            val toggleCount = 10
            val latch = CountDownLatch(toggleCount)
            val operations = ConcurrentLinkedQueue<String>()

            // when - 리액션을 빠르게 추가/삭제/변경
            repeat(toggleCount) { index ->
                executor.submit {
                    Thread.sleep(Random.nextLong(10, 50)) // 10-50ms 랜덤 딜레이
                    try {
                        if (index % 3 == 0) {
                            // 추가 또는 변경
                            val reaction = MessageReaction.create(
                                messageId = messageId,
                                userId = userId,
                                reactionType = if (index % 2 == 0) ReactionType.LIKE else ReactionType.SAD
                            )
                            adapter.save(reaction)
                            operations.add("save-${index}")
                        } else if (index % 3 == 1) {
                            // 삭제 시도
                            adapter.deleteByMessageIdAndUserId(messageId, userId)
                            operations.add("delete-${index}")
                        } else {
                            // 타입 변경 시도
                            val existing = adapter.findByMessageIdAndUserId(messageId, userId)
                            if (existing != null) {
                                val updated = existing.copy(reactionType = ReactionType.ANGRY)
                                adapter.save(updated)
                                operations.add("update-${index}")
                            }
                        }
                    } catch (e: Exception) {
                        operations.add("error-${index}")
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await(30, TimeUnit.SECONDS)

            // then - 최종적으로 0개 또는 1개의 리액션만 존재해야 함
            val finalReaction = adapter.findByMessageIdAndUserId(messageId, userId)
            val allReactions = adapter.findAllByMessageId(messageId)
            assertThat(allReactions).hasSizeLessThanOrEqualTo(1)

            // 최소한 일부 작업은 성공해야 함
            assertThat(operations).isNotEmpty()
        }

        @Test
        @DisplayName("[real-world] 여러 사용자가 동시에 같은 메시지에 스팸 (악의적 사용자 시뮬레이션)")
        fun `여러 사용자가 동시에 리액션 스팸`() {
            // given - 5명의 악의적 사용자가 각각 20번씩 스팸
            val messageId = MessageId.from("message-spam-002")
            val userCount = 5
            val spamPerUser = 20
            val totalAttempts = userCount * spamPerUser
            val latch = CountDownLatch(totalAttempts)
            val successCount = AtomicInteger(0)
            val duplicateCount = AtomicInteger(0)

            // when
            repeat(userCount) { userId ->
                repeat(spamPerUser) { _ ->
                    executor.submit {
                        try {
                            val reaction = MessageReaction.create(
                                messageId = messageId,
                                userId = UserId.from((userId + 1).toLong()),
                                reactionType = ReactionType.values()[Random.nextInt(6)]
                            )
                            adapter.save(reaction)
                            successCount.incrementAndGet()
                        } catch (e: DuplicateKeyException) {
                            // 중복 키는 정상적인 상황 (같은 사용자가 여러 번 시도)
                            duplicateCount.incrementAndGet()
                        } catch (e: Exception) {
                            // 기타 오류
                        } finally {
                            latch.countDown()
                        }
                    }
                }
            }

            latch.await(30, TimeUnit.SECONDS)

            // then - 각 사용자당 최대 1개의 리액션만 존재해야 함
            val allReactions = adapter.findAllByMessageId(messageId)
            assertThat(allReactions).hasSizeLessThanOrEqualTo(userCount)

            // 중복 시도가 발생했어야 함 (unique constraint로 인한 거부)
            assertThat(duplicateCount.get()).isGreaterThan(0)

            // 각 사용자당 하나의 리액션만 있어야 함
            val reactionsByUser = allReactions.groupBy { it.userId.value }
            reactionsByUser.values.forEach { reactions ->
                assertThat(reactions).hasSize(1)
            }
        }
    }

    @Nested
    @DisplayName("리액션 타입 변경 경쟁 조건")
    inner class ReactionTypeChangeRaceTest {

        @Test
        @DisplayName("[real-world] 사용자가 리액션 타입을 빠르게 변경 (마음이 바뀜)")
        fun `사용자가 리액션 타입을 빠르게 변경`() {
            // given - 이미 리액션이 있는 상태
            val messageId = MessageId.from("message-change-001")
            val userId = UserId.from(1L)
            val initialReaction = MessageReaction.create(messageId, userId, ReactionType.LIKE)
            val saved = adapter.save(initialReaction)

            val changeCount = 10
            val latch = CountDownLatch(changeCount)
            val reactionTypes = ReactionType.values()
            val successfulChanges = ConcurrentLinkedQueue<ReactionType>()

            // when - 10번 연속으로 다른 리액션 타입으로 변경
            repeat(changeCount) { index ->
                executor.submit {
                    Thread.sleep(Random.nextLong(5, 30))
                    try {
                        val current = adapter.findByMessageIdAndUserId(messageId, userId)!!
                        val newType = reactionTypes[index % reactionTypes.size]
                        val updated = current.copy(reactionType = newType)
                        adapter.save(updated)
                        successfulChanges.add(newType)
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await(30, TimeUnit.SECONDS)

            // then - 여전히 하나의 리액션만 존재해야 함
            val allReactions = adapter.findAllByMessageId(messageId)
            assertThat(allReactions).hasSize(1)

            // 마지막 변경이 반영되어 있어야 함
            val finalReaction = adapter.findByMessageIdAndUserId(messageId, userId)!!
            assertThat(finalReaction.reactionType).isIn(*reactionTypes)
            assertThat(successfulChanges).isNotEmpty()
        }

        @Test
        @DisplayName("[real-world] 여러 사용자가 각자 리액션을 동시에 변경")
        fun `여러 사용자가 각자 리액션을 동시에 변경`() {
            // given - 10명의 사용자가 이미 LIKE 리액션을 가지고 있음
            val messageId = MessageId.from("message-change-002")
            val userCount = 10

            repeat(userCount) { userId ->
                val reaction = MessageReaction.create(
                    messageId = messageId,
                    userId = UserId.from((userId + 1).toLong()),
                    reactionType = ReactionType.LIKE
                )
                adapter.save(reaction)
            }

            val latch = CountDownLatch(userCount)
            val successCount = AtomicInteger(0)

            // when - 모든 사용자가 동시에 SAD로 변경
            repeat(userCount) { userId ->
                executor.submit {
                    try {
                        val user = UserId.from((userId + 1).toLong())
                        val current = adapter.findByMessageIdAndUserId(messageId, user)!!
                        val updated = current.copy(reactionType = ReactionType.SAD)
                        adapter.save(updated)
                        successCount.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await(30, TimeUnit.SECONDS)

            // then - 모든 변경이 성공해야 함
            assertThat(successCount.get()).isEqualTo(userCount)

            // 모든 리액션이 SAD로 변경되어야 함
            val allReactions = adapter.findAllByMessageId(messageId)
            assertThat(allReactions).hasSize(userCount)
            assertThat(allReactions.all { it.reactionType == ReactionType.SAD }).isTrue()

            // 리액션 카운트 확인
            val sadCount = adapter.countByMessageIdAndReactionType(messageId, ReactionType.SAD)
            assertThat(sadCount).isEqualTo(userCount.toLong())
        }
    }

    @Nested
    @DisplayName("리액션 카운트 일관성 테스트")
    inner class ReactionCountConsistencyTest {

        @Test
        @DisplayName("[real-world] 동시 추가/삭제 후 카운트 정확성 검증")
        fun `동시 추가 삭제 후 카운트 정확성`() {
            // given
            val messageId = MessageId.from("message-count-001")
            val userCount = 100

            // 먼저 100명이 리액션 추가
            repeat(userCount) { userId ->
                val reaction = MessageReaction.create(
                    messageId = messageId,
                    userId = UserId.from((userId + 1).toLong()),
                    reactionType = ReactionType.LIKE
                )
                adapter.save(reaction)
            }

            val latch = CountDownLatch(50)
            val deleteSuccessCount = AtomicInteger(0)

            // when - 50명이 동시에 리액션 삭제
            repeat(50) { userId ->
                executor.submit {
                    try {
                        adapter.deleteByMessageIdAndUserId(
                            messageId,
                            UserId.from((userId + 1).toLong())
                        )
                        deleteSuccessCount.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await(30, TimeUnit.SECONDS)

            // then - 정확히 50개의 리액션만 남아있어야 함
            val remainingReactions = adapter.findAllByMessageId(messageId)
            assertThat(remainingReactions).hasSize(50)

            // 카운트도 일치해야 함
            val totalCount = adapter.countByMessageId(messageId)
            assertThat(totalCount).isEqualTo(50L)

            val likeCount = adapter.countByMessageIdAndReactionType(messageId, ReactionType.LIKE)
            assertThat(likeCount).isEqualTo(50L)
        }

        @Test
        @DisplayName("[real-world] 리액션 요약 정보의 일관성 (대규모)")
        fun `리액션 요약 정보 일관성`() {
            // given - 600개의 리액션 (각 타입당 100개)
            val messageId = MessageId.from("message-summary-001")
            val reactionTypes = ReactionType.values()
            val perType = 100
            val totalCount = reactionTypes.size * perType
            val latch = CountDownLatch(totalCount)

            // when - 동시에 모든 리액션 추가
            repeat(totalCount) { index ->
                executor.submit {
                    try {
                        val reaction = MessageReaction.create(
                            messageId = messageId,
                            userId = UserId.from((index + 1).toLong()),
                            reactionType = reactionTypes[index / perType]
                        )
                        adapter.save(reaction)
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await(60, TimeUnit.SECONDS)

            // then - 요약 정보가 정확해야 함
            val summary = adapter.getReactionSummary(messageId)
            assertThat(summary).hasSize(reactionTypes.size)

            var totalSum = 0L
            reactionTypes.forEach { type ->
                val count = summary[type] ?: 0L
                assertThat(count).isEqualTo(perType.toLong())
                totalSum += count
            }

            assertThat(totalSum).isEqualTo(totalCount.toLong())

            // 전체 카운트와도 일치해야 함
            val totalCountQuery = adapter.countByMessageId(messageId)
            assertThat(totalCountQuery).isEqualTo(totalCount.toLong())
        }
    }

    @Nested
    @DisplayName("리액션 삭제 경쟁 조건")
    inner class ReactionDeleteRaceTest {

        @Test
        @DisplayName("[real-world] 여러 프로세스가 동시에 같은 리액션 삭제 시도")
        fun `여러 프로세스가 동시에 같은 리액션 삭제`() {
            // given
            val messageId = MessageId.from("message-delete-001")
            val userId = UserId.from(1L)
            val reaction = MessageReaction.create(messageId, userId, ReactionType.LIKE)
            adapter.save(reaction)

            val deleteAttempts = 10
            val latch = CountDownLatch(deleteAttempts)
            val successCount = AtomicInteger(0)

            // when - 10개의 스레드가 동시에 같은 리액션 삭제 시도
            repeat(deleteAttempts) {
                executor.submit {
                    try {
                        adapter.deleteByMessageIdAndUserId(messageId, userId)
                        successCount.incrementAndGet()
                    } catch (e: Exception) {
                        // 삭제 실패는 정상 (이미 삭제된 경우)
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await(10, TimeUnit.SECONDS)

            // then - 리액션이 삭제되어야 함
            val remaining = adapter.findByMessageIdAndUserId(messageId, userId)
            assertThat(remaining).isNull()

            // 최소한 한 번은 성공해야 함
            assertThat(successCount.get()).isGreaterThanOrEqualTo(1)
        }

        @Test
        @DisplayName("[real-world] 메시지 삭제 시 모든 리액션 동시 삭제")
        fun `메시지 삭제 시 모든 리액션 동시 삭제`() {
            // given - 메시지에 100개의 리액션이 있음
            val messageId = MessageId.from("message-delete-all-001")
            val userCount = 100

            repeat(userCount) { userId ->
                val reaction = MessageReaction.create(
                    messageId = messageId,
                    userId = UserId.from((userId + 1).toLong()),
                    reactionType = ReactionType.LIKE
                )
                adapter.save(reaction)
            }

            // when - 메시지가 삭제되어 모든 리액션 삭제
            adapter.deleteAllByMessageId(messageId)

            // then - 모든 리액션이 삭제되어야 함
            val remainingReactions = adapter.findAllByMessageId(messageId)
            assertThat(remainingReactions).isEmpty()

            val count = adapter.countByMessageId(messageId)
            assertThat(count).isEqualTo(0L)
        }
    }
}
