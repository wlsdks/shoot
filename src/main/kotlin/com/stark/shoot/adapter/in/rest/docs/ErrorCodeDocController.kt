package com.stark.shoot.adapter.`in`.rest.docs

import com.stark.shoot.adapter.`in`.rest.dto.ErrorCode
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ErrorCodeDocController {

    @Operation(
        summary = "에러 코드 목록 조회",
        description = """
            - 에러 코드 목록을 조회합니다.
            - 에러 코드는 `code`, `message`, `description` 필드를 가지고 있습니다.
        """
    )
    @GetMapping("/error-codes")
    fun getErrorCodes(): Map<String, Map<String, Any>> {
        return ErrorCode.getErrorCodeMap()
    }

}