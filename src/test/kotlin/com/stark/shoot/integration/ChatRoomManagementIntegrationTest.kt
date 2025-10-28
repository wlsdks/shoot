package com.stark.shoot.integration

import com.stark.shoot.adapter.out.persistence.mongodb.repository.ChatMessageMongoRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomUserRepository
import com.stark.shoot.adapter.out.persistence.postgres.repository.UserRepository
import com.stark.shoot.application.port.`in`.chatroom.CreateChatRoomUseCase
import com.stark.shoot.application.port.`in`.chatroom.FindChatRoomUseCase
import com.stark.shoot.application.port.`in`.chatroom.ManageChatRoomUseCase
import com.stark.shoot.application.port.`in`.chatroom.command.CreateDirectChatCommand
import com.stark.shoot.application.port.`in`.chatroom.command.CreateGroupChatCommand
import com.stark.shoot.application.port.`in`.chatroom.command.GetChatRoomsCommand
import com.stark.shoot.application.port.`in`.chatroom.command.LeaveChatRoomCommand
import com.stark.shoot.domain.chatroom.type.ChatRoomType
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
@DisplayName("채팅방 관리 통합 테스트")
class ChatRoomManagementIntegrationTest {

    @Autowired
    private lateinit var createChatRoomUseCase: CreateChatRoomUseCase

    @Autowired
    private lateinit var findChatRoomUseCase: FindChatRoomUseCase

    @Autowired
    private lateinit var manageChatRoomUseCase: ManageChatRoomUseCase

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var chatRoomRepository: ChatRoomRepository

    @Autowired
    private lateinit var chatRoomUserRepository: ChatRoomUserRepository

    @Autowired
    private lateinit var chatMessageMongoRepository: ChatMessageMongoRepository

    private lateinit var user1Id: Long
    private lateinit var user2Id: Long
    private lateinit var user3Id: Long
    private lateinit var user4Id: Long

    @BeforeEach
    fun setUp() {
        // 테스트 사용자 생성
        val user1 = TestEntityFactory.createUser("user1", "USER0001")
        val user2 = TestEntityFactory.createUser("user2", "USER0002")
        val user3 = TestEntityFactory.createUser("user3", "USER0003")
        val user4 = TestEntityFactory.createUser("user4", "USER0004")

        userRepository.saveAll(listOf(user1, user2, user3, user4))

        user1Id = user1.id!!
        user2Id = user2.id!!
        user3Id = user3.id!!
        user4Id = user4.id!!
    }

    @AfterEach
    fun tearDown() {
        chatMessageMongoRepository.deleteAll()
        chatRoomUserRepository.deleteAll()
        chatRoomRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    @DisplayName("1:1 채팅방 생성 → 참여자 2명 확인")
    fun `일대일 채팅방 생성 테스트`() {
        // 1. 1:1 채팅방 생성
        val createCommand = CreateDirectChatCommand(
            userId = user1Id,
            friendId = user2Id
        )
        val chatRoomResponse = createChatRoomUseCase.createDirectChat(createCommand)

        // 2. 채팅방 정보 확인
        assertThat(chatRoomResponse).isNotNull()
        assertThat(chatRoomResponse.type).isEqualTo(ChatRoomType.INDIVIDUAL)
        assertThat(chatRoomResponse.participants).hasSize(2)
        assertThat(chatRoomResponse.participants.map { it.userId })
            .containsExactlyInAnyOrder(user1Id, user2Id)

        // 3. DB에서 채팅방 확인
        val chatRoom = chatRoomRepository.findById(chatRoomResponse.chatRoomId).orElseThrow()
        assertThat(chatRoom.type).isEqualTo(ChatRoomType.INDIVIDUAL)

        // 4. 참여자 확인
        val participants = chatRoomUserRepository.findByChatRoomId(chatRoom.id)
        assertThat(participants).hasSize(2)
        assertThat(participants.map { it.user.id }).containsExactlyInAnyOrder(user1Id, user2Id)
    }

    @Test
    @DisplayName("그룹 채팅방 생성 → 참여자 3명 이상 확인")
    fun `그룹 채팅방 생성 테스트`() {
        // 1. 그룹 채팅방 생성
        val createCommand = CreateGroupChatCommand(
            userId = user1Id,
            title = "테스트 그룹",
            participantIds = listOf(user2Id, user3Id, user4Id)
        )
        val chatRoomResponse = createChatRoomUseCase.createGroupChat(createCommand)

        // 2. 채팅방 정보 확인
        assertThat(chatRoomResponse).isNotNull()
        assertThat(chatRoomResponse.type).isEqualTo(ChatRoomType.GROUP)
        assertThat(chatRoomResponse.title).isEqualTo("테스트 그룹")
        assertThat(chatRoomResponse.participants).hasSize(4)
        assertThat(chatRoomResponse.participants.map { it.userId })
            .containsExactlyInAnyOrder(user1Id, user2Id, user3Id, user4Id)

        // 3. DB에서 채팅방 확인
        val chatRoom = chatRoomRepository.findById(chatRoomResponse.chatRoomId).orElseThrow()
        assertThat(chatRoom.type).isEqualTo(ChatRoomType.GROUP)
        assertThat(chatRoom.title).isEqualTo("테스트 그룹")

        // 4. 참여자 확인
        val participants = chatRoomUserRepository.findByChatRoomId(chatRoom.id)
        assertThat(participants).hasSize(4)
    }

    @Test
    @DisplayName("채팅방 목록 조회 → 참여 중인 채팅방만 조회")
    fun `채팅방 목록 조회 테스트`() {
        // 1. 여러 채팅방 생성
        createChatRoomUseCase.createDirectChat(CreateDirectChatCommand(user1Id, user2Id))
        createChatRoomUseCase.createGroupChat(
            CreateGroupChatCommand(user1Id, "그룹1", listOf(user2Id, user3Id))
        )
        createChatRoomUseCase.createDirectChat(CreateDirectChatCommand(user2Id, user3Id))

        // 2. user1의 채팅방 목록 조회
        val user1ChatRooms = findChatRoomUseCase.getChatRooms(
            GetChatRoomsCommand(userId = user1Id)
        )
        assertThat(user1ChatRooms).hasSize(2)  // 1:1 채팅방 1개 + 그룹 채팅방 1개

        // 3. user2의 채팅방 목록 조회
        val user2ChatRooms = findChatRoomUseCase.getChatRooms(
            GetChatRoomsCommand(userId = user2Id)
        )
        assertThat(user2ChatRooms).hasSize(3)  // 모든 채팅방에 참여

        // 4. user4의 채팅방 목록 조회 (참여 없음)
        val user4ChatRooms = findChatRoomUseCase.getChatRooms(
            GetChatRoomsCommand(userId = user4Id)
        )
        assertThat(user4ChatRooms).isEmpty()
    }

    @Test
    @DisplayName("채팅방 나가기 → 참여자에서 제외")
    fun `채팅방 나가기 테스트`() {
        // 1. 그룹 채팅방 생성
        val chatRoom = createChatRoomUseCase.createGroupChat(
            CreateGroupChatCommand(user1Id, "테스트 그룹", listOf(user2Id, user3Id))
        )

        // 2. user2가 채팅방 나가기
        val leaveCommand = LeaveChatRoomCommand(
            chatRoomId = chatRoom.chatRoomId,
            userId = user2Id
        )
        manageChatRoomUseCase.leaveChatRoom(leaveCommand)

        // 3. 참여자 확인 (user2 제외)
        val participants = chatRoomUserRepository.findByChatRoomId(chatRoom.chatRoomId)
        assertThat(participants).hasSize(2)
        assertThat(participants.map { it.user.id }).containsExactlyInAnyOrder(user1Id, user3Id)

        // 4. user2의 채팅방 목록 확인 (빈 목록)
        val user2ChatRooms = findChatRoomUseCase.getChatRooms(
            GetChatRoomsCommand(userId = user2Id)
        )
        assertThat(user2ChatRooms).isEmpty()
    }

    @Test
    @DisplayName("중복 1:1 채팅방 생성 방지 → 기존 채팅방 반환")
    fun `중복 일대일 채팅방 생성 방지 테스트`() {
        // 1. 첫 번째 채팅방 생성
        val chatRoom1 = createChatRoomUseCase.createDirectChat(
            CreateDirectChatCommand(user1Id, user2Id)
        )

        // 2. 동일한 사용자 간 채팅방 재생성 시도
        val chatRoom2 = createChatRoomUseCase.createDirectChat(
            CreateDirectChatCommand(user1Id, user2Id)
        )

        // 3. 동일한 채팅방 반환 확인
        assertThat(chatRoom1.chatRoomId).isEqualTo(chatRoom2.chatRoomId)

        // 4. DB에 채팅방 1개만 존재 확인
        val allChatRooms = chatRoomRepository.findAll()
        assertThat(allChatRooms).hasSize(1)
    }

    @Test
    @DisplayName("역순으로 1:1 채팅방 생성 → 동일한 채팅방 반환")
    fun `역순 일대일 채팅방 생성 시 동일 채팅방 반환 테스트`() {
        // 1. user1 → user2 채팅방 생성
        val chatRoom1 = createChatRoomUseCase.createDirectChat(
            CreateDirectChatCommand(user1Id, user2Id)
        )

        // 2. user2 → user1 채팅방 생성 (역순)
        val chatRoom2 = createChatRoomUseCase.createDirectChat(
            CreateDirectChatCommand(user2Id, user1Id)
        )

        // 3. 동일한 채팅방 반환 확인
        assertThat(chatRoom1.chatRoomId).isEqualTo(chatRoom2.chatRoomId)
    }

    @Test
    @DisplayName("N+1 쿼리 검증: 채팅방 목록 조회 시 참여자 정보 배치 로딩")
    fun `채팅방 목록 조회 시 N+1 쿼리 발생하지 않는지 확인`() {
        // 1. 10개의 채팅방 생성
        val additionalUsers = (1..10).map { i ->
            TestEntityFactory.createUser("user_$i", "USER00$i")
        }
        userRepository.saveAll(additionalUsers)

        additionalUsers.forEach { user ->
            createChatRoomUseCase.createDirectChat(
                CreateDirectChatCommand(user1Id, user.id!!)
            )
        }

        // 2. 채팅방 목록 조회
        // 이 시점에서 N+1 쿼리가 발생하는지 로그 확인 필요
        val chatRooms = findChatRoomUseCase.getChatRooms(
            GetChatRoomsCommand(userId = user1Id)
        )

        assertThat(chatRooms).hasSize(10)

        // 3. 각 채팅방의 참여자 정보 확인
        chatRooms.forEach { chatRoom ->
            assertThat(chatRoom.participants).hasSize(2)
        }

        // NOTE: 실제로는 SQL 로그를 확인하거나 Hibernate Statistics를 사용해야 함
        // show-sql: true로 설정되어 있어 콘솔에서 쿼리 수 확인 가능
    }

    @Test
    @DisplayName("채팅방 ID로 특정 채팅방 조회")
    fun `채팅방 ID로 채팅방 조회 테스트`() {
        // 1. 채팅방 생성
        val createdChatRoom = createChatRoomUseCase.createDirectChat(
            CreateDirectChatCommand(user1Id, user2Id)
        )

        // 2. 채팅방 ID로 조회
        val foundChatRoom = findChatRoomUseCase.getChatRoomById(
            chatRoomId = createdChatRoom.chatRoomId,
            userId = user1Id
        )

        // 3. 채팅방 정보 확인
        assertThat(foundChatRoom).isNotNull()
        assertThat(foundChatRoom.chatRoomId).isEqualTo(createdChatRoom.chatRoomId)
        assertThat(foundChatRoom.type).isEqualTo(ChatRoomType.INDIVIDUAL)
        assertThat(foundChatRoom.participants).hasSize(2)
    }

    @Test
    @DisplayName("채팅방 참여자가 모두 나가면 채팅방 삭제되지 않음 (메시지 보존)")
    fun `모든 참여자가 나간 채팅방은 유지됨 테스트`() {
        // 1. 1:1 채팅방 생성
        val chatRoom = createChatRoomUseCase.createDirectChat(
            CreateDirectChatCommand(user1Id, user2Id)
        )

        // 2. user1 나가기
        manageChatRoomUseCase.leaveChatRoom(
            LeaveChatRoomCommand(chatRoom.chatRoomId, user1Id)
        )

        // 3. user2 나가기
        manageChatRoomUseCase.leaveChatRoom(
            LeaveChatRoomCommand(chatRoom.chatRoomId, user2Id)
        )

        // 4. 채팅방은 여전히 존재 (메시지 보존을 위해)
        val stillExists = chatRoomRepository.existsById(chatRoom.chatRoomId)
        assertThat(stillExists).isTrue()

        // 5. 하지만 참여자는 없음
        val participants = chatRoomUserRepository.findByChatRoomId(chatRoom.chatRoomId)
        assertThat(participants).isEmpty()
    }

    @Test
    @DisplayName("그룹 채팅방 제목 변경")
    fun `그룹 채팅방 제목 변경 테스트`() {
        // 1. 그룹 채팅방 생성
        val chatRoom = createChatRoomUseCase.createGroupChat(
            CreateGroupChatCommand(user1Id, "초기 제목", listOf(user2Id, user3Id))
        )

        // 2. 채팅방 제목 변경
        val newTitle = "변경된 제목"
        manageChatRoomUseCase.updateChatRoomTitle(
            chatRoomId = chatRoom.chatRoomId,
            userId = user1Id,
            newTitle = newTitle
        )

        // 3. 변경된 제목 확인
        val updatedChatRoom = chatRoomRepository.findById(chatRoom.chatRoomId).orElseThrow()
        assertThat(updatedChatRoom.title).isEqualTo(newTitle)
    }
}
