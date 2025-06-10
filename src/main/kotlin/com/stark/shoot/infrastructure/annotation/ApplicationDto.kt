package com.stark.shoot.infrastructure.annotation

/**
 * Marker annotation for DTOs returned from the application layer.
 * This helps separate domain models from external adapters.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ApplicationDto
