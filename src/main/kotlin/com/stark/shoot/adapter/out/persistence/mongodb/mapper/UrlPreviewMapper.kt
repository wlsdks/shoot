package com.stark.shoot.adapter.out.persistence.mongodb.mapper

import com.stark.shoot.adapter.`in`.web.dto.message.UrlPreviewDto
import com.stark.shoot.domain.chat.message.UrlPreview
import org.springframework.stereotype.Component

@Component
class UrlPreviewMapper {

    fun domainToDto(
        urlPreview: UrlPreview?
    ): UrlPreviewDto {
        if (urlPreview != null) {
            return UrlPreviewDto(
                url = urlPreview.url,
                title = urlPreview.title,
                description = urlPreview.description,
                imageUrl = urlPreview.imageUrl
            )
        }

        return UrlPreviewDto(
            url = "",
            title = "",
            description = "",
            imageUrl = ""
        )
    }

}