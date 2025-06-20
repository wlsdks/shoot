package com.stark.shoot.application.service.message

import com.stark.shoot.adapter.`in`.web.dto.message.MessageResponseDto
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.application.port.`in`.message.GetMessagesUseCase
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.infrastructure.annotation.UseCase

@UseCase
class GetMessagesService(
    private val loadMessagePort: LoadMessagePort,
    private val chatMessageMapper: ChatMessageMapper
) : GetMessagesUseCase {

    /**
     * 특정 채팅방의 메시지를 조회 (DTO 반환)
     *
     * @param roomId 채팅방 ID
     * @param lastMessageId 마지막 메시지 ID (null인 경우 최신 메시지부터 조회)
     * @param limit 조회할 메시지 수량
     * @return 메시지 DTO 리스트
     */
    override fun getMessages(
        roomId: ChatRoomId,
        lastMessageId: MessageId?,
        limit: Int
    ): List<MessageResponseDto> {
        // 도메인 메시지 조회
        val domainMessages = if (lastMessageId != null) {
            loadMessagePort.findByRoomIdAndBeforeId(roomId, lastMessageId, limit)
        } else {
            loadMessagePort.findByRoomId(roomId, limit)
        }

        // DTO로 변환하여 반환
        return chatMessageMapper.toDtoList(domainMessages)
    }

}
