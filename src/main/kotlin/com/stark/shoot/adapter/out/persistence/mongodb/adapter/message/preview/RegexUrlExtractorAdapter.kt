package com.stark.shoot.adapter.out.persistence.mongodb.adapter.message.preview

import com.stark.shoot.application.port.out.message.preview.ExtractUrlPort
import com.stark.shoot.infrastructure.annotation.Adapter

@Adapter
class RegexUrlExtractorAdapter : ExtractUrlPort {
    
    private val urlRegex = Regex(
        "https?://[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)"
    )

    /**
     * 텍스트에서 URL 추출
     * @param text URL을 추출할 텍스트
     * @return 추출된 URL 리스트
     */
    override fun extractUrls(
        text: String
    ): List<String> {
        return urlRegex.findAll(text).map { it.value }.toList()
    }

}