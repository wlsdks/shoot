package com.stark.shoot.application.service.saga.friend

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.application.port.out.AlertNotificationPort
import com.stark.shoot.application.port.out.DeadLetterQueuePort
import com.stark.shoot.application.service.saga.friend.steps.AcceptFriendRequestStep
import com.stark.shoot.application.service.saga.friend.steps.CreateFriendshipsStep
import com.stark.shoot.application.service.saga.friend.steps.PublishFriendEventsStep
import com.stark.shoot.domain.saga.SagaState
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.domain.shared.event.FriendAddedEvent
import com.stark.shoot.domain.social.Friendship
import com.stark.shoot.domain.social.FriendshipPair
import com.stark.shoot.domain.social.type.FriendRequestStatus
import jakarta.persistence.OptimisticLockException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*

@DisplayName("친구 요청 Saga 오케스트레이터 테스트")
class FriendRequestSagaOrchestratorTest {

    // Kotlin에서 Mockito any() 사용을 위한 헬퍼 함수
    private fun <T> any(): T {
        org.mockito.ArgumentMatchers.any<T>()
        @Suppress("UNCHECKED_CAST")
        return null as T
    }

    private lateinit var acceptFriendRequestStep: AcceptFriendRequestStep
    private lateinit var createFriendshipsStep: CreateFriendshipsStep
    private lateinit var publishFriendEventsStep: PublishFriendEventsStep
    private lateinit var deadLetterQueuePort: DeadLetterQueuePort
    private lateinit var alertNotificationPort: AlertNotificationPort
    private lateinit var objectMapper: ObjectMapper
    private lateinit var orchestrator: FriendRequestSagaOrchestrator

    private val requesterId = UserId.from(1L)
    private val receiverId = UserId.from(2L)

    @BeforeEach
    fun setUp() {
        acceptFriendRequestStep = mock(AcceptFriendRequestStep::class.java)
        createFriendshipsStep = mock(CreateFriendshipsStep::class.java)
        publishFriendEventsStep = mock(PublishFriendEventsStep::class.java)
        deadLetterQueuePort = mock(DeadLetterQueuePort::class.java)
        alertNotificationPort = mock(AlertNotificationPort::class.java)
        objectMapper = ObjectMapper()

        orchestrator = FriendRequestSagaOrchestrator(
            acceptFriendRequestStep = acceptFriendRequestStep,
            createFriendshipsStep = createFriendshipsStep,
            publishFriendEventsStep = publishFriendEventsStep,
            deadLetterQueuePort = deadLetterQueuePort,
            alertNotificationPort = alertNotificationPort,
            objectMapper = objectMapper
        )

        // Mock step names
        `when`(acceptFriendRequestStep.stepName()).thenReturn("AcceptFriendRequest")
        `when`(createFriendshipsStep.stepName()).thenReturn("CreateFriendships")
        `when`(publishFriendEventsStep.stepName()).thenReturn("PublishFriendEvents")
    }

    @Nested
    @DisplayName("정상 시나리오")
    inner class SuccessScenario {

        @Test
        @DisplayName("[happy] 모든 Step이 성공하면 Saga가 성공적으로 완료된다")
        fun `모든 Step이 성공하면 Saga가 성공적으로 완료된다`() {
            // Given: 모든 Step이 성공하도록 설정
            doAnswer { invocation ->
                val context = invocation.getArgument<FriendRequestSagaContext>(0)

                // Step 1: FriendshipPair 생성
                val friendship1 = Friendship.create(userId = receiverId, friendId = requesterId)
                val friendship2 = Friendship.create(userId = requesterId, friendId = receiverId)
                val events = listOf(
                    FriendAddedEvent.create(userId = receiverId, friendId = requesterId),
                    FriendAddedEvent.create(userId = requesterId, friendId = receiverId)
                )
                context.friendshipPair = FriendshipPair(friendship1, friendship2, events)

                // Snapshot 저장
                context.friendRequestSnapshot = FriendRequestSagaContext.FriendRequestSnapshot(
                    requesterId = requesterId.value,
                    receiverId = receiverId.value,
                    previousStatus = FriendRequestStatus.PENDING,
                    previousRespondedAt = null
                )

                context.recordStep("AcceptFriendRequest")
                true
            }.`when`(acceptFriendRequestStep).execute(any())

            doAnswer { invocation ->
                val context = invocation.getArgument<FriendRequestSagaContext>(0)
                context.recordStep("CreateFriendships")
                true
            }.`when`(createFriendshipsStep).execute(any())

            doAnswer { invocation ->
                val context = invocation.getArgument<FriendRequestSagaContext>(0)
                context.recordStep("PublishFriendEvents")
                true
            }.`when`(publishFriendEventsStep).execute(any())

            // When
            val result = orchestrator.execute(requesterId, receiverId)

            // Then
            assertThat(result.state).isEqualTo(SagaState.COMPLETED)
            assertThat(result.error).isNull()
            assertThat(result.executedSteps).containsExactly(
                "AcceptFriendRequest",
                "CreateFriendships",
                "PublishFriendEvents"
            )

            // Verify: 모든 Step이 실행됨
            verify(acceptFriendRequestStep).execute(any())
            verify(createFriendshipsStep).execute(any())
            verify(publishFriendEventsStep).execute(any())

            // Verify: 보상이 실행되지 않음
            verify(acceptFriendRequestStep, never()).compensate(any())
            verify(createFriendshipsStep, never()).compensate(any())
            verify(publishFriendEventsStep, never()).compensate(any())

            // Verify: FriendshipPair 생성 확인
            assertThat(result.friendshipPair).isNotNull
            assertThat(result.friendshipPair!!.getAllFriendships()).hasSize(2)
            assertThat(result.friendshipPair!!.events).hasSize(2)
        }
    }

    @Nested
    @DisplayName("보상 트랜잭션")
    inner class CompensationScenario {

        @Test
        @DisplayName("[happy] Step 2 실패 시 Step 1이 보상된다")
        fun `Step 2 실패 시 Step 1이 보상된다`() {
            // Given: Step 1 성공, Step 2 실패
            doAnswer { invocation ->
                val context = invocation.getArgument<FriendRequestSagaContext>(0)

                // FriendshipPair 생성
                val friendship1 = Friendship.create(userId = receiverId, friendId = requesterId)
                val friendship2 = Friendship.create(userId = requesterId, friendId = receiverId)
                val events = listOf(
                    FriendAddedEvent.create(userId = receiverId, friendId = requesterId),
                    FriendAddedEvent.create(userId = requesterId, friendId = receiverId)
                )
                context.friendshipPair = FriendshipPair(friendship1, friendship2, events)

                // Snapshot 저장
                context.friendRequestSnapshot = FriendRequestSagaContext.FriendRequestSnapshot(
                    requesterId = requesterId.value,
                    receiverId = receiverId.value,
                    previousStatus = FriendRequestStatus.PENDING,
                    previousRespondedAt = null
                )

                context.recordStep("AcceptFriendRequest")
                true
            }.`when`(acceptFriendRequestStep).execute(any())

            doAnswer { invocation ->
                val context = invocation.getArgument<FriendRequestSagaContext>(0)
                context.markFailed(RuntimeException("Friendship creation failed"))
                false
            }.`when`(createFriendshipsStep).execute(any())

            `when`(acceptFriendRequestStep.compensate(any())).thenReturn(true)

            // When
            val result = orchestrator.execute(requesterId, receiverId)

            // Then
            assertThat(result.state).isEqualTo(SagaState.COMPENSATED)
            assertThat(result.error).isNotNull
            assertThat(result.error!!.message).isEqualTo("Friendship creation failed")

            // Verify: Step 1만 실행됨
            verify(acceptFriendRequestStep).execute(any())
            verify(createFriendshipsStep).execute(any())
            verify(publishFriendEventsStep, never()).execute(any())

            // Verify: Step 1만 보상됨
            verify(acceptFriendRequestStep).compensate(any())
            verify(createFriendshipsStep, never()).compensate(any())
        }

        @Test
        @DisplayName("[happy] Step 3 실패 시 Step 2, 1이 모두 보상된다")
        fun `Step 3 실패 시 Step 2, 1이 모두 보상된다`() {
            // Given: Step 1, 2 성공, Step 3 실패
            doAnswer { invocation ->
                val context = invocation.getArgument<FriendRequestSagaContext>(0)

                // FriendshipPair 생성
                val friendship1 = Friendship.create(userId = receiverId, friendId = requesterId)
                val friendship2 = Friendship.create(userId = requesterId, friendId = receiverId)
                val events = listOf(
                    FriendAddedEvent.create(userId = receiverId, friendId = requesterId),
                    FriendAddedEvent.create(userId = requesterId, friendId = receiverId)
                )
                context.friendshipPair = FriendshipPair(friendship1, friendship2, events)

                // Snapshot 저장
                context.friendRequestSnapshot = FriendRequestSagaContext.FriendRequestSnapshot(
                    requesterId = requesterId.value,
                    receiverId = receiverId.value,
                    previousStatus = FriendRequestStatus.PENDING,
                    previousRespondedAt = null
                )

                context.recordStep("AcceptFriendRequest")
                true
            }.`when`(acceptFriendRequestStep).execute(any())

            doAnswer { invocation ->
                val context = invocation.getArgument<FriendRequestSagaContext>(0)
                context.recordStep("CreateFriendships")
                true
            }.`when`(createFriendshipsStep).execute(any())

            doAnswer { invocation ->
                val context = invocation.getArgument<FriendRequestSagaContext>(0)
                context.markFailed(RuntimeException("Event publishing failed"))
                false
            }.`when`(publishFriendEventsStep).execute(any())

            `when`(createFriendshipsStep.compensate(any())).thenReturn(true)
            `when`(acceptFriendRequestStep.compensate(any())).thenReturn(true)

            // When
            val result = orchestrator.execute(requesterId, receiverId)

            // Then
            assertThat(result.state).isEqualTo(SagaState.COMPENSATED)
            assertThat(result.error).isNotNull

            // Verify: Step 1, 2, 3 모두 실행됨
            verify(acceptFriendRequestStep).execute(any())
            verify(createFriendshipsStep).execute(any())
            verify(publishFriendEventsStep).execute(any())

            // Verify: Step 2, 1 순서로 보상됨 (역순)
            val inOrder = inOrder(createFriendshipsStep, acceptFriendRequestStep)
            inOrder.verify(createFriendshipsStep).compensate(any())
            inOrder.verify(acceptFriendRequestStep).compensate(any())
        }

        @Test
        @DisplayName("[bad] 보상 실패 시 DLQ와 알림이 전송된다")
        fun `보상 실패 시 DLQ와 알림이 전송된다`() {
            // Given: Step 2 실패, Step 1 보상 실패
            doAnswer { invocation ->
                val context = invocation.getArgument<FriendRequestSagaContext>(0)

                // FriendshipPair 생성
                val friendship1 = Friendship.create(userId = receiverId, friendId = requesterId)
                val friendship2 = Friendship.create(userId = requesterId, friendId = receiverId)
                val events = listOf(
                    FriendAddedEvent.create(userId = receiverId, friendId = requesterId),
                    FriendAddedEvent.create(userId = requesterId, friendId = receiverId)
                )
                context.friendshipPair = FriendshipPair(friendship1, friendship2, events)

                // Snapshot 저장
                context.friendRequestSnapshot = FriendRequestSagaContext.FriendRequestSnapshot(
                    requesterId = requesterId.value,
                    receiverId = receiverId.value,
                    previousStatus = FriendRequestStatus.PENDING,
                    previousRespondedAt = null
                )

                context.recordStep("AcceptFriendRequest")
                true
            }.`when`(acceptFriendRequestStep).execute(any())

            doAnswer { invocation ->
                val context = invocation.getArgument<FriendRequestSagaContext>(0)
                context.markFailed(RuntimeException("Step 2 failed"))
                false
            }.`when`(createFriendshipsStep).execute(any())

            `when`(acceptFriendRequestStep.compensate(any())).thenReturn(false)  // 보상 실패

            // When
            val result = orchestrator.execute(requesterId, receiverId)

            // Then
            assertThat(result.state).isEqualTo(SagaState.FAILED)

            // Verify: DLQ 발행
            verify(deadLetterQueuePort).publish(any())

            // Verify: Critical 알림 전송
            verify(alertNotificationPort).sendCriticalAlert(any())
        }
    }

    @Nested
    @DisplayName("OptimisticLockException 재시도")
    inner class RetryScenario {

        @Test
        @DisplayName("[happy] OptimisticLockException 발생 시 재시도한다")
        fun `OptimisticLockException 발생 시 재시도한다`() {
            // Given: 첫 번째 시도는 OptimisticLockException, 두 번째 시도는 성공
            var attemptCount = 0

            doAnswer { invocation ->
                val context = invocation.getArgument<FriendRequestSagaContext>(0)
                attemptCount++

                if (attemptCount == 1) {
                    // 첫 번째 시도: OptimisticLockException
                    context.markFailed(OptimisticLockException("Version mismatch"))
                    false
                } else {
                    // 두 번째 시도: 성공
                    val friendship1 = Friendship.create(userId = receiverId, friendId = requesterId)
                    val friendship2 = Friendship.create(userId = requesterId, friendId = receiverId)
                    val events = listOf(
                        FriendAddedEvent.create(userId = receiverId, friendId = requesterId),
                        FriendAddedEvent.create(userId = requesterId, friendId = receiverId)
                    )
                    context.friendshipPair = FriendshipPair(friendship1, friendship2, events)

                    context.friendRequestSnapshot = FriendRequestSagaContext.FriendRequestSnapshot(
                        requesterId = requesterId.value,
                        receiverId = receiverId.value,
                        previousStatus = FriendRequestStatus.PENDING,
                        previousRespondedAt = null
                    )

                    context.recordStep("AcceptFriendRequest")
                    true
                }
            }.`when`(acceptFriendRequestStep).execute(any())

            doAnswer { invocation ->
                val context = invocation.getArgument<FriendRequestSagaContext>(0)
                context.recordStep("CreateFriendships")
                true
            }.`when`(createFriendshipsStep).execute(any())

            doAnswer { invocation ->
                val context = invocation.getArgument<FriendRequestSagaContext>(0)
                context.recordStep("PublishFriendEvents")
                true
            }.`when`(publishFriendEventsStep).execute(any())

            // When
            val result = orchestrator.execute(requesterId, receiverId)

            // Then
            assertThat(result.state).isEqualTo(SagaState.COMPLETED)
            assertThat(attemptCount).isEqualTo(2)

            // Verify: Step 1이 2번 실행됨
            verify(acceptFriendRequestStep, times(2)).execute(any())
        }

        @Test
        @DisplayName("[bad] 최대 재시도 횟수 초과 시 실패한다")
        fun `최대 재시도 횟수 초과 시 실패한다`() {
            // Given: 모든 시도가 OptimisticLockException
            doAnswer { invocation ->
                val context = invocation.getArgument<FriendRequestSagaContext>(0)
                context.markFailed(OptimisticLockException("Version mismatch"))
                false
            }.`when`(acceptFriendRequestStep).execute(any())

            // When
            val result = orchestrator.execute(requesterId, receiverId)

            // Then
            assertThat(result.state).isEqualTo(SagaState.FAILED)
            assertThat(result.error).isInstanceOf(OptimisticLockException::class.java)

            // Verify: Step 1이 3번 실행됨 (최대 재시도 횟수)
            verify(acceptFriendRequestStep, times(3)).execute(any())

            // Verify: Step 2, 3은 실행되지 않음
            verify(createFriendshipsStep, never()).execute(any())
            verify(publishFriendEventsStep, never()).execute(any())
        }
    }
}
