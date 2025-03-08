package com.stark.shoot.adapter.out.persistence.mongodb.adapter.chatroom

import com.stark.shoot.adapter.out.persistence.mongodb.document.room.ChatRoomDocument
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatRoomMapper
import com.stark.shoot.application.port.out.chatroom.LoadPinnedRoomsPort
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.infrastructure.annotation.Adapter
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

@Adapter
class LoadPinnedRoomsMongoAdapter(
    private val mongoTemplate: MongoTemplate,
    private val chatRoomMapper: ChatRoomMapper
) : LoadPinnedRoomsPort {

    /**
     * 사용자가 고정한 채팅방 목록을 조회합니다.
     */
    override fun findByUserId(userId: String): List<ChatRoom> {
        // 사용자 ID를 ObjectId로 변환
        val userObjectId = ObjectId(userId)
        val query = Query()

        // participantsMetadata는 document 내에 Map 형태로 저장되어 있으며,
        // 해당 사용자의 isPinned 필드가 true인 채팅방을 조회합니다.
        query.addCriteria(Criteria.where("metadata.participantsMetadata.$userObjectId.isPinned").`is`(true))

        // 쿼리 실행
        val documents = mongoTemplate.find(query, ChatRoomDocument::class.java, "chat_rooms")

        // ChatRoomDocument -> ChatRoom 변환
        return documents.map { chatRoomMapper.toDomain(it) }
    }

}