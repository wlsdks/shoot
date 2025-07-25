package com.stark.shoot.domain.chat.room

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.type.ChatRoomType
import com.stark.shoot.domain.chatroom.vo.ChatRoomAnnouncement
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.chatroom.vo.ChatRoomTitle
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.exception.FavoriteLimitExceededException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

@DisplayName("채팅방 테스트")
class ChatRoomTest {

    @Nested
    @DisplayName("채팅방 생성 시")
    inner class CreateChatRoom {

        @Test
        @DisplayName("[happy] 기본 속성으로 채팅방을 생성할 수 있다")
        fun `기본 속성으로 채팅방을 생성할 수 있다`() {
            // given
            val participants = mutableSetOf(UserId.from(1L), UserId.from(2L))

            // when
            val chatRoom = ChatRoom(
                title = ChatRoomTitle.from("테스트 채팅방"),
                type = ChatRoomType.GROUP,
                participants = participants
            )

            // then
            assertThat(chatRoom.title?.value).isEqualTo("테스트 채팅방")
            assertThat(chatRoom.type).isEqualTo(ChatRoomType.GROUP)
            assertThat(chatRoom.participants).containsExactlyInAnyOrderElementsOf(participants)
            assertThat(chatRoom.lastMessageId).isNull()
            assertThat(chatRoom.announcement).isNull()
            assertThat(chatRoom.pinnedParticipants).isEmpty()
        }

        @Test
        @DisplayName("[happy] 1:1 채팅방을 생성할 수 있다")
        fun `1대1 채팅방을 생성할 수 있다`() {
            // given
            val userId = 1L
            val friendId = 2L
            val friendName = "친구"

            // when
            val chatRoom = ChatRoom.createDirectChat(userId, friendId, friendName)

            // then
            assertThat(chatRoom.title?.value).isEqualTo("${friendName}님과의 대화")
            assertThat(chatRoom.type).isEqualTo(ChatRoomType.INDIVIDUAL)
            assertThat(chatRoom.participants).containsExactlyInAnyOrder(UserId.from(userId), UserId.from(friendId))
            assertThat(chatRoom.lastMessageId).isNull()
            assertThat(chatRoom.announcement).isNull()
            assertThat(chatRoom.pinnedParticipants).isEmpty()
        }
    }

    @Nested
    @DisplayName("채팅방 정보 업데이트 시")
    inner class UpdateChatRoom {

        @Test
        @DisplayName("[happy] 채팅방 정보를 업데이트할 수 있다")
        fun `채팅방 정보를 업데이트할 수 있다`() {
            // given
            val chatRoom = ChatRoom(
                id = ChatRoomId.from(1L),
                title = ChatRoomTitle.from("원래 제목"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(UserId.from(1L), UserId.from(2L))
            )
            val newTitle = "새 제목"
            val newAnnouncement = ChatRoomAnnouncement.from("새 공지사항")
            val newLastMessageId = MessageId.from("message123")
            val newLastActiveAt = Instant.now().plusSeconds(3600)

            // when
            val updatedChatRoom = chatRoom.update(
                title = ChatRoomTitle.from(newTitle),
                announcement = newAnnouncement,
                lastMessageId = newLastMessageId,
                lastActiveAt = newLastActiveAt
            )

            // then
            assertThat(updatedChatRoom.id).isEqualTo(chatRoom.id)
            assertThat(updatedChatRoom.title?.value).isEqualTo(newTitle)
            assertThat(updatedChatRoom.type).isEqualTo(chatRoom.type)
            assertThat(updatedChatRoom.announcement).isEqualTo(newAnnouncement)
            assertThat(updatedChatRoom.lastMessageId).isEqualTo(newLastMessageId)
            assertThat(updatedChatRoom.lastActiveAt).isEqualTo(newLastActiveAt)
            assertThat(updatedChatRoom.updatedAt).isNotNull()
        }

        @Test
        @DisplayName("[happy] 공지사항만 업데이트할 수 있다")
        fun `공지사항만 업데이트할 수 있다`() {
            // given
            val chatRoom = ChatRoom(
                title = ChatRoomTitle.from("테스트 채팅방"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(UserId.from(1L), UserId.from(2L))
            )
            val newAnnouncement = ChatRoomAnnouncement.from("새 공지사항")

            // when
            val updatedChatRoom = chatRoom.updateAnnouncement(newAnnouncement)

            // then
            assertThat(updatedChatRoom.announcement).isEqualTo(newAnnouncement)
            assertThat(updatedChatRoom.updatedAt).isNotNull()
        }

        @Test
        @DisplayName("[happy] 공지사항을 삭제할 수 있다")
        fun `공지사항을 삭제할 수 있다`() {
            // given
            val chatRoom = ChatRoom(
                title = ChatRoomTitle.from("테스트 채팅방"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(UserId.from(1L), UserId.from(2L)),
                announcement = ChatRoomAnnouncement.from("기존 공지사항")
            )

            // when
            val updatedChatRoom = chatRoom.updateAnnouncement(null)

            // then
            assertThat(updatedChatRoom.announcement).isNull()
            assertThat(updatedChatRoom.updatedAt).isNotNull()
        }
    }

    @Nested
    @DisplayName("참여자 관리 시")
    inner class ManageParticipants {

        @Test
        @DisplayName("[happy] 참여자를 추가할 수 있다")
        fun `참여자를 추가할 수 있다`() {
            // given
            val chatRoom = ChatRoom(
                title = ChatRoomTitle.from("테스트 채팅방"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(UserId.from(1L), UserId.from(2L))
            )
            val newUserId = UserId.from(3L)

            // when
            val updatedChatRoom = chatRoom.addParticipant(newUserId)

            // then
            assertThat(updatedChatRoom.participants).contains(newUserId)
            assertThat(updatedChatRoom.participants.size).isEqualTo(3)
        }

        @Test
        @DisplayName("[happy] 이미 참여 중인 사용자를 추가하면 변경이 없다")
        fun `이미 참여 중인 사용자를 추가하면 변경이 없다`() {
            // given
            val chatRoom = ChatRoom(
                title = ChatRoomTitle.from("테스트 채팅방"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(UserId.from(1L), UserId.from(2L))
            )
            val existingUserId = UserId.from(1L)

            // when
            val updatedChatRoom = chatRoom.addParticipant(existingUserId)

            // then
            assertThat(updatedChatRoom).isEqualTo(chatRoom)
            assertThat(updatedChatRoom.participants.size).isEqualTo(2)
        }

        @Test
        @DisplayName("[happy] 참여자를 제거할 수 있다")
        fun `참여자를 제거할 수 있다`() {
            // given
            val chatRoom = ChatRoom(
                title = ChatRoomTitle.from("테스트 채팅방"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(UserId.from(1L), UserId.from(2L), UserId.from(3L))
            )
            val userIdToRemove = UserId.from(2L)

            // when
            val updatedChatRoom = chatRoom.removeParticipant(userIdToRemove)

            // then
            assertThat(updatedChatRoom.participants).doesNotContain(userIdToRemove)
            assertThat(updatedChatRoom.participants.size).isEqualTo(2)
        }

        @Test
        @DisplayName("[happy] 참여하지 않은 사용자를 제거하면 변경이 없다")
        fun `참여하지 않은 사용자를 제거하면 변경이 없다`() {
            // given
            val chatRoom = ChatRoom(
                title = ChatRoomTitle.from("테스트 채팅방"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(UserId.from(1L), UserId.from(2L))
            )
            val nonExistingUserId = UserId.from(3L)

            // when
            val updatedChatRoom = chatRoom.removeParticipant(nonExistingUserId)

            // then
            assertThat(updatedChatRoom).isEqualTo(chatRoom)
            assertThat(updatedChatRoom.participants.size).isEqualTo(2)
        }

        @Test
        @DisplayName("[happy] 여러 참여자를 한번에 추가할 수 있다")
        fun `여러 참여자를 한번에 추가할 수 있다`() {
            // given
            val chatRoom = ChatRoom(
                title = ChatRoomTitle.from("테스트 채팅방"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(UserId.from(1L), UserId.from(2L))
            )
            val newUserIds = listOf(UserId.from(3L), UserId.from(4L), UserId.from(5L))

            // when
            val updatedChatRoom = chatRoom.addParticipants(newUserIds)

            // then
            assertThat(updatedChatRoom.participants).containsAll(newUserIds)
            assertThat(updatedChatRoom.participants.size).isEqualTo(5)
        }

        @Test
        @DisplayName("[happy] 여러 참여자를 한번에 제거할 수 있다")
        fun `여러 참여자를 한번에 제거할 수 있다`() {
            // given
            val chatRoom = ChatRoom(
                title = ChatRoomTitle.from("테스트 채팅방"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(
                    UserId.from(1L),
                    UserId.from(2L),
                    UserId.from(3L),
                    UserId.from(4L),
                    UserId.from(5L)
                )
            )
            val userIdsToRemove = listOf(UserId.from(2L), UserId.from(4L))

            // when
            val updatedChatRoom = chatRoom.removeParticipants(userIdsToRemove)

            // then
            assertThat(updatedChatRoom.participants).doesNotContainAnyElementsOf(userIdsToRemove)
            assertThat(updatedChatRoom.participants.size).isEqualTo(3)
        }

        @Test
        @DisplayName("[happy] 참여자 목록을 한번에 업데이트할 수 있다")
        fun `참여자 목록을 한번에 업데이트할 수 있다`() {
            // given
            val chatRoom = ChatRoom(
                title = ChatRoomTitle.from("테스트 채팅방"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(UserId.from(1L), UserId.from(2L), UserId.from(3L))
            )
            val newParticipants =
                listOf(UserId.from(2L), UserId.from(3L), UserId.from(4L), UserId.from(5L)) // 1L 제거, 4L, 5L 추가

            // when
            val updatedChatRoom = chatRoom.updateParticipants(newParticipants)

            // then
            assertThat(updatedChatRoom.participants).containsExactlyInAnyOrderElementsOf(newParticipants)
            assertThat(updatedChatRoom.participants).doesNotContain(UserId.from(1L))
            assertThat(updatedChatRoom.participants.size).isEqualTo(4)
        }
    }

    @Nested
    @DisplayName("참여자 변경 계산 시")
    inner class CalculateParticipantChanges {

        @Test
        @DisplayName("[happy] 참여자 변경 사항을 계산할 수 있다")
        fun `참여자 변경 사항을 계산할 수 있다`() {
            // given
            val chatRoom = ChatRoom(
                title = ChatRoomTitle.from("테스트 채팅방"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(UserId.from(1L), UserId.from(2L), UserId.from(3L)),
                pinnedParticipants = mutableSetOf(UserId.from(1L))
            )
            val newParticipants = setOf(UserId.from(2L), UserId.from(3L), UserId.from(4L)) // 1L 제거, 4L 추가
            val newPinnedParticipants = setOf(UserId.from(2L), UserId.from(4L)) // 1L 제거, 2L, 4L 추가

            // when
            val changes = chatRoom.calculateParticipantChanges(newParticipants, newPinnedParticipants)

            // then
            assertThat(changes.participantsToAdd).containsExactly(UserId.from(4L))
            assertThat(changes.participantsToRemove).containsExactly(UserId.from(1L))
            assertThat(changes.pinnedStatusChanges).hasSize(2)
            assertThat(changes.pinnedStatusChanges[UserId.from(2L)]).isTrue()
            assertThat(changes.pinnedStatusChanges[UserId.from(4L)]).isTrue()
        }

        @Test
        @DisplayName("[happy] 변경 사항이 없으면 빈 결과를 반환한다")
        fun `변경 사항이 없으면 빈 결과를 반환한다`() {
            // given
            val chatRoom = ChatRoom(
                title = ChatRoomTitle.from("테스트 채팅방"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(UserId.from(1L), UserId.from(2L)),
                pinnedParticipants = mutableSetOf(UserId.from(1L))
            )
            val sameParticipants = setOf(UserId.from(1L), UserId.from(2L))
            val samePinnedParticipants = setOf(UserId.from(1L))

            // when
            val changes = chatRoom.calculateParticipantChanges(sameParticipants, samePinnedParticipants)

            // then
            assertThat(changes.isEmpty()).isTrue()
            assertThat(changes.participantsToAdd).isEmpty()
            assertThat(changes.participantsToRemove).isEmpty()
            assertThat(changes.pinnedStatusChanges).isEmpty()
        }
    }

    @Nested
    @DisplayName("즐겨찾기 관리 시")
    inner class ManageFavorites {

        @Test
        @DisplayName("[happy] 채팅방을 즐겨찾기에 추가할 수 있다")
        fun `채팅방을 즐겨찾기에 추가할 수 있다`() {
            // given
            val chatRoom = ChatRoom(
                title = ChatRoomTitle.from("테스트 채팅방"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(UserId.from(1L), UserId.from(2L))
            )
            val userId = UserId.from(1L)
            val isFavorite = true
            val userPinnedRoomsCount = 0

            // when
            val updatedChatRoom = chatRoom.updateFavoriteStatus(userId, isFavorite, userPinnedRoomsCount)

            // then
            assertThat(updatedChatRoom.pinnedParticipants).contains(userId)
        }

        @Test
        @DisplayName("[happy] 이미 즐겨찾기된 채팅방을 다시 즐겨찾기하면 제거된다")
        fun `이미 즐겨찾기된 채팅방을 다시 즐겨찾기하면 제거된다`() {
            // given
            val userId = UserId.from(1L)
            val chatRoom = ChatRoom(
                title = ChatRoomTitle.from("테스트 채팅방"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(userId, UserId.from(2L)),
                pinnedParticipants = mutableSetOf(userId)
            )
            val isFavorite = true
            val userPinnedRoomsCount = 1

            // when
            val updatedChatRoom = chatRoom.updateFavoriteStatus(userId, isFavorite, userPinnedRoomsCount)

            // then
            assertThat(updatedChatRoom.pinnedParticipants).doesNotContain(userId)
        }

        @Test
        @DisplayName("[happy] 즐겨찾기를 해제할 수 있다")
        fun `즐겨찾기를 해제할 수 있다`() {
            // given
            val userId = UserId.from(1L)
            val chatRoom = ChatRoom(
                title = ChatRoomTitle.from("테스트 채팅방"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(userId, UserId.from(2L)),
                pinnedParticipants = mutableSetOf(userId)
            )
            val isFavorite = false
            val userPinnedRoomsCount = 1

            // when
            val updatedChatRoom = chatRoom.updateFavoriteStatus(userId, isFavorite, userPinnedRoomsCount)

            // then
            assertThat(updatedChatRoom.pinnedParticipants).doesNotContain(userId)
        }

        @Test
        @DisplayName("[happy] 즐겨찾기 최대 개수를 초과하면 예외가 발생한다")
        fun `즐겨찾기 최대 개수를 초과하면 예외가 발생한다`() {
            // given
            val chatRoom = ChatRoom(
                title = ChatRoomTitle.from("테스트 채팅방"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(UserId.from(1L), UserId.from(2L))
            )
            val userId = UserId.from(1L)
            val isFavorite = true
            val userPinnedRoomsCount = 5 // 최대 개수

            // when & then
            assertThrows<FavoriteLimitExceededException> {
                chatRoom.updateFavoriteStatus(userId, isFavorite, userPinnedRoomsCount)
            }
        }
    }

    @Nested
    @DisplayName("채팅방 상태 확인 시")
    inner class CheckChatRoomState {

        @Test
        @DisplayName("[happy] 채팅방이 비어있는지 확인할 수 있다")
        fun `채팅방이 비어있는지 확인할 수 있다`() {
            // given
            val emptyRoom = ChatRoom(
                title = ChatRoomTitle.from("빈 채팅방"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf()
            )
            val nonEmptyRoom = ChatRoom(
                title = ChatRoomTitle.from("채팅방"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(UserId.from(1L), UserId.from(2L))
            )

            // when & then
            assertThat(emptyRoom.isEmpty()).isTrue()
            assertThat(nonEmptyRoom.isEmpty()).isFalse()
        }

        @Test
        @DisplayName("[happy] 채팅방이 삭제되어야 하는지 확인할 수 있다")
        fun `채팅방이 삭제되어야 하는지 확인할 수 있다`() {
            // given
            val emptyRoom = ChatRoom(
                title = ChatRoomTitle.from("빈 채팅방"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf()
            )
            val nonEmptyRoom = ChatRoom(
                title = ChatRoomTitle.from("채팅방"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(UserId.from(1L), UserId.from(2L))
            )

            // when & then
            assertThat(emptyRoom.shouldBeDeleted()).isTrue()
            assertThat(nonEmptyRoom.shouldBeDeleted()).isFalse()
        }

        @Test
        @DisplayName("[happy] 1대1 채팅방인지 확인할 수 있다")
        fun `1대1 채팅방인지 확인할 수 있다`() {
            // given
            val userId1 = UserId.from(1L)
            val userId2 = UserId.from(2L)
            val directChatRoom = ChatRoom(
                title = ChatRoomTitle.from("1:1 채팅"),
                type = ChatRoomType.INDIVIDUAL,
                participants = mutableSetOf(userId1, userId2)
            )
            val groupChatRoom = ChatRoom(
                title = ChatRoomTitle.from("그룹 채팅"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(userId1, userId2, UserId.from(3L))
            )
            val wrongTypeChatRoom = ChatRoom(
                title = ChatRoomTitle.from("잘못된 타입"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(userId1, userId2)
            )

            // when & then
            assertThat(directChatRoom.isDirectChatBetween(userId1, userId2)).isTrue()
            assertThat(groupChatRoom.isDirectChatBetween(userId1, userId2)).isFalse()
            assertThat(wrongTypeChatRoom.isDirectChatBetween(userId1, userId2)).isFalse()
        }
    }

    @Nested
    @DisplayName("채팅방 표시 정보 생성 시")
    inner class CreateDisplayInfo {

        @Test
        @DisplayName("[happy] 1대1 채팅방 제목을 생성할 수 있다")
        fun `1대1 채팅방 제목을 생성할 수 있다`() {
            // given
            val userId = UserId.from(1L)
            val friendId = UserId.from(2L)
            val chatRoom = ChatRoom(
                title = ChatRoomTitle.from("친구님과의 대화"),
                type = ChatRoomType.INDIVIDUAL,
                participants = mutableSetOf(userId, friendId)
            )

            // when
            val title = chatRoom.createChatRoomTitle(userId)

            // then
            assertThat(title).isEqualTo("친구님과의 대화")
        }

        @Test
        @DisplayName("[happy] 그룹 채팅방 제목을 생성할 수 있다")
        fun `그룹 채팅방 제목을 생성할 수 있다`() {
            // given
            val userId = UserId.from(1L)
            val chatRoom = ChatRoom(
                title = ChatRoomTitle.from("프로젝트 팀"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(userId, UserId.from(2L), UserId.from(3L))
            )

            // when
            val title = chatRoom.createChatRoomTitle(userId)

            // then
            assertThat(title).isEqualTo("프로젝트 팀")
        }

        @Test
        @DisplayName("[happy] 제목이 없는 경우 기본 제목을 생성한다")
        fun `제목이 없는 경우 기본 제목을 생성한다`() {
            // given
            val userId = UserId.from(1L)
            val directChatRoom = ChatRoom(
                title = null,
                type = ChatRoomType.INDIVIDUAL,
                participants = mutableSetOf(userId, UserId.from(2L))
            )
            val groupChatRoom = ChatRoom(
                title = null,
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(userId, UserId.from(2L), UserId.from(3L))
            )

            // when
            val directTitle = directChatRoom.createChatRoomTitle(userId)
            val groupTitle = groupChatRoom.createChatRoomTitle(userId)

            // then
            assertThat(directTitle).isEqualTo("1:1 채팅방")
            assertThat(groupTitle).isEqualTo("그룹 채팅방")
        }

        @Test
        @DisplayName("[happy] 마지막 메시지 텍스트를 생성할 수 있다")
        fun `마지막 메시지 텍스트를 생성할 수 있다`() {
            // given
            val chatRoomWithMessage = ChatRoom(
                title = ChatRoomTitle.from("채팅방"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(UserId.from(1L), UserId.from(2L)),
                lastMessageId = MessageId.from("message123")
            )
            val chatRoomWithoutMessage = ChatRoom(
                title = ChatRoomTitle.from("채팅방"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(UserId.from(1L), UserId.from(2L))
            )

            // when
            val textWithMessage = chatRoomWithMessage.createLastMessageText()
            val textWithoutMessage = chatRoomWithoutMessage.createLastMessageText()

            // then
            assertThat(textWithMessage).isEqualTo("최근 메시지")
            assertThat(textWithoutMessage).isEqualTo("최근 메시지가 없습니다.")
        }

        @Test
        @DisplayName("[happy] 타임스탬프를 포맷팅할 수 있다")
        fun `타임스탬프를 포맷팅할 수 있다`() {
            // given
            val now = Instant.now()
            val chatRoom = ChatRoom(
                title = ChatRoomTitle.from("채팅방"),
                type = ChatRoomType.GROUP,
                participants = mutableSetOf(UserId.from(1L), UserId.from(2L)),
                lastActiveAt = now
            )

            // when
            val formattedTimestamp = chatRoom.formatTimestamp()

            // then
            assertThat(formattedTimestamp).isNotEmpty()
        }
    }
}