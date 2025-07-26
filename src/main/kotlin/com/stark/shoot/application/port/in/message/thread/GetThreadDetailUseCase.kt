package com.stark.shoot.application.port.`in`.message.thread

import com.stark.shoot.adapter.`in`.rest.dto.message.thread.ThreadDetailDto
import com.stark.shoot.application.port.`in`.message.thread.command.GetThreadDetailCommand

interface GetThreadDetailUseCase {
    fun getThreadDetail(command: GetThreadDetailCommand): ThreadDetailDto
}
