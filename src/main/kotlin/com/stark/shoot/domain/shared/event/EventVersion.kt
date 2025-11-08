package com.stark.shoot.domain.shared.event

/**
 * Event Schema Version Value Object
 *
 * Semantic Versioning을 따르는 이벤트 스키마 버전
 * - MAJOR: Breaking changes (호환되지 않는 변경)
 * - MINOR: Backward-compatible additions (하위 호환 가능한 추가)
 * - PATCH: Bug fixes (버그 수정)
 *
 * 예시:
 * - 1.0.0 → 1.1.0: 새 필드 추가 (선택적)
 * - 1.1.0 → 2.0.0: 필수 필드 제거 또는 타입 변경
 *
 * MSA 환경에서 이벤트 스키마 진화를 관리하기 위해 사용됩니다.
 */
@JvmInline
value class EventVersion private constructor(val value: String) {

    companion object {
        // Current versions for each event type
        val MESSAGE_SENT_V1 = EventVersion("1.0.0")
        val MESSAGE_EDITED_V1 = EventVersion("1.0.0")
        val MESSAGE_DELETED_V1 = EventVersion("1.0.0")
        val MESSAGE_REACTION_V1 = EventVersion("1.0.0")
        val MESSAGE_PIN_V1 = EventVersion("1.0.0")
        val MESSAGE_BULK_READ_V1 = EventVersion("1.0.0")
        val MENTION_V1 = EventVersion("1.0.0")

        val CHATROOM_CREATED_V1 = EventVersion("1.0.0")
        val CHATROOM_TITLE_CHANGED_V1 = EventVersion("1.0.0")
        val CHATROOM_PARTICIPANT_CHANGED_V1 = EventVersion("1.0.0")

        val FRIEND_ADDED_V1 = EventVersion("1.0.0")
        val FRIEND_REMOVED_V1 = EventVersion("1.0.0")
        val FRIEND_REQUEST_SENT_V1 = EventVersion("1.0.0")
        val FRIEND_REQUEST_REJECTED_V1 = EventVersion("1.0.0")
        val FRIEND_REQUEST_CANCELLED_V1 = EventVersion("1.0.0")

        val USER_CREATED_V1 = EventVersion("1.0.0")
        val USER_DELETED_V1 = EventVersion("1.0.0")

        val NOTIFICATION_V1 = EventVersion("1.0.0")

        /**
         * 문자열로부터 EventVersion 생성
         *
         * @param version Semantic Versioning 형식의 버전 문자열 (예: "1.0.0")
         * @return EventVersion 인스턴스
         * @throws IllegalArgumentException 버전 형식이 잘못된 경우
         */
        fun from(version: String): EventVersion {
            require(version.matches(Regex("""^\d+\.\d+\.\d+$"""))) {
                "버전은 Semantic Versioning 형식(MAJOR.MINOR.PATCH)이어야 합니다. 예: 1.0.0"
            }
            return EventVersion(version)
        }

        /**
         * Major, Minor, Patch 값으로 EventVersion 생성
         *
         * @param major Major 버전 (Breaking changes)
         * @param minor Minor 버전 (Backward-compatible additions)
         * @param patch Patch 버전 (Bug fixes)
         * @return EventVersion 인스턴스
         */
        fun of(major: Int, minor: Int, patch: Int): EventVersion {
            require(major >= 0 && minor >= 0 && patch >= 0) {
                "버전 값은 0 이상이어야 합니다."
            }
            return EventVersion("$major.$minor.$patch")
        }
    }

    /**
     * Major 버전 번호
     */
    val major: Int
        get() = value.split(".")[0].toInt()

    /**
     * Minor 버전 번호
     */
    val minor: Int
        get() = value.split(".")[1].toInt()

    /**
     * Patch 버전 번호
     */
    val patch: Int
        get() = value.split(".")[2].toInt()

    /**
     * 다른 버전과 호환 가능한지 확인
     * Major 버전이 같으면 호환 가능 (하위 호환성)
     *
     * @param other 비교할 버전
     * @return 호환 가능하면 true
     */
    fun isCompatibleWith(other: EventVersion): Boolean {
        return this.major == other.major
    }

    /**
     * 이 버전이 다른 버전보다 새로운지 확인
     *
     * @param other 비교할 버전
     * @return 이 버전이 더 새로우면 true
     */
    fun isNewerThan(other: EventVersion): Boolean {
        return when {
            this.major != other.major -> this.major > other.major
            this.minor != other.minor -> this.minor > other.minor
            else -> this.patch > other.patch
        }
    }

    /**
     * 이 버전이 다른 버전보다 오래되었는지 확인
     *
     * @param other 비교할 버전
     * @return 이 버전이 더 오래되었으면 true
     */
    fun isOlderThan(other: EventVersion): Boolean {
        return other.isNewerThan(this)
    }

    override fun toString(): String = value
}
