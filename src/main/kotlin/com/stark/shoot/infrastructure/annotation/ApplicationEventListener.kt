package com.stark.shoot.infrastructure.annotation

@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@UseCase
annotation class ApplicationEventListener()
