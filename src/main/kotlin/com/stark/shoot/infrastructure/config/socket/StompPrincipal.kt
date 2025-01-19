package com.stark.shoot.infrastructure.config.socket

import java.security.Principal

class StompPrincipal(
    private val nameValue : String
): Principal {

    override fun getName(): String {
        return nameValue
    }

}