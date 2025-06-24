package com.stark.shoot.adapter.`in`.web

import com.mongodb.client.MongoDatabase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.data.mongodb.core.MongoTemplate

@DisplayName("MongoTestController 단위 테스트")
class MongoTestControllerTest {

    private val mongoTemplate = mock(MongoTemplate::class.java)
    private val controller = MongoTestController(mongoTemplate)

    @Test
    @DisplayName("[happy] MongoDB 연결 성공 메시지를 반환한다")
    fun `MongoDB 연결 성공 메시지를 반환한다`() {
        val db = mock(MongoDatabase::class.java)
        `when`(mongoTemplate.db).thenReturn(db)
        `when`(db.name).thenReturn("test-db")

        val response = controller.testMongo()

        assertThat(response.data).isEqualTo("MongoDB 연결 성공: test-db")
        assertThat(response.success).isTrue()
    }
}
