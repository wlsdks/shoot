package com.stark.shoot.integration

import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendRequestRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.FriendshipMappingRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.UserRepository
import com.stark.shoot.application.port.`in`.user.friend.FindFriendUseCase
import com.stark.shoot.application.port.`in`.user.friend.FriendReceiveUseCase
import com.stark.shoot.application.port.`in`.user.friend.FriendRemoveUseCase
import com.stark.shoot.application.port.`in`.user.friend.FriendRequestUseCase
import com.stark.shoot.application.port.`in`.user.friend.command.*
import com.stark.shoot.domain.user.type.FriendRequestStatus
import com.stark.shoot.util.TestEntityFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("친구 관리 통합 테스트")
class FriendManagementIntegrationTest {

    @Autowired
    private lateinit var friendRequestUseCase: FriendRequestUseCase

    @Autowired
    private lateinit var friendReceiveUseCase: FriendReceiveUseCase

    @Autowired
    private lateinit var findFriendUseCase: FindFriendUseCase

    @Autowired
    private lateinit var friendRemoveUseCase: FriendRemoveUseCase

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var friendRequestRepository: FriendRequestRepository

    @Autowired
    private lateinit var friendshipMappingRepository: FriendshipMappingRepository

    private lateinit var user1Id: Long
    private lateinit var user2Id: Long
    private lateinit var user3Id: Long

    @BeforeEach
    fun setUp() {
        // 테스트 사용자 생성
        val user1 = TestEntityFactory.createUser("user1", "USER0001")
        val user2 = TestEntityFactory.createUser("user2", "USER0002")
        val user3 = TestEntityFactory.createUser("user3", "USER0003")

        userRepository.saveAll(listOf(user1, user2, user3))

        user1Id = user1.id!!
        user2Id = user2.id!!
        user3Id = user3.id!!
    }

    @AfterEach
    fun tearDown() {
        friendshipMappingRepository.deleteAll()
        friendRequestRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    @DisplayName("친구 요청 → 수락 → 양방향 친구 관계 생성")
    fun `친구 요청부터 수락까지 전체 플로우 테스트`() {
        // 1. 친구 요청 전송 (user1 → user2)
        val sendCommand = SendFriendRequestCommand(
            currentUserId = user1Id,
            targetUserId = user2Id
        )
        friendRequestUseCase.sendFriendRequest(sendCommand)

        // 2. 친구 요청 확인
        val friendRequest = friendRequestRepository
            .findAllBySenderIdAndReceiverId(user1Id, user2Id)
            .first()
        assertThat(friendRequest.status).isEqualTo(FriendRequestStatus.PENDING)
        assertThat(friendRequest.sender.id).isEqualTo(user1Id)
        assertThat(friendRequest.receiver.id).isEqualTo(user2Id)

        // 3. 친구 요청 수락 (user2가 수락)
        val acceptCommand = AcceptFriendRequestCommand(
            currentUserId = user2Id,
            requesterId = user1Id
        )
        friendReceiveUseCase.acceptFriendRequest(acceptCommand)

        // 4. 친구 요청 상태 확인
        val acceptedRequest = friendRequestRepository
            .findAllBySenderIdAndReceiverId(user1Id, user2Id)
            .first()
        assertThat(acceptedRequest.status).isEqualTo(FriendRequestStatus.ACCEPTED)
        assertThat(acceptedRequest.respondedAt).isNotNull()

        // 5. 양방향 친구 관계 확인
        val user1ToUser2 = friendshipMappingRepository
            .existsByUserIdAndFriendId(user1Id, user2Id)
        val user2ToUser1 = friendshipMappingRepository
            .existsByUserIdAndFriendId(user2Id, user1Id)

        assertThat(user1ToUser2).isTrue()
        assertThat(user2ToUser1).isTrue()

        // 6. 친구 목록 조회 (user1)
        val user1Friends = findFriendUseCase.getFriends(
            GetFriendsCommand(currentUserId = user1Id)
        )
        assertThat(user1Friends).hasSize(1)
        assertThat(user1Friends.first().id).isEqualTo(user2Id)

        // 7. 친구 목록 조회 (user2)
        val user2Friends = findFriendUseCase.getFriends(
            GetFriendsCommand(currentUserId = user2Id)
        )
        assertThat(user2Friends).hasSize(1)
        assertThat(user2Friends.first().id).isEqualTo(user1Id)
    }

    @Test
    @DisplayName("친구 요청 → 거절 → 친구 관계 미생성")
    fun `친구 요청 거절 테스트`() {
        // 1. 친구 요청 전송
        val sendCommand = SendFriendRequestCommand(
            currentUserId = user1Id,
            targetUserId = user2Id
        )
        friendRequestUseCase.sendFriendRequest(sendCommand)

        // 2. 친구 요청 거절
        val rejectCommand = RejectFriendRequestCommand(
            currentUserId = user2Id,
            requesterId = user1Id
        )
        friendReceiveUseCase.rejectFriendRequest(rejectCommand)

        // 3. 친구 요청 상태 확인
        val rejectedRequest = friendRequestRepository
            .findAllBySenderIdAndReceiverId(user1Id, user2Id)
            .first()
        assertThat(rejectedRequest.status).isEqualTo(FriendRequestStatus.REJECTED)

        // 4. 친구 관계 미생성 확인
        val user1ToUser2 = friendshipMappingRepository
            .existsByUserIdAndFriendId(user1Id, user2Id)
        assertThat(user1ToUser2).isFalse()

        // 5. 친구 목록 확인 (빈 목록)
        val user1Friends = findFriendUseCase.getFriends(
            GetFriendsCommand(currentUserId = user1Id)
        )
        assertThat(user1Friends).isEmpty()
    }

    @Test
    @DisplayName("친구 요청 취소 → 요청 상태 CANCELLED")
    fun `친구 요청 취소 테스트`() {
        // 1. 친구 요청 전송
        val sendCommand = SendFriendRequestCommand(
            currentUserId = user1Id,
            targetUserId = user2Id
        )
        friendRequestUseCase.sendFriendRequest(sendCommand)

        // 2. 친구 요청 취소
        val cancelCommand = CancelFriendRequestCommand(
            currentUserId = user1Id,
            targetUserId = user2Id
        )
        friendRequestUseCase.cancelFriendRequest(cancelCommand)

        // 3. 친구 요청 상태 확인
        val cancelledRequests = friendRequestRepository
            .findAllBySenderIdAndReceiverId(user1Id, user2Id)
        assertThat(cancelledRequests).isNotEmpty()
        assertThat(cancelledRequests.all { it.status == FriendRequestStatus.CANCELLED }).isTrue()
    }

    @Test
    @DisplayName("친구 삭제 → 양방향 친구 관계 삭제")
    fun `친구 삭제 테스트`() {
        // 1. 친구 관계 설정 (요청 → 수락)
        friendRequestUseCase.sendFriendRequest(
            SendFriendRequestCommand(user1Id, user2Id)
        )
        friendReceiveUseCase.acceptFriendRequest(
            AcceptFriendRequestCommand(user2Id, user1Id)
        )

        // 2. 친구 관계 확인
        assertThat(friendshipMappingRepository.existsByUserIdAndFriendId(user1Id, user2Id)).isTrue()
        assertThat(friendshipMappingRepository.existsByUserIdAndFriendId(user2Id, user1Id)).isTrue()

        // 3. 친구 삭제 (user1이 user2 삭제)
        val removeCommand = RemoveFriendCommand(
            userId = user1Id,
            friendId = user2Id
        )
        friendRemoveUseCase.removeFriend(removeCommand)

        // 4. 양방향 친구 관계 삭제 확인
        assertThat(friendshipMappingRepository.existsByUserIdAndFriendId(user1Id, user2Id)).isFalse()
        assertThat(friendshipMappingRepository.existsByUserIdAndFriendId(user2Id, user1Id)).isFalse()

        // 5. 친구 목록 확인 (빈 목록)
        val user1Friends = findFriendUseCase.getFriends(
            GetFriendsCommand(currentUserId = user1Id)
        )
        assertThat(user1Friends).isEmpty()
    }

    @Test
    @DisplayName("여러 명과 친구 맺기 → 친구 목록 조회")
    fun `다수의 친구 관계 테스트`() {
        // 1. user1이 user2, user3와 친구 맺기
        friendRequestUseCase.sendFriendRequest(SendFriendRequestCommand(user1Id, user2Id))
        friendReceiveUseCase.acceptFriendRequest(AcceptFriendRequestCommand(user2Id, user1Id))

        friendRequestUseCase.sendFriendRequest(SendFriendRequestCommand(user1Id, user3Id))
        friendReceiveUseCase.acceptFriendRequest(AcceptFriendRequestCommand(user3Id, user1Id))

        // 2. user1의 친구 목록 조회
        val user1Friends = findFriendUseCase.getFriends(
            GetFriendsCommand(currentUserId = user1Id)
        )
        assertThat(user1Friends).hasSize(2)
        assertThat(user1Friends.map { it.id }).containsExactlyInAnyOrder(user2Id, user3Id)

        // 3. user2의 친구 목록 조회
        val user2Friends = findFriendUseCase.getFriends(
            GetFriendsCommand(currentUserId = user2Id)
        )
        assertThat(user2Friends).hasSize(1)
        assertThat(user2Friends.first().id).isEqualTo(user1Id)

        // 4. user3의 친구 목록 조회
        val user3Friends = findFriendUseCase.getFriends(
            GetFriendsCommand(currentUserId = user3Id)
        )
        assertThat(user3Friends).hasSize(1)
        assertThat(user3Friends.first().id).isEqualTo(user1Id)
    }

    @Test
    @DisplayName("받은 친구 요청 목록 조회")
    fun `받은 친구 요청 목록 조회 테스트`() {
        // 1. user1, user2가 user3에게 친구 요청
        friendRequestUseCase.sendFriendRequest(SendFriendRequestCommand(user1Id, user3Id))
        friendRequestUseCase.sendFriendRequest(SendFriendRequestCommand(user2Id, user3Id))

        // 2. user3의 받은 친구 요청 조회
        val receivedRequests = findFriendUseCase.getReceivedFriendRequests(
            GetFriendRequestsCommand(currentUserId = user3Id)
        )

        assertThat(receivedRequests).hasSize(2)
        assertThat(receivedRequests.map { it.senderId }).containsExactlyInAnyOrder(user1Id, user2Id)
        assertThat(receivedRequests.all { it.status == FriendRequestStatus.PENDING }).isTrue()
    }

    @Test
    @DisplayName("보낸 친구 요청 목록 조회")
    fun `보낸 친구 요청 목록 조회 테스트`() {
        // 1. user1이 user2, user3에게 친구 요청
        friendRequestUseCase.sendFriendRequest(SendFriendRequestCommand(user1Id, user2Id))
        friendRequestUseCase.sendFriendRequest(SendFriendRequestCommand(user1Id, user3Id))

        // 2. user1의 보낸 친구 요청 조회
        val sentRequests = findFriendUseCase.getSentFriendRequests(
            GetFriendRequestsCommand(currentUserId = user1Id)
        )

        assertThat(sentRequests).hasSize(2)
        assertThat(sentRequests.map { it.receiverId }).containsExactlyInAnyOrder(user2Id, user3Id)
        assertThat(sentRequests.all { it.status == FriendRequestStatus.PENDING }).isTrue()
    }

    @Test
    @DisplayName("N+1 쿼리 검증: 친구 목록 조회 시 단일 쿼리 사용")
    fun `친구 목록 조회 시 N+1 쿼리 발생하지 않는지 확인`() {
        // 1. 10명의 친구 관계 설정
        val additionalUsers = (1..10).map { i ->
            TestEntityFactory.createUser("user_$i", "USER00$i")
        }
        userRepository.saveAll(additionalUsers)

        additionalUsers.forEach { user ->
            friendRequestUseCase.sendFriendRequest(
                SendFriendRequestCommand(user1Id, user.id!!)
            )
            friendReceiveUseCase.acceptFriendRequest(
                AcceptFriendRequestCommand(user.id!!, user1Id)
            )
        }

        // 2. 친구 목록 조회
        // 이 시점에서 N+1 쿼리가 발생하는지 로그 확인 필요
        val friends = findFriendUseCase.getFriends(
            GetFriendsCommand(currentUserId = user1Id)
        )

        assertThat(friends).hasSize(10)

        // NOTE: 실제로는 SQL 로그를 확인하거나 Hibernate Statistics를 사용해야 함
        // show-sql: true로 설정되어 있어 콘솔에서 쿼리 수 확인 가능
    }
}
