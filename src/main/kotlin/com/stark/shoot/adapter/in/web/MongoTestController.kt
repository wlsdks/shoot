package com.stark.shoot.adapter.`in`.web

import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class MongoTestController(
    private val mongoTemplate: MongoTemplate
) {

    @GetMapping("/test-mongo")
    fun testMongo(): String {
        return try {
            mongoTemplate.db.name // 현재 연결된 데이터베이스 이름 반환
            "MongoDB 연결 성공: ${mongoTemplate.db.name}"
        } catch (e: Exception) {
            "MongoDB 연결 실패: ${e.message}"
        }
    }
}
