package com.stark.shoot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.EnableAspectJAutoProxy

@EnableAspectJAutoProxy
@SpringBootApplication
class ShootApplication

fun main(args: Array<String>) {
	runApplication<ShootApplication>(*args)
}
