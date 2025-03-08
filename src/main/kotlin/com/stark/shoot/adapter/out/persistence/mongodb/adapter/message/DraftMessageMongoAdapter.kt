package com.stark.shoot.adapter.out.persistence.mongodb.adapter.message

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.DraftMessageDocument
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.DraftMessageMapper
import com.stark.shoot.application.port.out.message.DraftMessagePort
import com.stark.shoot.domain.chat.message.DraftMessage
import com.stark.shoot.infrastructure.annotation.Adapter
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

@Adapter
class DraftMessageMongoAdapter(
    private val mongoTemplate: MongoTemplate,
    private val draftMessageMapper: DraftMessageMapper
) : DraftMessagePort {

    /**
     * 임시 메시지 저장
     *
     * @param draft 임시 메시지
     * @return 저장된 임시 메시지
     */
    override fun saveDraft(
        draft: DraftMessage
    ): DraftMessage {
        val document = draftMessageMapper.toDocument(draft)
        val savedDocument = mongoTemplate.save(document)
        return draftMessageMapper.toDomain(savedDocument)
    }

    /**
     * ID로 임시 메시지 조회
     *
     * @param id 임시 메시지 ID
     * @return 임시 메시지
     */
    override fun findById(
        id: ObjectId
    ): DraftMessage? {
        val document = mongoTemplate.findById(id, DraftMessageDocument::class.java)
        return document?.let { draftMessageMapper.toDomain(it) }
    }

    /**
     * 사용자와 채팅방으로 임시 메시지 조회
     *
     * @param userId 사용자 ID
     * @param roomId 채팅방 ID
     * @return 임시 메시지
     */
    override fun findByUserAndRoom(
        userId: ObjectId,
        roomId: ObjectId
    ): DraftMessage? {
        val query = Query(
            Criteria.where("userId").`is`(userId)
                .and("roomId").`is`(roomId)
        )

        val document = mongoTemplate.findOne(query, DraftMessageDocument::class.java)
        return document?.let { draftMessageMapper.toDomain(it) }
    }

    /**
     * 사용자로 임시 메시지 조회
     *
     * @param userId 사용자 ID
     * @return 임시 메시지 목록
     */
    override fun findAllByUser(
        userId: ObjectId
    ): List<DraftMessage> {
        val query = Query(Criteria.where("userId").`is`(userId))
        val documents = mongoTemplate.find(query, DraftMessageDocument::class.java)
        return documents.map { draftMessageMapper.toDomain(it) }
    }

    /**
     * 임시 메시지 삭제
     *
     * @param id 임시 메시지 ID
     * @return 삭제 여부
     */
    override fun deleteDraft(
        id: ObjectId
    ): Boolean {
        val query = Query(Criteria.where("_id").`is`(id))
        val result = mongoTemplate.remove(query, DraftMessageDocument::class.java)
        return result.deletedCount > 0
    }

}