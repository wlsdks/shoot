package com.stark.shoot.adapter.`in`.web.sse

import com.stark.shoot.application.port.`in`.chatroom.SseEmitterUseCase
import com.stark.shoot.domain.user.vo.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@DisplayName("ChatUnreadCountController 단위 테스트")
class ChatUnreadCountControllerTest {

    private val sseEmitterUseCase = mock(SseEmitterUseCase::class.java)
    private val controller = ChatUnreadCountController(sseEmitterUseCase)

    @Test
    @DisplayName("[happy] 사용자의 SSE 연결을 생성한다")
    fun `사용자의 SSE 연결을 생성한다`() {
        // given
        val userId = 1L
        val mockEmitter = mock(SseEmitter::class.java)
        
        `when`(sseEmitterUseCase.createEmitter(UserId.from(userId)))
            .thenReturn(mockEmitter)

        // when
        val result = controller.streamUpdates(userId)

        // then
        assertThat(result).isNotNull
        assertThat(result).isEqualTo(mockEmitter)
        
        verify(sseEmitterUseCase).createEmitter(UserId.from(userId))
    }
}