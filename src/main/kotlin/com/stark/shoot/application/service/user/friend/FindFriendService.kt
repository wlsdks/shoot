package com.stark.shoot.application.service.user.friend

import com.stark.shoot.adapter.`in`.web.dto.user.FriendResponse
import com.stark.shoot.application.port.`in`.user.friend.FindFriendUseCase
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.infrastructure.common.exception.web.ResourceNotFoundException
import org.bson.types.ObjectId
import org.springframework.stereotype.Service

@Service
class FindFriendService(
    private val findUserPort: FindUserPort
) : FindFriendUseCase {

    /**
     * 친구 목록 조회
     */
    override fun getFriends(
        currentUserId: ObjectId
    ): List<FriendResponse> {
        // 현재 사용자 조회
        val user = findUserPort.findUserById(currentUserId)
            ?: throw ResourceNotFoundException("User not found")

        // 친구 목록 조회
        return user.friends.map { friendId ->
            val friend = findUserPort.findUserById(friendId)
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
        val user = findUserPort.findUserById(currentUserId)
            ?: throw ResourceNotFoundException("User not found")

        // 받은 친구 요청 목록 조회
        return user.incomingFriendRequests.map { requesterId ->
            val requester = findUserPort.findUserById(requesterId)
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
        val user = findUserPort.findUserById(currentUserId)
            ?: throw ResourceNotFoundException("User not found")

        // 보낸 친구 요청 목록 조회
        return user.outgoingFriendRequests.map { targetId ->
            val target = findUserPort.findUserById(targetId)
                ?: throw ResourceNotFoundException("Target not found: $targetId")

            // 친구 정보를 DTO로 변환
            FriendResponse(id = target.id.toString(), username = target.username)
        }
    }

}