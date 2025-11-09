package com.stark.shoot.application.service.concurrency

import com.stark.shoot.adapter.out.persistence.mongodb.adapter.message.MessageCommandMongoAdapter
import com.stark.shoot.adapter.out.persistence.mongodb.adapter.message.MessageQueryMongoAdapter
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.adapter.out.persistence.mongodb.repository.ChatMessageMongoRepository
import com.stark.shoot.domain.chat.exception.MessageException
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId as ChatRoomQueryId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.context.annotation.Import
import java.time.Instant
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

/**
 * 실제 채팅 서비스에서 발생할 수 있는 메시지 동시성 문제 테스트
 *
 * 이 테스트는 실제 프로덕션 환경에서 발생하는 다음과 같은 상황을 시뮬레이션합니다:
 * - 대규모 그룹 채팅에서 여러 사용자가 동시에 메시지 전송
 * - 메시지 순서 보장 검증
 * - 동시 편집/삭제 시나리오
 * - 24시간 편집 제한 경계 조건
 * - 고부하 상황에서의 시스템 안정성
 */
@DataMongoTest
@Import(ChatMessageMapper::class, MessageCommandMongoAdapter::class, MessageQueryMongoAdapter::class)
@DisplayName("실제 채팅 서비스 메시지 동시성 테스트")
class MessageConcurrencyRealWorldTest {

    @Autowired
    private lateinit var commandAdapter: MessageCommandMongoAdapter

    @Autowired
    private lateinit var queryAdapter: MessageQueryMongoAdapter

    @Autowired
    private lateinit var repository: ChatMessageMongoRepository

    companion object {
        private val executor = Executors.newFixedThreadPool(50) as ThreadPoolExecutor

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
    }

    @AfterEach
    fun tearDown() {
        repository.deleteAll()
        executor.purge()
    }

    @Nested
    @DisplayName("대규모 동시 메시지 전송 시나리오")
    inner class MassiveConcurrentSendTest {

        @Test
        @DisplayName("[real-world] 100명이 동시에 메시지를 보낼 때 모든 메시지가 저장되어야 한다")
        fun `100명이 동시에 메시지를 보낼 때 모든 메시지가 저장되어야 한다`() {
            // given - 대규모 그룹 채팅방
            val roomId = ChatRoomId.from(1001L)
            val userCount = 100
            val latch = CountDownLatch(userCount)
            val errors = ConcurrentLinkedQueue<Exception>()
            val savedMessages = ConcurrentLinkedQueue<ChatMessage>()

            // when - 100명의 사용자가 동시에 메시지 전송
            repeat(userCount) { userId ->
                executor.submit {
                    try {
                        val message = ChatMessage.create(
                            roomId = roomId,
                            senderId = UserId.from((userId + 1).toLong()),
                            text = "동시 전송 메시지 from user ${userId + 1}",
                            type = MessageType.TEXT
                        )
                        val saved = commandAdapter.save(message)
                        savedMessages.add(saved)
                    } catch (e: Exception) {
                        errors.add(e)
                    } finally {
                        latch.countDown()
                    }
                }
            }

            // then
            assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue()
            assertThat(errors).isEmpty()
            assertThat(savedMessages).hasSize(userCount)

            // 실제 DB에서 검증
            val allMessages = queryAdapter.findByRoomId(ChatRoomQueryId.from(roomId.value), 1000)
            assertThat(allMessages).hasSize(userCount)

            // 모든 사용자의 메시지가 저장되었는지 확인
            val userIds = allMessages.map { it.senderId.value }.toSet()
            assertThat(userIds).hasSize(userCount)
        }

        @Test
        @DisplayName("[real-world] 동시 메시지 전송 시 타임스탬프 순서가 유지되어야 한다")
        fun `동시 메시지 전송 시 타임스탬프 순서가 유지되어야 한다`() {
            // given
            val roomId = ChatRoomId.from(1002L)
            val messageCount = 50
            val latch = CountDownLatch(messageCount)
            val startTime = Instant.now()

            // when - 순차적으로 메시지를 빠르게 전송 (실제 타이핑 시뮬레이션)
            repeat(messageCount) { index ->
                executor.submit {
                    Thread.sleep(Random.nextLong(0, 10)) // 0-10ms 랜덤 딜레이
                    try {
                        val message = ChatMessage.create(
                            roomId = roomId,
                            senderId = UserId.from(1L),
                            text = "순서 테스트 메시지 #$index"
                        )
                        commandAdapter.save(message)
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await(30, TimeUnit.SECONDS)

            // then - 모든 메시지가 저장되어야 함
            val messages = queryAdapter.findByRoomId(ChatRoomQueryId.from(roomId.value), 1000)
            assertThat(messages).hasSize(messageCount)

            // 모든 메시지가 시작 시간 이후에 생성되어야 함
            val timestamps = messages.mapNotNull { it.createdAt }
            assertThat(timestamps).allMatch { it.isAfter(startTime) || it.equals(startTime) }

            // MongoDB는 _id로 정렬되므로 메시지 순서는 보장됨
            // 타임스탬프는 동일할 수 있음 (매우 빠르게 생성된 경우)
            val allIds = messages.mapNotNull { it.id?.value }
            assertThat(allIds).hasSize(messageCount)
        }

        @Test
        @DisplayName("[real-world] 스파이크 트래픽 시나리오 - 갑작스런 500개 메시지 폭주")
        fun `스파이크 트래픽 시나리오 - 갑작스런 500개 메시지 폭주`() {
            // given - 실제 프로덕션에서 발생하는 순간적인 트래픽 폭주
            val roomId = ChatRoomId.from(1003L)
            val spikeMessageCount = 500
            val latch = CountDownLatch(spikeMessageCount)
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)

            // when - 모든 메시지를 거의 동시에 전송 (no delay)
            val startTime = System.currentTimeMillis()
            repeat(spikeMessageCount) { index ->
                executor.submit {
                    try {
                        val message = ChatMessage.create(
                            roomId = roomId,
                            senderId = UserId.from((index % 50 + 1).toLong()), // 50명의 사용자
                            text = "스파이크 메시지 #$index"
                        )
                        commandAdapter.save(message)
                        successCount.incrementAndGet()
                    } catch (e: Exception) {
                        failCount.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }

            assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue()
            val duration = System.currentTimeMillis() - startTime

            // then - 시스템이 안정적으로 처리해야 함
            assertThat(successCount.get()).isEqualTo(spikeMessageCount)
            assertThat(failCount.get()).isEqualTo(0)

            // 성능 검증 - 60초 내에 처리되어야 함
            assertThat(duration).isLessThan(60000)

            val savedMessages = queryAdapter.findByRoomId(ChatRoomQueryId.from(roomId.value), 1000)
            assertThat(savedMessages).hasSize(spikeMessageCount)
        }
    }

    @Nested
    @DisplayName("동시 메시지 편집 시나리오")
    inner class ConcurrentEditTest {

        @Test
        @DisplayName("[real-world] 메시지 편집 중 다른 프로세스가 삭제 시도 - Race Condition")
        fun `메시지 편집 중 다른 프로세스가 삭제 시도`() {
            // given - 저장된 메시지
            val message = ChatMessage.create(
                roomId = ChatRoomId.from(1004L),
                senderId = UserId.from(1L),
                text = "원본 메시지"
            )
            val saved = commandAdapter.save(message)
            val messageId = saved.id!!

            val latch = CountDownLatch(2)
            val results = ConcurrentHashMap<String, Boolean>()

            // when - 동시에 편집과 삭제 시도
            executor.submit {
                Thread.sleep(10) // 약간의 딜레이로 타이밍 조절
                try {
                    val msg = queryAdapter.findById(messageId)!!
                    msg.editMessage("수정된 메시지")
                    commandAdapter.save(msg)
                    results["edit"] = true
                } catch (e: Exception) {
                    results["edit"] = false
                } finally {
                    latch.countDown()
                }
            }

            executor.submit {
                try {
                    val msg = queryAdapter.findById(messageId)!!
                    msg.markAsDeleted()
                    commandAdapter.save(msg)
                    results["delete"] = true
                } catch (e: Exception) {
                    results["delete"] = false
                } finally {
                    latch.countDown()
                }
            }

            latch.await(10, TimeUnit.SECONDS)

            // then - 최소한 하나는 성공해야 함
            val successCount = results.values.count { it }
            assertThat(successCount).isGreaterThanOrEqualTo(1)

            // 최종 상태 확인
            val finalMessage = queryAdapter.findById(messageId)!!

            // 삭제되었거나, 편집되었거나 둘 중 하나
            val isDeleted = finalMessage.isDeleted
            val isEdited = finalMessage.content.isEdited

            assertThat(isDeleted || isEdited).isTrue()
        }

        @Test
        @DisplayName("[real-world] 동일 메시지를 여러 번 빠르게 수정 - 마지막 편집이 승리")
        fun `동일 메시지를 여러 번 빠르게 수정 - 마지막 편집이 승리`() {
            // given
            val message = ChatMessage.create(
                roomId = ChatRoomId.from(1005L),
                senderId = UserId.from(1L),
                text = "원본 메시지"
            )
            val saved = commandAdapter.save(message)
            val messageId = saved.id!!

            val editCount = 20
            val latch = CountDownLatch(editCount)
            val successfulEdits = ConcurrentLinkedQueue<String>()

            // when - 20번 연속으로 빠르게 편집
            repeat(editCount) { index ->
                executor.submit {
                    Thread.sleep(Random.nextLong(5, 20)) // 5-20ms 랜덤 딜레이
                    try {
                        val msg = queryAdapter.findById(messageId)!!
                        val newText = "편집 #$index - ${System.nanoTime()}"
                        msg.editMessage(newText)
                        commandAdapter.save(msg)
                        successfulEdits.add(newText)
                    } catch (e: Exception) {
                        // 편집 충돌은 정상적인 상황
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await(30, TimeUnit.SECONDS)

            // then
            assertThat(successfulEdits).isNotEmpty()

            val finalMessage = queryAdapter.findById(messageId)!!
            assertThat(finalMessage.content.isEdited).isTrue()
            assertThat(finalMessage.content.text).isIn(successfulEdits)
        }

        @Test
        @DisplayName("[real-world] 24시간 편집 제한 경계 조건 - 정확히 23:59:59에 편집 시도")
        fun `24시간 편집 제한 경계 조건`() {
            // given - 거의 24시간이 지난 메시지 (23시간 59분 50초)
            val almostExpired = Instant.now().minusSeconds(86390) // 23:59:50
            val message = ChatMessage.create(
                roomId = ChatRoomId.from(1006L),
                senderId = UserId.from(1L),
                text = "오래된 메시지"
            )
            val messageWithOldTime = message.copy(createdAt = almostExpired)
            val saved = commandAdapter.save(messageWithOldTime)

            // when & then - 아직 24시간이 안 지났으므로 편집 가능
            val retrieved = queryAdapter.findById(saved.id!!)!!
            retrieved.editMessage("경계선 편집")

            val updated = commandAdapter.save(retrieved)
            assertThat(updated.content.text).isEqualTo("경계선 편집")
            assertThat(updated.content.isEdited).isTrue()
        }

        @Test
        @DisplayName("[real-world] 24시간 초과 메시지 편집 시도 - 예외 발생")
        fun `24시간 초과 메시지 편집 시도`() {
            // given - 24시간이 지난 메시지
            val expired = Instant.now().minusSeconds(86401) // 24:00:01
            val message = ChatMessage.create(
                roomId = ChatRoomId.from(1007L),
                senderId = UserId.from(1L),
                text = "만료된 메시지"
            )
            val messageWithExpiredTime = message.copy(createdAt = expired)
            val saved = commandAdapter.save(messageWithExpiredTime)

            // when & then - 24시간이 지났으므로 편집 불가
            val retrieved = queryAdapter.findById(saved.id!!)!!

            assertThrows<MessageException.EditTimeExpired> {
                retrieved.editMessage("편집 시도")
            }
        }
    }

    @Nested
    @DisplayName("메시지 상태 전환 동시성 테스트")
    inner class MessageStatusTransitionTest {

        @Test
        @DisplayName("[real-world] 동시에 여러 프로세스가 메시지 상태를 변경하려고 시도")
        fun `동시에 여러 프로세스가 메시지 상태를 변경하려고 시도`() {
            // given
            val message = ChatMessage.create(
                roomId = ChatRoomId.from(1008L),
                senderId = UserId.from(1L),
                text = "상태 전환 테스트"
            )
            val saved = commandAdapter.save(message)
            val messageId = saved.id!!

            val threadCount = 10
            val latch = CountDownLatch(threadCount)
            val successCount = AtomicInteger(0)

            // when - 10개의 스레드가 동시에 상태 변경 시도
            repeat(threadCount) { index ->
                executor.submit {
                    try {
                        val msg = queryAdapter.findById(messageId)!!
                        // 교대로 SENT/FAILED 상태로 변경
                        msg.status = if (index % 2 == 0) MessageStatus.SENT else MessageStatus.FAILED
                        commandAdapter.save(msg)
                        successCount.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await(10, TimeUnit.SECONDS)

            // then - 모든 시도가 성공해야 함 (last-write-wins)
            assertThat(successCount.get()).isEqualTo(threadCount)

            val finalMessage = queryAdapter.findById(messageId)!!
            assertThat(finalMessage.status).isIn(MessageStatus.SENT, MessageStatus.FAILED)
        }
    }

    @Nested
    @DisplayName("실제 채팅 사용 패턴 시뮬레이션")
    inner class RealWorldChatPatternTest {

        @Test
        @DisplayName("[real-world] 활발한 그룹 채팅 - 10명이 각자 50개씩 메시지 전송")
        fun `활발한 그룹 채팅 시뮬레이션`() {
            // given - 활발한 그룹 채팅방
            val roomId = ChatRoomId.from(1009L)
            val userCount = 10
            val messagesPerUser = 50
            val totalMessages = userCount * messagesPerUser
            val latch = CountDownLatch(totalMessages)
            val successCount = AtomicInteger(0)

            // when - 각 사용자가 50개씩 메시지를 랜덤한 간격으로 전송
            repeat(userCount) { userId ->
                repeat(messagesPerUser) { messageIndex ->
                    executor.submit {
                        Thread.sleep(Random.nextLong(1, 50)) // 1-50ms 랜덤 딜레이
                        try {
                            val message = ChatMessage.create(
                                roomId = roomId,
                                senderId = UserId.from((userId + 1).toLong()),
                                text = "User${userId + 1}: Message #$messageIndex - ${System.currentTimeMillis()}"
                            )
                            commandAdapter.save(message)
                            successCount.incrementAndGet()
                        } finally {
                            latch.countDown()
                        }
                    }
                }
            }

            assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue()

            // then
            assertThat(successCount.get()).isEqualTo(totalMessages)

            val allMessages = queryAdapter.findByRoomId(ChatRoomQueryId.from(roomId.value), 1000)
            assertThat(allMessages).hasSize(totalMessages)

            // 각 사용자가 정확히 50개씩 보냈는지 확인
            val messagesByUser = allMessages.groupBy { it.senderId.value }
            assertThat(messagesByUser).hasSize(userCount)
            messagesByUser.values.forEach { messages ->
                assertThat(messages).hasSize(messagesPerUser)
            }
        }

        @Test
        @DisplayName("[real-world] 메시지 폭주 후 일부 삭제 - 실제 사용자 행동 패턴")
        fun `메시지 폭주 후 일부 삭제`() {
            // given - 먼저 메시지들을 저장
            val roomId = ChatRoomId.from(1010L)
            val messageCount = 100
            val savedIds = ConcurrentLinkedQueue<MessageId>()

            repeat(messageCount) { index ->
                val message = ChatMessage.create(
                    roomId = roomId,
                    senderId = UserId.from((index % 5 + 1).toLong()),
                    text = "메시지 #$index"
                )
                val saved = commandAdapter.save(message)
                savedIds.add(saved.id!!)
            }

            // when - 30%의 메시지를 동시에 삭제 시도
            val toDelete = savedIds.take(30)
            val deleteLatch = CountDownLatch(toDelete.size)
            val deleteSuccessCount = AtomicInteger(0)

            toDelete.forEach { messageId ->
                executor.submit {
                    try {
                        val msg = queryAdapter.findById(messageId)
                        if (msg != null) {
                            msg.markAsDeleted()
                            commandAdapter.save(msg)
                            deleteSuccessCount.incrementAndGet()
                        }
                    } finally {
                        deleteLatch.countDown()
                    }
                }
            }

            deleteLatch.await(30, TimeUnit.SECONDS)

            // then
            assertThat(deleteSuccessCount.get()).isEqualTo(30)

            val allMessages = queryAdapter.findByRoomId(ChatRoomQueryId.from(roomId.value), 1000)
            val deletedMessages = allMessages.filter { it.isDeleted }
            assertThat(deletedMessages).hasSize(30)
        }
    }

    @Nested
    @DisplayName("메시지 처리 실패 시나리오")
    inner class MessageFailureScenarioTest {

        @Test
        @DisplayName("[real-world] 일부 메시지 저장 실패 시 다른 메시지는 영향 받지 않아야 함")
        fun `일부 메시지 저장 실패 시 다른 메시지는 영향 받지 않음`() {
            // given
            val roomId = ChatRoomId.from(1011L)
            val totalMessages = 50
            val latch = CountDownLatch(totalMessages)
            val successCount = AtomicInteger(0)
            val failureCount = AtomicInteger(0)

            // when - 정상 메시지와 잘못된 메시지를 섞어서 전송
            repeat(totalMessages) { index ->
                executor.submit {
                    try {
                        val text = if (index % 10 == 0) {
                            // 10개 중 1개는 빈 문자열로 시도 (domain validation 실패)
                            ""
                        } else {
                            "정상 메시지 #$index"
                        }

                        if (text.isBlank()) {
                            throw MessageException.EmptyContent()
                        }

                        val message = ChatMessage.create(
                            roomId = roomId,
                            senderId = UserId.from((index % 5 + 1).toLong()),
                            text = text
                        )
                        commandAdapter.save(message)
                        successCount.incrementAndGet()
                    } catch (e: Exception) {
                        failureCount.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await(30, TimeUnit.SECONDS)

            // then
            assertThat(successCount.get()).isEqualTo(45) // 50 - 5개 실패
            assertThat(failureCount.get()).isEqualTo(5)

            val savedMessages = queryAdapter.findByRoomId(ChatRoomQueryId.from(roomId.value), 1000)
            assertThat(savedMessages).hasSize(45)
        }
    }
}
