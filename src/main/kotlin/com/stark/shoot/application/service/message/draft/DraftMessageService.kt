package com.stark.shoot.application.service.message.draft

import com.stark.shoot.adapter.`in`.web.dto.ApiException
import com.stark.shoot.adapter.`in`.web.dto.ErrorCode
import com.stark.shoot.adapter.`in`.web.dto.message.draft.DraftMessageResponseDto
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.DraftMessageMapper
import com.stark.shoot.application.port.`in`.message.draft.DraftMessageUseCase
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.application.port.out.message.DraftMessagePort
import com.stark.shoot.domain.chat.message.DraftMessage
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.util.toObjectId
import java.time.Instant

@UseCase
class DraftMessageService(
    private val draftMessagePort: DraftMessagePort,
    private val loadChatRoomPort: LoadChatRoomPort,
    private val draftMessageMapper: DraftMessageMapper
) : DraftMessageUseCase {

    /**
     * 임시 메시지 저장
     *
     * @param userId 사용자 ID
     * @param roomId 채팅방 ID
     * @param content 메시지 내용
     * @param attachments 첨부 파일 ID 목록
     * @param mentions 멘션된 사용자 ID 목록
     * @return 저장된 임시 메시지
     */
    override fun saveDraft(
        userId: Long,
        roomId: Long,
        content: String,
        attachments: List<String>,
        mentions: Set<String>
    ): DraftMessageResponseDto {
        // 채팅방 존재 여부 확인
        val chatRoom = loadChatRoomPort.findById(roomId)
            ?: throw ApiException("채팅방을 찾을 수 없습니다.", ErrorCode.ROOM_NOT_FOUND)

        // 사용자가 채팅방에 속해 있는지 확인
        if (!chatRoom.participants.contains(userId)) {
            throw ApiException("채팅방에 속해 있지 않습니다", ErrorCode.USER_NOT_IN_ROOM)
        }

        // 기존 임시 메시지가 있는지 확인
        val existingDraft = draftMessagePort.findByUserAndRoom(userId, roomId)

        // 기존 임시 메시지 업데이트 또는 새로 생성
        val draftToSave = existingDraft?.copy(
            content = content,
            attachments = attachments,
            mentions = mentions,
            updatedAt = Instant.now()
        ) ?: DraftMessage(
            userId = userId,
            roomId = roomId,
            content = content,
            attachments = attachments,
            mentions = mentions
        )

        val saveDraftMessage = draftMessagePort.saveDraft(draftToSave)
        return draftMessageMapper.toDraftMessageResponseDto(saveDraftMessage)
    }

    /**
     * 임시 메시지 수정
     *
     * @param draftId 임시 메시지 ID
     * @param userId 사용자 ID
     * @param content 메시지 내용
     * @param attachments 첨부 파일 ID 목록
     * @param mentions 멘션된 사용자 ID 목록
     * @return 수정된 임시 메시지
     */
    override fun updateDraft(
        draftId: String,
        userId: Long,
        content: String,
        attachments: List<String>,
        mentions: Set<String>
    ): DraftMessageResponseDto {
        // 임시 메시지 조회
        val draft = draftMessagePort.findById(draftId.toObjectId())
            ?: throw ApiException("임시 메시지를 찾을 수 없습니다: id=$draftId", ErrorCode.DRAFT_MESSAGE_NOT_FOUND)

        // 본인 확인
        if (draft.userId != userId) {
            throw ApiException("본인의 임시 메시지만 수정할 수 있습니다", ErrorCode.UNAUTHORIZED)
        }

        // 업데이트
        val updatedDraft = draft.copy(
            content = content,
            attachments = attachments,
            mentions = mentions,
            updatedAt = Instant.now()
        )

        val saveDraftMessage = draftMessagePort.saveDraft(updatedDraft)
        return draftMessageMapper.toDraftMessageResponseDto(saveDraftMessage)
    }

    /**
     * 임시 메시지 삭제
     *
     * @param draftId 임시 메시지 ID
     * @param userId 사용자 ID
     * @return 삭제 성공 여부
     */
    override fun deleteDraft(
        draftId: String,
        userId: Long
    ): Boolean {
        // 임시 메시지 조회
        val draft = draftMessagePort.findById(draftId.toObjectId())
            ?: throw ApiException("임시 메시지를 찾을 수 없습니다: id=$draftId", ErrorCode.DRAFT_MESSAGE_NOT_FOUND)

        // 본인 확인
        if (draft.userId != userId) {
            throw ApiException("본인의 임시 메시지만 삭제할 수 있습니다", ErrorCode.UNAUTHORIZED)
        }

        return draftMessagePort.deleteDraft(draftId.toObjectId())
    }

    /**
     * 채팅방 별 임시 메시지 조회
     *
     * @param userId 사용자 ID
     * @param roomId 채팅방 ID
     * @return 임시 메시지
     */
    override fun getDraftByRoom(
        userId: Long,
        roomId: Long
    ): DraftMessageResponseDto? {
        val findByUserAndRoom = draftMessagePort.findByUserAndRoom(userId, roomId)
        return draftMessageMapper.toDraftMessageResponseDto(findByUserAndRoom ?: return null)
    }

    /**
     * 모든 임시 메시지 조회
     *
     * @param userId 사용자 ID
     * @return 임시 메시지 목록
     */
    override fun getAllDrafts(
        userId: Long
    ): Map<Long, DraftMessageResponseDto> {
        val drafts = draftMessagePort.findAllByUser(userId)
        val draftMessages = drafts.associateBy { it.roomId }

        // 임시 메시지 DTO로 변환
        return draftMessages.mapValues { draftMessageMapper.toDraftMessageResponseDto(it.value) }
    }

}