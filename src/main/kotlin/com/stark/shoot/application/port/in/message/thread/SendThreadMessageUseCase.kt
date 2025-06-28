package com.stark.shoot.application.port.`in`.message.thread

import com.stark.shoot.application.port.`in`.message.thread.command.SendThreadMessageCommand

/**
 * 스레드에 메시지를 전송하기 위한 UseCase
 */
interface SendThreadMessageUseCase {
    fun sendThreadMessage(command: SendThreadMessageCommand)
}
