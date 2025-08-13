package com.stark.shoot.domain.chatroom.service

import com.stark.shoot.infrastructure.config.domain.DomainConstants
import com.stark.shoot.infrastructure.exception.FavoriteLimitExceededException
import com.stark.shoot.infrastructure.exception.ValidationException

/**
 * 채팅방 검증 로직을 처리하는 도메인 서비스
 */
class ChatRoomValidationDomainService(
    val domainConstants: DomainConstants
) {
    
    /**
     * 그룹 채팅방 참여자 수 검증
     */
    fun validateGroupChatParticipants(participantCount: Int) {
        val maxParticipants = domainConstants.chatRoom.maxParticipants
        val minParticipants = domainConstants.chatRoom.minGroupParticipants
        
        if (participantCount < minParticipants) {
            throw ValidationException.GroupChatMinParticipants(minParticipants)
        }
        if (participantCount > maxParticipants) {
            throw ValidationException.GroupChatMaxParticipants(maxParticipants)
        }
    }
    
    /**
     * 고정 채팅방 수 검증
     */
    fun validatePinnedRoomLimit(currentPinnedCount: Int) {
        val maxPinned = domainConstants.chatRoom.maxPinnedMessages
        
        if (currentPinnedCount >= maxPinned) {
            throw FavoriteLimitExceededException("최대 핀 채팅방 개수를 초과했습니다. (최대: ${maxPinned}개)")
        }
    }
}