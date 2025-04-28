package com.stark.shoot.adapter.out.persistence.postgres.adapter.chatroom

import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomRepository
import com.stark.shoot.application.port.out.chatroom.DeleteChatRoomPort
import com.stark.shoot.infrastructure.annotation.Adapter
import io.github.oshai.kotlinlogging.KotlinLogging

@Adapter
class DeleteChatRoomPersistenceAdapter(
    private val chatRoomRepository: ChatRoomRepository
) : DeleteChatRoomPort {

    private val logger = KotlinLogging.logger {}

    /**
     * 채팅방 ID로 채팅방 삭제
     *
     * @param roomId 채팅방 ID
     * @return 삭제 성공 여부
     */
    override fun deleteById(roomId: Long): Boolean {
        return try {
            // 채팅방 존재 여부 확인
            if (!chatRoomRepository.existsById(roomId)) {
                logger.warn { "삭제하려는 채팅방이 존재하지 않습니다: roomId=$roomId" }
                return false
            }

            // 채팅방 삭제 (JpaRepository의 deleteById 메서드 사용)
            // 연관된 ChatRoomUserEntity는 cascade 설정에 따라 자동으로 삭제됨
            chatRoomRepository.deleteById(roomId)
            logger.info { "채팅방 삭제 완료: roomId=$roomId" }
            true
        } catch (e: Exception) {
            logger.error(e) { "채팅방 삭제 중 오류 발생: roomId=$roomId" }
            false
        }
    }
}