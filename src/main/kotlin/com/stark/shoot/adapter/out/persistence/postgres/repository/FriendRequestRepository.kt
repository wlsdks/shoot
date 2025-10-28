package com.stark.shoot.adapter.out.persistence.postgres.repository

import com.stark.shoot.adapter.out.persistence.postgres.entity.FriendRequestEntity
import com.stark.shoot.domain.user.type.FriendRequestStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface FriendRequestRepository : JpaRepository<FriendRequestEntity, Long> {
    fun findAllBySenderIdAndStatus(senderId: Long, status: FriendRequestStatus): List<FriendRequestEntity>
    fun findAllByReceiverIdAndStatus(receiverId: Long, status: FriendRequestStatus): List<FriendRequestEntity>
    fun findBySenderIdAndReceiverId(senderId: Long, receiverId: Long): FriendRequestEntity?
    fun findAllBySenderIdAndReceiverId(senderId: Long, receiverId: Long): List<FriendRequestEntity>
    fun existsBySenderIdAndReceiverIdAndStatus(senderId: Long, receiverId: Long, status: FriendRequestStatus): Boolean

    /**
     * 특정 발신자가 여러 수신자에게 보낸 친구 요청을 배치 조회
     * N+1 쿼리 문제를 방지하기 위한 배치 조회
     *
     * @param senderId 발신자 ID
     * @param receiverIds 수신자 ID 목록
     * @param status 요청 상태
     * @return 친구 요청 목록
     */
    fun findAllBySenderIdAndReceiverIdInAndStatus(
        senderId: Long,
        receiverIds: List<Long>,
        status: FriendRequestStatus
    ): List<FriendRequestEntity>

    /**
     * 여러 발신자로부터 특정 수신자가 받은 친구 요청을 배치 조회
     * N+1 쿼리 문제를 방지하기 위한 배치 조회
     *
     * @param senderIds 발신자 ID 목록
     * @param receiverId 수신자 ID
     * @param status 요청 상태
     * @return 친구 요청 목록
     */
    fun findAllBySenderIdInAndReceiverIdAndStatus(
        senderIds: List<Long>,
        receiverId: Long,
        status: FriendRequestStatus
    ): List<FriendRequestEntity>

    @Modifying
    @Query("""
        DELETE
        FROM FriendRequestEntity f
        WHERE f.sender.id = :senderId
    """)
    fun deleteBySenderId(@Param("senderId") senderId: Long)

    @Modifying
    @Query("""
        DELETE
        FROM FriendRequestEntity f
        WHERE f.receiver.id = :receiverId
    """)
    fun deleteByReceiverId(@Param("receiverId") receiverId: Long)
}
