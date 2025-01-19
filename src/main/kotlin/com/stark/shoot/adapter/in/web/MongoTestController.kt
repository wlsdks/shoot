package com.stark.shoot.adapter.`in`.web

import io.swagger.v3.oas.annotations.Operation
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class MongoTestController(
    private val mongoTemplate: MongoTemplate
) {

    @Operation(
        summary = "MongoDB 연결 테스트",
        description = "MongoDB 연결 테스트를 수행합니다."
    )
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
