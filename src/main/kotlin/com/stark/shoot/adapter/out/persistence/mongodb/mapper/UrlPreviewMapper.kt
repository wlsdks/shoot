package com.stark.shoot.adapter.out.persistence.mongodb.mapper

import com.stark.shoot.adapter.`in`.rest.dto.message.UrlPreviewDto
import com.stark.shoot.domain.chat.message.vo.ChatMessageMetadata
import org.springframework.stereotype.Component

@Component
class UrlPreviewMapper {

    fun domainToDto(
        urlPreview: ChatMessageMetadata.UrlPreview?
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