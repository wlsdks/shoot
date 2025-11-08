package com.stark.shoot.domain.shared.event

import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Event Version Validator
 *
 * 이벤트 버전 호환성을 검증하는 유틸리티 클래스
 * MSA 환경에서 이벤트 스키마 진화를 안전하게 처리하기 위해 사용됩니다.
 */
object EventVersionValidator {

    private val logger = KotlinLogging.logger {}

    /**
     * 이벤트 버전이 지원되는지 확인
     *
     * @param event 검증할 이벤트
     * @param expectedVersion 기대하는 버전
     * @param consumerName Event Consumer 이름 (로깅용)
     * @return 버전이 호환되면 true
     */
    fun isSupported(
        event: DomainEvent,
        expectedVersion: EventVersion,
        consumerName: String
    ): Boolean {
        val eventVersion = event.version

        // Major 버전이 같으면 호환 가능 (하위 호환성)
        if (!eventVersion.isCompatibleWith(expectedVersion)) {
            logger.warn {
                "$consumerName: Incompatible event version detected. " +
                "Expected: ${expectedVersion.value}, Received: ${eventVersion.value}. " +
                "Event type: ${event::class.simpleName}"
            }
            return false
        }

        // 이벤트 버전이 기대 버전보다 새로운 경우 경고 (하위 호환이지만 알림)
        if (eventVersion.isNewerThan(expectedVersion)) {
            logger.info {
                "$consumerName: Received newer event version. " +
                "Expected: ${expectedVersion.value}, Received: ${eventVersion.value}. " +
                "Event type: ${event::class.simpleName}. " +
                "Consider upgrading consumer to support newer features."
            }
        }

        // 이벤트 버전이 기대 버전보다 오래된 경우 정보 로깅
        if (eventVersion.isOlderThan(expectedVersion)) {
            logger.debug {
                "$consumerName: Received older event version. " +
                "Expected: ${expectedVersion.value}, Received: ${eventVersion.value}. " +
                "Event type: ${event::class.simpleName}. " +
                "Backward compatibility maintained."
            }
        }

        return true
    }

    /**
     * 이벤트 버전을 체크하고 로깅
     * 버전이 호환되지 않아도 예외를 던지지 않고 경고만 로깅합니다.
     *
     * @param event 검증할 이벤트
     * @param expectedVersion 기대하는 버전
     * @param consumerName Event Consumer 이름 (로깅용)
     */
    fun checkAndLog(
        event: DomainEvent,
        expectedVersion: EventVersion,
        consumerName: String
    ) {
        isSupported(event, expectedVersion, consumerName)
    }

    /**
     * 이벤트 버전이 정확히 일치하는지 확인
     *
     * @param event 검증할 이벤트
     * @param expectedVersion 기대하는 버전
     * @return 버전이 정확히 일치하면 true
     */
    fun isExactMatch(
        event: DomainEvent,
        expectedVersion: EventVersion
    ): Boolean {
        return event.version == expectedVersion
    }

    /**
     * 여러 버전 중 하나라도 호환되는지 확인
     *
     * @param event 검증할 이벤트
     * @param supportedVersions 지원하는 버전 목록
     * @param consumerName Event Consumer 이름 (로깅용)
     * @return 호환되는 버전이 있으면 true
     */
    fun isSupportedAny(
        event: DomainEvent,
        supportedVersions: List<EventVersion>,
        consumerName: String
    ): Boolean {
        val eventVersion = event.version

        val compatible = supportedVersions.any { it.isCompatibleWith(eventVersion) }

        if (!compatible) {
            logger.warn {
                "$consumerName: Unsupported event version. " +
                "Supported versions: ${supportedVersions.joinToString { it.value }}, " +
                "Received: ${eventVersion.value}. " +
                "Event type: ${event::class.simpleName}"
            }
        }

        return compatible
    }
}
