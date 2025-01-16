package com.stark.shoot.adapter.`in`.websocket

import com.stark.shoot.adapter.`in`.web.dto.ChatMessageRequest
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import java.lang.reflect.Type
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatWebSocketHandlerTest {

    @LocalServerPort
    private var port: Int = 0

    @Test
    fun testWebSocketConnection() {
        val latch = CountDownLatch(1) // 비동기 작업 완료를 기다리기 위한 래치
        val stompClient = WebSocketStompClient(StandardWebSocketClient())
        stompClient.messageConverter = MappingJackson2MessageConverter()

        val stompSession = stompClient
            .connectAsync("ws://localhost:$port/ws/chat", CustomStompSessionHandler())
            .get()

        // 먼저 구독을 설정
        stompSession.subscribe("/topic/messages", object : StompFrameHandler {
            override fun getPayloadType(headers: StompHeaders): Type {
                return String::class.java
            }

            override fun handleFrame(headers: StompHeaders, payload: Any?) {
                println("Received message: $payload")
                latch.countDown() // 메시지를 받으면 래치를 감소
            }
        })

        // 잠시 대기하여 구독이 완료되도록 함
        Thread.sleep(500)

        // 메시지 전송
        val message = ChatMessageRequest(content = "테스트 메시지")
        stompSession.send("/app/chat/send", message)

        // 최대 5초 동안 응답을 기다림
        assertTrue(latch.await(1, TimeUnit.SECONDS), "응답을 받지 못했습니다.")
    }

}

class CustomStompSessionHandler : StompSessionHandlerAdapter() {
    override fun afterConnected(session: StompSession, connectedHeaders: StompHeaders) {
        println("STOMP 연결 성공!")
    }

    override fun handleTransportError(session: StompSession, exception: Throwable) {
        println("STOMP 전송 오류: ${exception.message}")
    }
}


