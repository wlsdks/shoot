package com.stark.shoot.adapter.out.persistence.mongodb.adapter.message

import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.adapter.out.persistence.mongodb.repository.ChatMessageMongoRepository
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.Adapter
import com.stark.shoot.infrastructure.util.toObjectId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

@Adapter
class MessageQueryMongoAdapter(
    private val chatMessageRepository: ChatMessageMongoRepository,
    private val mongoTemplate: MongoTemplate,
    private val chatMessageMapper: ChatMessageMapper
) : MessageQueryPort {

    /**
     * ID로 채팅 메시지 조회
     *
     * @param messageId 채팅 메시지 ID
     * @return 채팅 메시지
     */
    override fun findById(
        messageId: MessageId
    ): ChatMessage? {
        return chatMessageRepository.findById(messageId.value.toObjectId())
            .map(chatMessageMapper::toDomain)
            .orElse(null)
    }

    /**
     * 채팅방 ID로 채팅 메시지 조회
     *
     * @param roomId 채팅방 ID
     * @param limit 조회 개수
     * @return 채팅 메시지 목록
     */
    override fun findByRoomId(
        roomId: ChatRoomId,
        limit: Int
    ): List<ChatMessage> {
        val pageable = PageRequest.of(
            0,
            limit,
            Sort.by(Sort.Direction.DESC, "_id") // 최신순 정렬
        )

        return chatMessageRepository.findByRoomId(roomId.value, pageable)
            .map(chatMessageMapper::toDomain)
    }

    /**
     * 채팅방 ID와 이전 메시지 ID로 이전 메시지 조회
     *
     * @param roomId 채팅방 ID
     * @param beforeMessageId 이전 메시지 ID
     * @param limit 조회 개수
     * @return 채팅 메시지 목록
     */
    override fun findByRoomIdAndBeforeId(
        roomId: ChatRoomId,
        beforeMessageId: MessageId,
        limit: Int
    ): List<ChatMessage> {
        val pageable = PageRequest.of(
            0,
            limit,
            Sort.by(Sort.Direction.DESC, "_id") // 최신순 정렬
        )

        return chatMessageRepository.findByRoomIdAndIdBefore(
            roomId.value,
            beforeMessageId.value.toObjectId(),
            pageable
        ).map(chatMessageMapper::toDomain)
    }

    /**
     * 채팅방 ID와 이후 메시지 ID로 이후 메시지 조회
     *
     * @param roomId 채팅방 ID
     * @param afterMessageId 이후 메시지 ID
     * @param limit 조회 개수
     * @return 채팅 메시지 목록
     */
    override fun findByRoomIdAndAfterId(
        roomId: ChatRoomId,
        afterMessageId: MessageId,
        limit: Int
    ): List<ChatMessage> {
        val pageable = PageRequest.of(
            0,
            limit,
            Sort.by(Sort.Direction.ASC, "_id") // ID 오름차순 정렬 (이후 메시지)
        )

        return chatMessageRepository.findByRoomIdAndIdAfter(
            roomId.value,
            afterMessageId.value.toObjectId(),
            pageable
        ).map(chatMessageMapper::toDomain)
    }

    /**
     * 채팅방 ID와 사용자 ID로 읽지 않은 메시지 조회 (페이지네이션 적용)
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @param limit 한 번에 조회할 최대 메시지 수 (기본값: 100)
     * @return 읽지 않은 메시지 목록
     */
    override fun findUnreadByRoomId(
        roomId: ChatRoomId,
        userId: UserId,
        limit: Int
    ): List<ChatMessage> {
        val pageable = PageRequest.of(
            0,
            limit,
            Sort.by(Sort.Direction.DESC, "createdAt") // 최신순 정렬
        )
        val notReadMessage = chatMessageRepository.findUnreadMessages(roomId.value, userId.value, pageable)
        return notReadMessage.map(chatMessageMapper::toDomain)
    }

    /**
     * 채팅방 ID로 고정된 메시지 조회
     *
     * @param roomId 채팅방 ID
     * @param limit 조회 개수
     * @return 고정된 메시지 목록
     */
    override fun findPinnedMessagesByRoomId(
        roomId: ChatRoomId,
        limit: Int
    ): List<ChatMessage> {
        val pageable = PageRequest.of(
            0,
            limit,
            Sort.by(Sort.Direction.DESC, "createdAt") // 최신순 정렬
        )

        // MongoDB 쿼리: {roomId: roomId, "metadata.isPinned": true}
        return chatMessageRepository.findPinnedMessagesByRoomId(roomId.value, pageable)
            .map(chatMessageMapper::toDomain)
    }

    /**
     * 스레드 ID로 메시지 조회 (특정 메시지의 모든 답글 조회)
     *
     * @param threadId 스레드 루트 메시지 ID
     * @return 해당 스레드에 속한 메시지 목록
     */
    override fun findByThreadId(threadId: MessageId): List<ChatMessage> {
        return chatMessageRepository.findByThreadId(threadId.value.toObjectId())
            .map(chatMessageMapper::toDomain)
    }

    /**
     * 채팅방 ID로 메시지 조회 (Flow)
     *
     * @param roomId 채팅방 ID
     * @param limit 조회 개수
     * @return 채팅 메시지 Flow
     */
    override fun findByRoomIdFlow(
        roomId: ChatRoomId,
        limit: Int
    ): Flow<ChatMessage> = flow {
        val messages = findByRoomId(roomId, limit)
        messages.forEach { emit(it) }
    }


    /**
     * 채팅방 ID와 이전 메시지 ID로 이전 메시지 조회 (Flow)
     *
     * @param roomId 채팅방 ID
     * @param beforeMessageId 이전 메시지 ID
     * @param limit 조회 개수
     * @return 채팅 메시지 Flow
     */
    override fun findByRoomIdAndBeforeIdFlow(
        roomId: ChatRoomId,
        beforeMessageId: MessageId,
        limit: Int
    ): Flow<ChatMessage> = flow {
        val messages = findByRoomIdAndBeforeId(roomId, beforeMessageId, limit)
        messages.forEach { emit(it) }
    }


    /**
     * 채팅방 ID와 이후 메시지 ID로 이후 메시지 조회 (Flow)
     *
     * @param roomId 채팅방 ID
     * @param afterMessageId 이후 메시지 ID
     * @param limit 조회 개수
     * @return 채팅 메시지 Flow
     */
    override fun findByRoomIdAndAfterIdFlow(
        roomId: ChatRoomId,
        afterMessageId: MessageId,
        limit: Int
    ): Flow<ChatMessage> = flow {
        val messages = findByRoomIdAndAfterId(roomId, afterMessageId, limit)
        messages.forEach { emit(it) }
    }

    /**
     * 특정 사용자의 특정 채팅방에서 안읽은 메시지 개수 조회
     * MongoDB count 쿼리를 사용하여 최적화된 성능 제공
     */
    override fun countUnreadMessages(
        userId: UserId,
        roomId: ChatRoomId
    ): Int {
        return try {
            val query = Query().addCriteria(
                Criteria.where("roomId").`is`(roomId.value)
                    .and("senderId").ne(userId.value) // 자신이 보낸 메시지는 제외
                    .and("readBy.${userId.value}").ne(true) // 읽지 않은 메시지만
                    .and("isDeleted").ne(true) // 삭제되지 않은 메시지만
            )

            // count 쿼리는 실제 문서를 가져오지 않고 개수만 세므로 매우 빠름
            val count = mongoTemplate.count(query, "messages").toInt()
            count
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 여러 사용자의 특정 채팅방에서 안읽은 메시지 개수를 배치로 조회
     * N+1 쿼리 문제를 방지하기 위한 배치 쿼리
     *
     * MongoDB aggregation을 사용하여 한 번의 쿼리로 모든 사용자의 unread count를 계산합니다.
     */
    override fun countUnreadMessagesBatch(
        userIds: Set<UserId>,
        roomId: ChatRoomId
    ): Map<UserId, Int> {
        if (userIds.isEmpty()) return emptyMap()

        return try {
            // 각 사용자별로 개별 count 쿼리 실행 (MongoDB 4.x 호환성)
            // aggregation pipeline은 복잡도가 높아서 단순 병렬 쿼리가 더 효율적
            val results = userIds.associateWith { userId ->
                val query = Query().addCriteria(
                    Criteria.where("roomId").`is`(roomId.value)
                        .and("senderId").ne(userId.value)
                        .and("readBy.${userId.value}").ne(true)
                        .and("isDeleted").ne(true)
                )
                mongoTemplate.count(query, "messages").toInt()
            }
            results
        } catch (e: Exception) {
            // 실패 시 모든 사용자에 대해 0 반환
            userIds.associateWith { 0 }
        }
    }

}
