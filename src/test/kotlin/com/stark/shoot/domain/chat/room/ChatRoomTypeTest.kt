package com.stark.shoot.domain.chat.room

import com.stark.shoot.domain.chatroom.type.ChatRoomType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("채팅방 유형 테스트")
class ChatRoomTypeTest {

    @Test
    @DisplayName("[happy] 채팅방 유형은 INDIVIDUAL과 GROUP 두 가지가 있다")
    fun `채팅방 유형은 INDIVIDUAL과 GROUP 두 가지가 있다`() {
        // when
        val types = ChatRoomType.values()

        // then
        assertThat(types).hasSize(2)
        assertThat(types).containsExactlyInAnyOrder(ChatRoomType.INDIVIDUAL, ChatRoomType.GROUP)
    }

    @Test
    @DisplayName("[happy] INDIVIDUAL 유형은 1대1 개인 채팅방을 나타낸다")
    fun `INDIVIDUAL 유형은 1대1 개인 채팅방을 나타낸다`() {
        // when
        val individualType = ChatRoomType.INDIVIDUAL

        // then
        assertThat(individualType.name).isEqualTo("INDIVIDUAL")
        assertThat(individualType.ordinal).isEqualTo(0)
    }

    @Test
    @DisplayName("[happy] GROUP 유형은 그룹 채팅방을 나타낸다")
    fun `GROUP 유형은 그룹 채팅방을 나타낸다`() {
        // when
        val groupType = ChatRoomType.GROUP

        // then
        assertThat(groupType.name).isEqualTo("GROUP")
        assertThat(groupType.ordinal).isEqualTo(1)
    }

    @Test
    @DisplayName("[happy] 문자열로부터 채팅방 유형을 변환할 수 있다")
    fun `문자열로부터 채팅방 유형을 변환할 수 있다`() {
        // when
        val individualType = ChatRoomType.valueOf("INDIVIDUAL")
        val groupType = ChatRoomType.valueOf("GROUP")

        // then
        assertThat(individualType).isEqualTo(ChatRoomType.INDIVIDUAL)
        assertThat(groupType).isEqualTo(ChatRoomType.GROUP)
    }
}