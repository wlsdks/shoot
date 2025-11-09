package com.stark.shoot.domain.chatroom.service

import com.stark.shoot.domain.chatroom.constants.ChatRoomConstants
import com.stark.shoot.domain.chatroom.exception.ChatRoomValidationException
import com.stark.shoot.domain.chatroom.exception.FavoriteLimitExceededException

/**
 * 채팅방 검증 로직을 처리하는 도메인 서비스
 *
 * DDD 개선: ValidationException → ChatRoomValidationException 사용
 */
class ChatRoomValidationDomainService(
    val chatRoomConstants: ChatRoomConstants
) {

    /**
     * 그룹 채팅방 참여자 수 검증
     */
    fun validateGroupChatParticipants(participantCount: Int) {
        val maxParticipants = chatRoomConstants.maxParticipants
        val minParticipants = chatRoomConstants.minGroupParticipants

        if (participantCount < minParticipants) {
            throw ChatRoomValidationException.GroupChatMinParticipants(minParticipants)
        }
        if (participantCount > maxParticipants) {
            throw ChatRoomValidationException.GroupChatMaxParticipants(maxParticipants)
        }
    }
    
    /**
     * 고정 채팅방 수 검증
     */
    fun validatePinnedRoomLimit(currentPinnedCount: Int) {
        val maxPinned = chatRoomConstants.maxPinnedMessages
        
        if (currentPinnedCount >= maxPinned) {
            throw FavoriteLimitExceededException("최대 핀 채팅방 개수를 초과했습니다. (최대: ${maxPinned}개)")
        }
    }
}