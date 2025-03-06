package com.stark.shoot.adapter.`in`.web

import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
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
    fun testMongo(): ResponseDto<String> {
        val dbName = mongoTemplate.db.name
        return ResponseDto.success("MongoDB 연결 성공: $dbName", "MongoDB 연결 테스트가 성공했습니다.")
    }

}