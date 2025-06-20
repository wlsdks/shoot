package com.stark.shoot.adapter.out.persistence.mongodb.adapter.message

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.ScheduledMessageDocument
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ScheduledMessageMapper
import com.stark.shoot.application.port.out.message.ScheduledMessagePort
import com.stark.shoot.domain.chat.message.ScheduledMessage
import com.stark.shoot.infrastructure.annotation.Adapter
import com.stark.shoot.domain.chat.message.type.ScheduledMessageStatus
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import java.time.Instant

@Adapter
class ScheduledMessageMongoAdapter(
    private val mongoTemplate: MongoTemplate,
    private val scheduledMessageMapper: ScheduledMessageMapper
) : ScheduledMessagePort {

    /**
     * 스케줄된 메시지 저장
     *
     * @param scheduledMessage 스케줄된 메시지
     * @return 스케줄된 메시지
     */
    override fun saveScheduledMessage(
        scheduledMessage: ScheduledMessage
    ): ScheduledMessage {
        val document = scheduledMessageMapper.toDocument(scheduledMessage)
        val savedDocument = mongoTemplate.save(document)
        return scheduledMessageMapper.toDomain(savedDocument)
    }

    /**
     * ID로 스케줄된 메시지 조회
     *
     * @param id 스케줄된 메시지 ID
     * @return 스케줄된 메시지
     */
    override fun findById(
        id: ObjectId
    ): ScheduledMessage? {
        val document = mongoTemplate.findById(id, ScheduledMessageDocument::class.java)
        return document?.let { scheduledMessageMapper.toDomain(it) }
    }

    /**
     * 사용자 ID로 스케줄된 메시지 조회
     *
     * @param userId 사용자 ID
     * @param roomId 채팅방 ID
     * @return 스케줄된 메시지 목록
     */
    override fun findByUserId(
        userId: Long,
        roomId: Long?
    ): List<ScheduledMessage> {
        val criteria = Criteria.where("senderId").`is`(userId)

        if (roomId != null) {
            criteria.and("roomId").`is`(roomId)
        }

        val query = Query(criteria)
        val documents = mongoTemplate.find(query, ScheduledMessageDocument::class.java)

        return documents.map { scheduledMessageMapper.toDomain(it) }
    }

    /**
     * 특정 시간 이전의 대기 중인 메시지 조회
     *
     * @param time 시간
     * @return 대기 중인 메시지 목록
     */
    override fun findPendingMessagesBeforeTime(
        time: Instant
    ): List<ScheduledMessage> {
        val query = Query(
            Criteria.where("scheduledAt").lte(time)
                .and("status").`is`(ScheduledMessageStatus.PENDING.name)
        )

        val documents = mongoTemplate.find(query, ScheduledMessageDocument::class.java)
        return documents.map { scheduledMessageMapper.toDomain(it) }
    }

}