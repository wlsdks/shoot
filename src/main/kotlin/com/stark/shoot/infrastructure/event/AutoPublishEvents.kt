package com.stark.shoot.infrastructure.event

/**
 * 자동 이벤트 발행을 위한 애노테이션
 * 클래스나 메서드에 적용할 수 있습니다.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class AutoPublishEvents