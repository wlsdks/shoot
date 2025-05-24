package com.stark.shoot.domain.chat.message

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageType

data class MessageContent(
    val text: String,
    val type: MessageType,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,

    // 필요한 경우에만 남길 선택적 필드
    val metadata: ChatMessageMetadata? = null,
    val attachments: List<Attachment> = emptyList()
) {
    companion object {
        /**
         * 텍스트 메시지 컨텐츠 생성
         *
         * @param text 메시지 텍스트
         * @param isEdited 편집 여부
         * @param isDeleted 삭제 여부
         * @return 생성된 MessageContent 객체
         */
        fun createText(
            text: String,
            isEdited: Boolean = false,
            isDeleted: Boolean = false
        ): MessageContent {
            return MessageContent(
                text = text,
                type = MessageType.TEXT,
                isEdited = isEdited,
                isDeleted = isDeleted
            )
        }

        /**
         * 이미지 메시지 컨텐츠 생성
         *
         * @param text 메시지 텍스트 (캡션)
         * @param attachments 첨부 이미지 목록
         * @return 생성된 MessageContent 객체
         */
        fun createImage(
            text: String = "",
            attachments: List<Attachment>
        ): MessageContent {
            return MessageContent(
                text = text,
                type = MessageType.FILE,
                attachments = attachments
            )
        }

        /**
         * 파일 메시지 컨텐츠 생성
         *
         * @param text 메시지 텍스트 (설명)
         * @param attachments 첨부 파일 목록
         * @return 생성된 MessageContent 객체
         */
        fun createFile(
            text: String = "",
            attachments: List<Attachment>
        ): MessageContent {
            return MessageContent(
                text = text,
                type = MessageType.FILE,
                attachments = attachments
            )
        }

        /**
         * URL 메시지 컨텐츠 생성
         *
         * @param text 메시지 텍스트 (URL 포함)
         * @param urlPreview URL 미리보기 정보
         * @return 생성된 MessageContent 객체
         */
        fun createUrl(
            text: String,
            urlPreview: UrlPreview? = null
        ): MessageContent {
            val metadata = if (urlPreview != null) {
                ChatMessageMetadata.create().copy(urlPreview = urlPreview)
            } else {
                null
            }

            return MessageContent(
                text = text,
                type = MessageType.URL,
                metadata = metadata
            )
        }

        /**
         * 이모티콘 메시지 컨텐츠 생성
         *
         * @param text 이모티콘 코드 또는 설명
         * @return 생성된 MessageContent 객체
         */
        fun createEmoticon(
            text: String
        ): MessageContent {
            return MessageContent(
                text = text,
                type = MessageType.EMOTICON
            )
        }
    }
}
