package com.stark.shoot.application.port.out.message.preview

interface ExtractUrlPort {
    fun extractUrls(text: String): List<String>
}