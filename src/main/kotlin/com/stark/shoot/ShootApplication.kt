package com.stark.shoot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ShootApplication

fun main(args: Array<String>) {
	runApplication<ShootApplication>(*args)
}
