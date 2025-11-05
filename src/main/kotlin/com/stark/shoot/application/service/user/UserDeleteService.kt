package com.stark.shoot.application.service.user

import com.stark.shoot.application.port.`in`.user.UserDeleteUseCase
import com.stark.shoot.application.port.`in`.user.command.DeleteUserCommand
import com.stark.shoot.application.port.out.chatroom.ChatRoomCommandPort
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.application.port.out.event.EventPublishPort
import com.stark.shoot.application.port.out.notification.NotificationCommandPort
import com.stark.shoot.application.port.out.user.UserCommandPort
import com.stark.shoot.application.port.out.user.UserQueryPort
import com.stark.shoot.application.port.out.user.friend.FriendCommandPort
import com.stark.shoot.application.port.out.user.friend.request.FriendRequestCommandPort
import com.stark.shoot.domain.chatroom.type.ChatRoomType
import com.stark.shoot.domain.shared.event.UserDeletedEvent
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.domain.exception.web.ResourceNotFoundException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * 사용자 삭제 서비스
 *
 * 사용자 탈퇴 시 연관된 모든 데이터를 정리합니다:
 * 1. 1:1 채팅방 삭제
 * 2. 그룹 채팅방에서 나가기
 * 3. 친구 관계 삭제
 * 4. 친구 요청 정리
 * 5. 알림 삭제
 * 6. 사용자 삭제
 * 7. 이벤트 발행
 */
@Transactional
@UseCase
class UserDeleteService(
    private val userQueryPort: UserQueryPort,
    private val userCommandPort: UserCommandPort,
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val chatRoomCommandPort: ChatRoomCommandPort,
    private val friendCommandPort: FriendCommandPort,
    private val friendRequestCommandPort: FriendRequestCommandPort,
    private val notificationCommandPort: NotificationCommandPort,
    private val eventPublisher: EventPublishPort
) : UserDeleteUseCase {

    private val logger = KotlinLogging.logger {}

    /**
     * 사용자를 삭제하고 연관 데이터를 정리합니다.
     *
     * @param command 사용자 삭제 커맨드
     */
    override fun deleteUser(command: DeleteUserCommand) {
        val userId = command.userId

        logger.info { "Starting user deletion process for userId=${userId.value}" }

        // 1. 사용자 존재 여부 확인
        val user = userQueryPort.findUserById(userId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: ${userId.value}")

        try {
            // 2. 채팅방 정리
            cleanupChatRooms(userId)

            // 3. 친구 관계 정리
            cleanupFriendships(userId)

            // 4. 친구 요청 정리
            cleanupFriendRequests(userId)

            // 5. 알림 정리
            cleanupNotifications(userId)

            // 6. 사용자 삭제
            userCommandPort.deleteUser(userId)

            // 7. 사용자 삭제 이벤트 발행
            publishUserDeletedEvent(user.username.value, userId)

            logger.info { "User deletion completed successfully: userId=${userId.value}" }

        } catch (e: Exception) {
            logger.error(e) { "Failed to delete user: userId=${userId.value}" }
            throw e
        }
    }

    /**
     * 사용자가 참여한 모든 채팅방을 정리합니다.
     * - 1:1 채팅방: 삭제
     * - 그룹 채팅방: 참여자에서 제거, 빈 방이 되면 삭제
     */
    private fun cleanupChatRooms(userId: com.stark.shoot.domain.shared.UserId) {
        try {
            val chatRooms = chatRoomQueryPort.findByParticipantId(userId)

            chatRooms.forEach { chatRoom ->
                when (chatRoom.type) {
                    ChatRoomType.INDIVIDUAL -> {
                        // 1:1 채팅방은 삭제
                        chatRoom.id?.let { roomId ->
                            chatRoomCommandPort.deleteById(roomId)
                            logger.debug { "Deleted individual chat room: roomId=${roomId.value}" }
                        }
                    }
                    ChatRoomType.GROUP -> {
                        // 그룹 채팅방은 참여자에서 제거
                        chatRoom.removeParticipant(userId)

                        if (chatRoom.participants.isEmpty()) {
                            // 빈 채팅방은 삭제
                            chatRoom.id?.let { roomId ->
                                chatRoomCommandPort.deleteById(roomId)
                                logger.debug { "Deleted empty group chat room: roomId=${roomId.value}" }
                            }
                        } else {
                            // 참여자가 남아있으면 업데이트
                            chatRoomCommandPort.save(chatRoom)
                            logger.debug { "Removed user from group chat room: roomId=${chatRoom.id?.value}" }
                        }
                    }
                }
            }

            logger.info { "Cleaned up ${chatRooms.size} chat rooms for userId=${userId.value}" }

        } catch (e: Exception) {
            logger.error(e) { "Failed to cleanup chat rooms for userId=${userId.value}" }
            throw e
        }
    }

    /**
     * 사용자의 모든 친구 관계를 삭제합니다.
     * 양방향 친구 관계를 모두 제거합니다.
     */
    private fun cleanupFriendships(userId: com.stark.shoot.domain.shared.UserId) {
        try {
            friendCommandPort.deleteAllFriendships(userId)
            logger.info { "Cleaned up all friendships for userId=${userId.value}" }

        } catch (e: Exception) {
            logger.error(e) { "Failed to cleanup friendships for userId=${userId.value}" }
            throw e
        }
    }

    /**
     * 사용자의 모든 친구 요청을 정리합니다.
     * 보낸 요청과 받은 요청 모두 삭제합니다.
     */
    private fun cleanupFriendRequests(userId: com.stark.shoot.domain.shared.UserId) {
        try {
            friendRequestCommandPort.deleteAllByUserId(userId)
            logger.info { "Cleaned up all friend requests for userId=${userId.value}" }

        } catch (e: Exception) {
            logger.error(e) { "Failed to cleanup friend requests for userId=${userId.value}" }
            throw e
        }
    }

    /**
     * 사용자의 모든 알림을 삭제합니다.
     */
    private fun cleanupNotifications(userId: com.stark.shoot.domain.shared.UserId) {
        try {
            val deletedCount = notificationCommandPort.deleteAllNotificationsForUser(userId)
            logger.info { "Cleaned up $deletedCount notifications for userId=${userId.value}" }

        } catch (e: Exception) {
            logger.error(e) { "Failed to cleanup notifications for userId=${userId.value}" }
            throw e
        }
    }

    /**
     * 사용자 삭제 이벤트를 발행합니다.
     * 트랜잭션 커밋 후 리스너들이 외부 시스템 연동, 분석 등의 처리를 수행할 수 있습니다.
     */
    private fun publishUserDeletedEvent(username: String, userId: com.stark.shoot.domain.shared.UserId) {
        try {
            val event = UserDeletedEvent.create(
                userId = userId,
                username = username,
                deletedAt = Instant.now()
            )
            eventPublisher.publishEvent(event)
            logger.debug { "UserDeletedEvent published for user ${userId.value}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to publish UserDeletedEvent for user ${userId.value}" }
        }
    }
}
