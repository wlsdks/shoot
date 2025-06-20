package com.stark.shoot.domain.service.message

import com.stark.shoot.domain.event.MessageReactionEvent
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.service.MessageReactionService
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.message.vo.ReactionToggleResult
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.reaction.vo.MessageReactions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("메시지 리액션 도메인 서비스 테스트")
class MessageReactionServiceTest {
    private val service = MessageReactionService()

    private fun message() = ChatMessage(
        id = "m1",
        roomId = 1L,
        senderId = 2L,
        content = MessageContent("hi", MessageType.TEXT),
        status = MessageStatus.SAVED,
        createdAt = Instant.now()
    )

    @Nested
    inner class ProcessResult {
        @Test
        fun `리액션 교체 결과는 두 개의 이벤트를 생성한다`() {
            val result = ReactionToggleResult(
                reactions = MessageReactions(),
                message = message(),
                userId = 1L,
                reactionType = "heart",
                isAdded = true,
                previousReactionType = "like",
                isReplacement = true
            )

            val events = service.processReactionToggleResult(result)
            assertThat(events).hasSize(2)
            assertThat(events[0]).isEqualTo(
                MessageReactionEvent.create("m1", "1", "1", "like", false, true)
            )
            assertThat(events[1].isAdded).isTrue()
        }

        @Test
        fun `일반 추가 결과는 하나의 이벤트를 생성한다`() {
            val result = ReactionToggleResult(
                reactions = MessageReactions(),
                message = message(),
                userId = 1L,
                reactionType = "like",
                isAdded = true,
                previousReactionType = null,
                isReplacement = false
            )

            val events = service.processReactionToggleResult(result)
            assertThat(events).hasSize(1)
            assertThat(events[0]).isEqualTo(
                MessageReactionEvent.create("m1", "1", "1", "like", true, false)
            )
        }
    }
}
