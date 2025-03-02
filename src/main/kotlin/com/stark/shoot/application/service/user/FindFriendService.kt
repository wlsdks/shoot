package com.stark.shoot.application.service.user

import com.stark.shoot.adapter.`in`.web.dto.user.FriendResponse
import com.stark.shoot.application.port.`in`.user.friend.FindFriendUseCase
import com.stark.shoot.application.port.out.user.RetrieveUserPort
import com.stark.shoot.infrastructure.common.exception.ResourceNotFoundException
import org.bson.types.ObjectId
import org.springframework.stereotype.Service

@Service
class FindFriendService(
    private val retrieveUserPort: RetrieveUserPort
) : FindFriendUseCase {

    /**
     * 친구 목록 조회
     */
    override fun getFriends(
        currentUserId: ObjectId
    ): List<FriendResponse> {
        // 현재 사용자 조회
        val user = retrieveUserPort.findById(currentUserId)
            ?: throw ResourceNotFoundException("User not found")

        // 친구 목록 조회
        return user.friends.map { friendId ->
            val friend = retrieveUserPort.findById(friendId)
                ?: throw ResourceNotFoundException("Friend not found: $friendId")

            // 친구 정보를 DTO로 변환
            FriendResponse(id = friendId.toString(), username = friend.username)
        }
    }

    /**
     * 받은 친구 요청 목록 조회
     */
    override fun getIncomingFriendRequests(
        currentUserId: ObjectId
    ): List<FriendResponse> {
        // 현재 사용자 조회
        val user = retrieveUserPort.findById(currentUserId)
            ?: throw ResourceNotFoundException("User not found")

        // 받은 친구 요청 목록 조회
        return user.incomingFriendRequests.map { requesterId ->
            val requester = retrieveUserPort.findById(requesterId)
                ?: throw ResourceNotFoundException("Requester not found: $requesterId")

            // 친구 정보를 DTO로 변환
            FriendResponse(id = requesterId.toString(), username = requester.username)
        }
    }

    /**
     * 보낸 친구 요청 목록 조회
     */
    override fun getOutgoingFriendRequests(
        currentUserId: ObjectId
    ): List<FriendResponse> {
        // 현재 사용자 조회
        val user = retrieveUserPort.findById(currentUserId)
            ?: throw ResourceNotFoundException("User not found")

        // 보낸 친구 요청 목록 조회
        return user.outgoingFriendRequests.map { targetId ->
            val target = retrieveUserPort.findById(targetId)
                ?: throw ResourceNotFoundException("Target not found: $targetId")

            // 친구 정보를 DTO로 변환
            FriendResponse(id = target.id.toString(), username = target.username)
        }
    }

}