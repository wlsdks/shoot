package com.stark.shoot.adapter.`in`.socket

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * WebSocket 세션 관리자
 * - 사용자별 연결 상태 추적
 * - 채팅방별 활성 사용자 관리
 * - 연결 통계 및 모니터링
 */
@Component
class SessionManager {
    
    private val logger = KotlinLogging.logger {}
    private val lock = ReentrantReadWriteLock()
    
    // 사용자별 세션 정보
    private data class UserSession(
        val userId: String,
        val sessionId: String,
        val connectedAt: Instant,
        val lastActivityAt: Instant,
        val activeChatRooms: MutableSet<String> = mutableSetOf()
    )
    
    // userId -> UserSession
    private val userSessions = ConcurrentHashMap<String, UserSession>()
    
    // sessionId -> userId (빠른 역방향 조회)
    private val sessionToUser = ConcurrentHashMap<String, String>()
    
    // chatRoomId -> Set<userId>
    private val chatRoomSessions = ConcurrentHashMap<String, MutableSet<String>>()
    
    /**
     * 사용자 세션 추가
     */
    fun addUserSession(userId: String, sessionId: String) {
        lock.write {
            val now = Instant.now()
            val session = UserSession(
                userId = userId,
                sessionId = sessionId,
                connectedAt = now,
                lastActivityAt = now
            )
            
            // 기존 세션이 있다면 제거
            userSessions[userId]?.let { oldSession ->
                sessionToUser.remove(oldSession.sessionId)
                // 활성 채팅방에서 제거
                oldSession.activeChatRooms.forEach { roomId ->
                    chatRoomSessions[roomId]?.remove(userId)
                }
            }
            
            userSessions[userId] = session
            sessionToUser[sessionId] = userId
            
            logger.info { "User session added: userId=$userId, sessionId=$sessionId" }
        }
    }
    
    /**
     * 사용자 세션 제거
     */
    fun removeUserSession(sessionId: String) {
        lock.write {
            val userId = sessionToUser.remove(sessionId)
            if (userId != null) {
                val session = userSessions.remove(userId)
                session?.activeChatRooms?.forEach { roomId ->
                    chatRoomSessions[roomId]?.remove(userId)
                    if (chatRoomSessions[roomId]?.isEmpty() == true) {
                        chatRoomSessions.remove(roomId)
                    }
                }
                logger.info { "User session removed: userId=$userId, sessionId=$sessionId" }
            }
        }
    }
    
    /**
     * 사용자가 채팅방에 입장
     */
    fun joinChatRoom(userId: String, chatRoomId: String) {
        lock.write {
            userSessions[userId]?.let { session ->
                session.activeChatRooms.add(chatRoomId)
                chatRoomSessions.computeIfAbsent(chatRoomId) { mutableSetOf() }.add(userId)
                logger.debug { "User joined chat room: userId=$userId, chatRoomId=$chatRoomId" }
            }
        }
    }
    
    /**
     * 사용자가 채팅방에서 퇴장
     */
    fun leaveChatRoom(userId: String, chatRoomId: String) {
        lock.write {
            userSessions[userId]?.activeChatRooms?.remove(chatRoomId)
            chatRoomSessions[chatRoomId]?.remove(userId)
            
            if (chatRoomSessions[chatRoomId]?.isEmpty() == true) {
                chatRoomSessions.remove(chatRoomId)
            }
            
            logger.debug { "User left chat room: userId=$userId, chatRoomId=$chatRoomId" }
        }
    }
    
    /**
     * 사용자 활동 시간 업데이트
     */
    fun updateLastActivity(userId: String) {
        userSessions[userId]?.let { session ->
            userSessions[userId] = session.copy(lastActivityAt = Instant.now())
        }
    }
    
    /**
     * 사용자가 온라인 상태인지 확인
     */
    fun isUserOnline(userId: String): Boolean {
        return lock.read {
            userSessions.containsKey(userId)
        }
    }
    
    /**
     * 채팅방의 활성 사용자 목록 조회
     */
    fun getActiveChatRoomUsers(chatRoomId: String): Set<String> {
        return lock.read {
            chatRoomSessions[chatRoomId]?.toSet() ?: emptySet()
        }
    }
    
    /**
     * 사용자의 활성 채팅방 목록 조회
     */
    fun getUserActiveChatRooms(userId: String): Set<String> {
        return lock.read {
            userSessions[userId]?.activeChatRooms?.toSet() ?: emptySet()
        }
    }
    
    /**
     * 전체 온라인 사용자 수 조회
     */
    fun getTotalOnlineUsers(): Int {
        return lock.read {
            userSessions.size
        }
    }
    
    /**
     * 세션 통계 정보 조회
     */
    fun getSessionStats(): Map<String, Any> {
        return lock.read {
            mapOf(
                "totalOnlineUsers" to userSessions.size,
                "totalActiveChatRooms" to chatRoomSessions.size,
                "averageRoomsPerUser" to if (userSessions.isEmpty()) 0.0 
                    else userSessions.values.sumOf { it.activeChatRooms.size }.toDouble() / userSessions.size
            )
        }
    }
    
    /**
     * 비활성 세션 정리 (예: 5분 이상 비활성)
     */
    fun cleanupInactiveSessions(inactiveThresholdMinutes: Long = 5) {
        val cutoff = Instant.now().minusSeconds(inactiveThresholdMinutes * 60)
        val inactiveSessions = mutableListOf<String>()
        
        lock.read {
            userSessions.values.forEach { session ->
                if (session.lastActivityAt.isBefore(cutoff)) {
                    inactiveSessions.add(session.sessionId)
                }
            }
        }
        
        inactiveSessions.forEach { sessionId ->
            removeUserSession(sessionId)
        }
        
        if (inactiveSessions.isNotEmpty()) {
            logger.info { "Cleaned up ${inactiveSessions.size} inactive sessions" }
        }
    }
}