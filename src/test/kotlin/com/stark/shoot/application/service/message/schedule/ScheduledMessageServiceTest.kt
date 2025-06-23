package com.stark.shoot.application.service.message.schedule

import com.stark.shoot.adapter.`in`.web.dto.ApiException
import com.stark.shoot.adapter.`in`.web.dto.ErrorCode
import com.stark.shoot.adapter.`in`.web.dto.message.MessageContentResponseDto
import com.stark.shoot.adapter.`in`.web.dto.message.MessageMetadataResponseDto
import com.stark.shoot.adapter.`in`.web.dto.message.schedule.ScheduledMessageResponseDto
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ScheduledMessageMapper
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.application.port.out.message.ScheduledMessagePort
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.domain.chat.message.ScheduledMessage
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.type.ScheduledMessageStatus
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.type.ChatRoomType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import java.time.Instant
import java.time.temporal.ChronoUnit

@DisplayName("예약 메시지 서비스 테스트")
class ScheduledMessageServiceTest {

    private val scheduledMessagePort = mock(ScheduledMessagePort::class.java)
    private val chatRoomQueryPort = mock(ChatRoomQueryPort::class.java)
    private val scheduledMessageMapper = mock(ScheduledMessageMapper::class.java)

    private val sut = ScheduledMessageService(
        scheduledMessagePort,
        chatRoomQueryPort,
        scheduledMessageMapper
    )

    @Test
    @DisplayName("존재하지 않는 채팅방에 메시지를 예약하면 예외가 발생한다")
    fun `존재하지 않는 채팅방에 메시지를 예약하면 예외가 발생한다`() {
        // given
        val roomIdLong = 1L
        val roomId = ChatRoomId.from(roomIdLong)
        val senderIdLong = 2L
        val senderId = UserId.from(senderIdLong)
        val content = "테스트 예약 메시지"
        val scheduledAt = Instant.now().plus(1, ChronoUnit.HOURS)

        doReturn(null).`when`(chatRoomQueryPort).findById(roomId)

        // when & then
        val exception = assertThrows<ApiException> {
            sut.scheduleMessage(roomId, senderId, content, scheduledAt)
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.ROOM_NOT_FOUND)
        verify(chatRoomQueryPort).findById(roomId)
        verifyNoInteractions(scheduledMessagePort)
        verifyNoInteractions(scheduledMessageMapper)
    }

    @Test
    @DisplayName("채팅방에 속하지 않은 사용자가 메시지를 예약하면 예외가 발생한다")
    fun `채팅방에 속하지 않은 사용자가 메시지를 예약하면 예외가 발생한다`() {
        // given
        val roomIdLong = 1L
        val roomId = ChatRoomId.from(roomIdLong)
        val senderIdLong = 2L
        val senderId = UserId.from(senderIdLong)
        val content = "테스트 예약 메시지"
        val scheduledAt = Instant.now().plus(1, ChronoUnit.HOURS)

        val chatRoom = ChatRoom(
            id = roomId,
            title = com.stark.shoot.domain.chatroom.vo.ChatRoomTitle.from("테스트 채팅방"),
            type = ChatRoomType.GROUP,
            participants = mutableSetOf(UserId.from(3L), UserId.from(4L)) // senderId는 포함되지 않음
        )

        doReturn(chatRoom).`when`(chatRoomQueryPort).findById(roomId)

        // when & then
        val exception = assertThrows<ApiException> {
            sut.scheduleMessage(roomId, senderId, content, scheduledAt)
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.USER_NOT_IN_ROOM)
        verify(chatRoomQueryPort).findById(roomId)
        verifyNoInteractions(scheduledMessagePort)
        verifyNoInteractions(scheduledMessageMapper)
    }

    @Test
    @DisplayName("과거 시간으로 메시지를 예약하면 예외가 발생한다")
    fun `과거 시간으로 메시지를 예약하면 예외가 발생한다`() {
        // given
        val roomIdLong = 1L
        val roomId = ChatRoomId.from(roomIdLong)
        val senderIdLong = 2L
        val senderId = UserId.from(senderIdLong)
        val content = "테스트 예약 메시지"
        val scheduledAt = Instant.now().minus(1, ChronoUnit.HOURS) // 과거 시간

        val chatRoom = ChatRoom(
            id = roomId,
            title = com.stark.shoot.domain.chatroom.vo.ChatRoomTitle.from("테스트 채팅방"),
            type = ChatRoomType.GROUP,
            participants = mutableSetOf(senderId, UserId.from(3L), UserId.from(4L))
        )

        doReturn(chatRoom).`when`(chatRoomQueryPort).findById(roomId)

        // when & then
        val exception = assertThrows<ApiException> {
            sut.scheduleMessage(roomId, senderId, content, scheduledAt)
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.INVALID_SCHEDULED_TIME)
        verify(chatRoomQueryPort).findById(roomId)
        verifyNoInteractions(scheduledMessagePort)
        verifyNoInteractions(scheduledMessageMapper)
    }

    @Test
    @DisplayName("유효한 정보로 메시지를 예약할 수 있다")
    fun `유효한 정보로 메시지를 예약할 수 있다`() {
        // Create a custom implementation of ScheduledMessagePort for testing
        class TestScheduledMessagePort : ScheduledMessagePort {
            var savedMessage: ScheduledMessage? = null

            override fun saveScheduledMessage(scheduledMessage: ScheduledMessage): ScheduledMessage {
                savedMessage = scheduledMessage
                return scheduledMessage
            }

            override fun findById(id: org.bson.types.ObjectId): ScheduledMessage? = null
            override fun findByUserId(userId: Long, roomId: Long?): List<ScheduledMessage> = emptyList()
            override fun findPendingMessagesBeforeTime(time: Instant): List<ScheduledMessage> = emptyList()
        }

        // Create a subclass of ScheduledMessageMapper for testing
        class TestScheduledMessageMapper : ScheduledMessageMapper() {
            override fun toScheduledMessageResponseDto(domain: ScheduledMessage): ScheduledMessageResponseDto {
                return ScheduledMessageResponseDto(
                    id = "test-id",
                    roomId = domain.roomId,
                    senderId = domain.senderId,
                    content = MessageContentResponseDto(
                        text = domain.content.text,
                        type = domain.content.type,
                        isEdited = false,
                        isDeleted = false
                    ),
                    scheduledAt = domain.scheduledAt,
                    createdAt = Instant.now(),
                    status = ScheduledMessageStatus.PENDING,
                    metadata = MessageMetadataResponseDto()
                )
            }
        }

        // given
        val roomIdLong = 1L
        val roomId = ChatRoomId.from(roomIdLong)
        val senderIdLong = 2L
        val senderId = UserId.from(senderIdLong)
        val content = "테스트 예약 메시지"
        val scheduledAt = Instant.now().plus(1, ChronoUnit.HOURS)

        val chatRoom = ChatRoom(
            id = roomId,
            title = com.stark.shoot.domain.chatroom.vo.ChatRoomTitle.from("테스트 채팅방"),
            type = ChatRoomType.GROUP,
            participants = mutableSetOf(senderId, UserId.from(3L), UserId.from(4L))
        )

        val testScheduledMessagePort = TestScheduledMessagePort()
        val testScheduledMessageMapper = TestScheduledMessageMapper()

        // Create a mock for ChatRoomQueryPort
        val testChatRoomQueryPort = mock(ChatRoomQueryPort::class.java)
        doReturn(chatRoom).`when`(testChatRoomQueryPort).findById(roomId)

        // Create a new service instance with our test implementations
        val testService = ScheduledMessageService(
            testScheduledMessagePort,
            testChatRoomQueryPort,
            testScheduledMessageMapper
        )

        // when
        val result = testService.scheduleMessage(roomId, senderId, content, scheduledAt)

        // then
        // Verify the result is not null and has the expected properties
        assertThat(result).isNotNull
        assertThat(result.roomId).isEqualTo(roomIdLong)
        assertThat(result.senderId).isEqualTo(senderIdLong)
        assertThat(result.content.text).isEqualTo(content)
        assertThat(result.scheduledAt).isEqualTo(scheduledAt)

        // Verify the saved message has the expected properties
        val savedMessage = testScheduledMessagePort.savedMessage
        assertThat(savedMessage).isNotNull
        assertThat(savedMessage?.roomId).isEqualTo(roomIdLong)
        assertThat(savedMessage?.senderId).isEqualTo(senderIdLong)
        assertThat(savedMessage?.content?.text).isEqualTo(content)
        assertThat(savedMessage?.scheduledAt).isEqualTo(scheduledAt)

        // Verify the chatRoomQueryPort was called
        verify(testChatRoomQueryPort).findById(roomId)
    }

    @Test
    @DisplayName("예약된 메시지를 취소할 수 있다")
    fun `예약된 메시지를 취소할 수 있다`() {
        // Create a custom implementation of ScheduledMessagePort for testing
        class TestScheduledMessagePort : ScheduledMessagePort {
            var savedMessage: ScheduledMessage? = null

            // Use a valid ObjectId string (24 hexadecimal characters)
            val messageId = "507f1f77bcf86cd799439011"

            override fun saveScheduledMessage(scheduledMessage: ScheduledMessage): ScheduledMessage {
                savedMessage = scheduledMessage
                return scheduledMessage
            }

            override fun findById(id: org.bson.types.ObjectId): ScheduledMessage? {
                // In test environment, we'll return a message regardless of the ObjectId
                return ScheduledMessage(
                    id = MessageId.from(messageId),
                    roomId = 1L,
                    senderId = 2L,
                    content = MessageContent(
                        text = "테스트 예약 메시지",
                        type = MessageType.TEXT
                    ),
                    scheduledAt = Instant.now().plus(1, ChronoUnit.HOURS),
                    status = ScheduledMessageStatus.PENDING
                )
            }

            override fun findByUserId(userId: Long, roomId: Long?): List<ScheduledMessage> = emptyList()
            override fun findPendingMessagesBeforeTime(time: Instant): List<ScheduledMessage> = emptyList()
        }

        // Create a subclass of ScheduledMessageMapper for testing
        class TestScheduledMessageMapper : ScheduledMessageMapper() {
            override fun toScheduledMessageResponseDto(domain: ScheduledMessage): ScheduledMessageResponseDto {
                return ScheduledMessageResponseDto(
                    id = domain.id!!.value,
                    roomId = domain.roomId,
                    senderId = domain.senderId,
                    content = MessageContentResponseDto(
                        text = domain.content.text,
                        type = domain.content.type,
                        isEdited = false,
                        isDeleted = false
                    ),
                    scheduledAt = domain.scheduledAt,
                    createdAt = Instant.now(),
                    status = domain.status,
                    metadata = MessageMetadataResponseDto()
                )
            }
        }

        // given
        val messageIdStr = "507f1f77bcf86cd799439011" // Use a valid ObjectId string
        val userIdLong = 2L
        val userId = UserId.from(userIdLong)

        val testScheduledMessagePort = TestScheduledMessagePort()
        val testScheduledMessageMapper = TestScheduledMessageMapper()
        val testChatRoomQueryPort = mock(ChatRoomQueryPort::class.java)

        // Create a new service instance with our test implementations
        val testService = ScheduledMessageService(
            testScheduledMessagePort,
            testChatRoomQueryPort,
            testScheduledMessageMapper
        )

        // when
        val result = testService.cancelScheduledMessage(messageIdStr, userId)

        // then
        // Verify the result is not null and has the expected properties
        assertThat(result).isNotNull
        assertThat(result.id).isEqualTo(messageIdStr)
        assertThat(result.senderId).isEqualTo(userIdLong)
        assertThat(result.status).isEqualTo(ScheduledMessageStatus.CANCELED)

        // Verify the saved message has the expected properties
        val savedMessage = testScheduledMessagePort.savedMessage
        assertThat(savedMessage).isNotNull
        assertThat(savedMessage!!.id).isNotNull
        assertThat(savedMessage.id!!.value).isEqualTo(messageIdStr)
        assertThat(savedMessage.senderId).isEqualTo(userIdLong)
        assertThat(savedMessage.status).isEqualTo(ScheduledMessageStatus.CANCELED)
    }

    @Test
    @DisplayName("예약된 메시지의 내용과 시간을 수정할 수 있다")
    fun `예약된 메시지의 내용과 시간을 수정할 수 있다`() {
        // Create a custom implementation of ScheduledMessagePort for testing
        class TestScheduledMessagePort : ScheduledMessagePort {
            var savedMessage: ScheduledMessage? = null

            // Use a valid ObjectId string (24 hexadecimal characters)
            val messageId = "507f1f77bcf86cd799439011"
            val originalContent = "원본 예약 메시지"
            val originalScheduledAt = Instant.now().plus(1, ChronoUnit.HOURS)

            override fun saveScheduledMessage(scheduledMessage: ScheduledMessage): ScheduledMessage {
                savedMessage = scheduledMessage
                return scheduledMessage
            }

            override fun findById(id: org.bson.types.ObjectId): ScheduledMessage? {
                // In test environment, we'll return a message regardless of the ObjectId
                return ScheduledMessage(
                    id = MessageId.from(messageId),
                    roomId = 1L,
                    senderId = 2L,
                    content = MessageContent(
                        text = originalContent,
                        type = MessageType.TEXT
                    ),
                    scheduledAt = originalScheduledAt,
                    status = ScheduledMessageStatus.PENDING
                )
            }

            override fun findByUserId(userId: Long, roomId: Long?): List<ScheduledMessage> = emptyList()
            override fun findPendingMessagesBeforeTime(time: Instant): List<ScheduledMessage> = emptyList()
        }

        // Create a subclass of ScheduledMessageMapper for testing
        class TestScheduledMessageMapper : ScheduledMessageMapper() {
            override fun toScheduledMessageResponseDto(domain: ScheduledMessage): ScheduledMessageResponseDto {
                return ScheduledMessageResponseDto(
                    id = domain.id!!.value,
                    roomId = domain.roomId,
                    senderId = domain.senderId,
                    content = MessageContentResponseDto(
                        text = domain.content.text,
                        type = domain.content.type,
                        isEdited = false,
                        isDeleted = false
                    ),
                    scheduledAt = domain.scheduledAt,
                    createdAt = Instant.now(),
                    status = domain.status,
                    metadata = MessageMetadataResponseDto()
                )
            }
        }

        // given
        val messageId = "507f1f77bcf86cd799439011" // Use a valid ObjectId string
        val userIdLong = 2L
        val userId = UserId.from(userIdLong)
        val newContent = "수정된 예약 메시지"
        val newScheduledAt = Instant.now().plus(2, ChronoUnit.HOURS)

        val testScheduledMessagePort = TestScheduledMessagePort()
        val testScheduledMessageMapper = TestScheduledMessageMapper()
        val testChatRoomQueryPort = mock(ChatRoomQueryPort::class.java)

        // Create a new service instance with our test implementations
        val testService = ScheduledMessageService(
            testScheduledMessagePort,
            testChatRoomQueryPort,
            testScheduledMessageMapper
        )

        // when
        val result = testService.updateScheduledMessage(messageId, userIdLong, newContent, newScheduledAt)

        // then
        // Verify the result is not null and has the expected properties
        assertThat(result).isNotNull
        assertThat(result.id).isEqualTo(messageId)
        assertThat(result.senderId).isEqualTo(userIdLong)
        assertThat(result.content.text).isEqualTo(newContent)
        assertThat(result.scheduledAt).isEqualTo(newScheduledAt)
        assertThat(result.status).isEqualTo(ScheduledMessageStatus.PENDING)

        // Verify the saved message has the expected properties
        val savedMessage = testScheduledMessagePort.savedMessage
        assertThat(savedMessage).isNotNull
        assertThat(savedMessage!!.id).isNotNull
        assertThat(savedMessage.id!!.value).isEqualTo(messageId)
        assertThat(savedMessage.senderId).isEqualTo(userIdLong)
        assertThat(savedMessage.content.text).isEqualTo(newContent)
        assertThat(savedMessage.scheduledAt).isEqualTo(newScheduledAt)
        assertThat(savedMessage.status).isEqualTo(ScheduledMessageStatus.PENDING)
    }

    @Test
    @DisplayName("사용자의 예약된 메시지 목록을 조회할 수 있다")
    fun `사용자의 예약된 메시지 목록을 조회할 수 있다`() {
        // Create a custom implementation of ScheduledMessagePort for testing
        class TestScheduledMessagePort : ScheduledMessagePort {
            override fun saveScheduledMessage(scheduledMessage: ScheduledMessage): ScheduledMessage {
                return scheduledMessage
            }

            override fun findById(id: org.bson.types.ObjectId): ScheduledMessage? = null

            override fun findByUserId(userId: Long, roomId: Long?): List<ScheduledMessage> {
                // 테스트용 예약 메시지 목록 생성
                val messages = listOf(
                    ScheduledMessage(
                        id = MessageId.from("message-1"),
                        roomId = 1L,
                        senderId = userId,
                        content = MessageContent(
                            text = "첫 번째 예약 메시지",
                            type = MessageType.TEXT
                        ),
                        scheduledAt = Instant.now().plus(1, ChronoUnit.HOURS),
                        status = ScheduledMessageStatus.PENDING
                    ),
                    ScheduledMessage(
                        id = MessageId.from("message-2"),
                        roomId = 2L,
                        senderId = userId,
                        content = MessageContent(
                            text = "두 번째 예약 메시지",
                            type = MessageType.TEXT
                        ),
                        scheduledAt = Instant.now().plus(2, ChronoUnit.HOURS),
                        status = ScheduledMessageStatus.PENDING
                    ),
                    ScheduledMessage(
                        id = MessageId.from("message-3"),
                        roomId = 1L,
                        senderId = userId,
                        content = MessageContent(
                            text = "세 번째 예약 메시지",
                            type = MessageType.TEXT
                        ),
                        scheduledAt = Instant.now().plus(3, ChronoUnit.HOURS),
                        status = ScheduledMessageStatus.CANCELED  // 취소된 메시지
                    )
                )

                // roomId가 지정된 경우 해당 채팅방의 메시지만 필터링
                return if (roomId != null) {
                    messages.filter { it.roomId == roomId }
                } else {
                    messages
                }
            }

            override fun findPendingMessagesBeforeTime(time: Instant): List<ScheduledMessage> = emptyList()
        }

        // Create a subclass of ScheduledMessageMapper for testing
        class TestScheduledMessageMapper : ScheduledMessageMapper() {
            override fun toScheduledMessageResponseDto(domain: ScheduledMessage): ScheduledMessageResponseDto {
                return ScheduledMessageResponseDto(
                    id = domain.id!!.value,
                    roomId = domain.roomId,
                    senderId = domain.senderId,
                    content = MessageContentResponseDto(
                        text = domain.content.text,
                        type = domain.content.type,
                        isEdited = false,
                        isDeleted = false
                    ),
                    scheduledAt = domain.scheduledAt,
                    createdAt = Instant.now(),
                    status = domain.status,
                    metadata = MessageMetadataResponseDto()
                )
            }
        }

        // given
        val userIdLong = 2L
        val userId = UserId.from(userIdLong)

        val testScheduledMessagePort = TestScheduledMessagePort()
        val testScheduledMessageMapper = TestScheduledMessageMapper()
        val testChatRoomQueryPort = mock(ChatRoomQueryPort::class.java)

        // Create a new service instance with our test implementations
        val testService = ScheduledMessageService(
            testScheduledMessagePort,
            testChatRoomQueryPort,
            testScheduledMessageMapper
        )

        // when - 모든 채팅방의 예약 메시지 조회
        val allResults = testService.getScheduledMessagesByUser(userIdLong, null)

        // then - 대기 중인 메시지만 반환되어야 함 (PENDING 상태)
        assertThat(allResults).hasSize(2)
        assertThat(allResults.map { it.id }).containsExactlyInAnyOrder("message-1", "message-2")
        assertThat(allResults.all { it.senderId == userIdLong }).isTrue()
        assertThat(allResults.all { it.status == ScheduledMessageStatus.PENDING }).isTrue()

        // when - 특정 채팅방의 예약 메시지 조회
        val roomResults = testService.getScheduledMessagesByUser(userIdLong, 1L)

        // then - 해당 채팅방의 대기 중인 메시지만 반환되어야 함
        assertThat(roomResults).hasSize(1)
        assertThat(roomResults[0].id).isEqualTo("message-1")
        assertThat(roomResults[0].roomId).isEqualTo(1L)
        assertThat(roomResults[0].senderId).isEqualTo(userIdLong)
        assertThat(roomResults[0].status).isEqualTo(ScheduledMessageStatus.PENDING)
    }

    @Test
    @DisplayName("예약된 메시지를 즉시 전송할 수 있다")
    fun `예약된 메시지를 즉시 전송할 수 있다`() {
        // Create a custom implementation of ScheduledMessagePort for testing
        class TestScheduledMessagePort : ScheduledMessagePort {
            var savedMessage: ScheduledMessage? = null

            // Use a valid ObjectId string (24 hexadecimal characters)
            val messageId = "507f1f77bcf86cd799439011"

            override fun saveScheduledMessage(scheduledMessage: ScheduledMessage): ScheduledMessage {
                savedMessage = scheduledMessage
                return scheduledMessage
            }

            override fun findById(id: org.bson.types.ObjectId): ScheduledMessage? {
                // In test environment, we'll return a message regardless of the ObjectId
                return ScheduledMessage(
                    id = MessageId.from(messageId),
                    roomId = 1L,
                    senderId = 2L,
                    content = MessageContent(
                        text = "테스트 예약 메시지",
                        type = MessageType.TEXT
                    ),
                    scheduledAt = Instant.now().plus(1, ChronoUnit.HOURS),
                    status = ScheduledMessageStatus.PENDING
                )
            }

            override fun findByUserId(userId: Long, roomId: Long?): List<ScheduledMessage> = emptyList()
            override fun findPendingMessagesBeforeTime(time: Instant): List<ScheduledMessage> = emptyList()
        }

        // Create a subclass of ScheduledMessageMapper for testing
        class TestScheduledMessageMapper : ScheduledMessageMapper() {
            override fun toScheduledMessageResponseDto(domain: ScheduledMessage): ScheduledMessageResponseDto {
                return ScheduledMessageResponseDto(
                    id = domain.id!!.value,
                    roomId = domain.roomId,
                    senderId = domain.senderId,
                    content = MessageContentResponseDto(
                        text = domain.content.text,
                        type = domain.content.type,
                        isEdited = false,
                        isDeleted = false
                    ),
                    scheduledAt = domain.scheduledAt,
                    createdAt = Instant.now(),
                    status = domain.status,
                    metadata = MessageMetadataResponseDto()
                )
            }
        }

        // given
        val messageId = "507f1f77bcf86cd799439011" // Use a valid ObjectId string
        val userIdLong = 2L
        val userId = UserId.from(userIdLong)

        val testScheduledMessagePort = TestScheduledMessagePort()
        val testScheduledMessageMapper = TestScheduledMessageMapper()
        val testChatRoomQueryPort = mock(ChatRoomQueryPort::class.java)

        // Create a new service instance with our test implementations
        val testService = ScheduledMessageService(
            testScheduledMessagePort,
            testChatRoomQueryPort,
            testScheduledMessageMapper
        )

        // when
        val result = testService.sendScheduledMessageNow(messageId, userIdLong)

        // then
        // Verify the result is not null and has the expected properties
        assertThat(result).isNotNull
        assertThat(result.id).isEqualTo(messageId)
        assertThat(result.senderId).isEqualTo(userIdLong)
        assertThat(result.status).isEqualTo(ScheduledMessageStatus.SENT)

        // Verify the saved message has the expected properties
        val savedMessage = testScheduledMessagePort.savedMessage
        assertThat(savedMessage).isNotNull
        assertThat(savedMessage!!.id).isNotNull
        assertThat(savedMessage.id!!.value).isEqualTo(messageId)
        assertThat(savedMessage.senderId).isEqualTo(userIdLong)
        assertThat(savedMessage.status).isEqualTo(ScheduledMessageStatus.SENT)
    }

}
