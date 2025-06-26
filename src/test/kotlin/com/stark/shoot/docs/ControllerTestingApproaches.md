# Controller Testing Approaches

## 현재 프로젝트의 컨트롤러 테스트 접근 방식

현재 프로젝트에서는 컨트롤러를 테스트하기 위해 **단위 테스트 접근 방식**을 사용하고 있습니다. 이 방식은 다음과 같은 특징이 있습니다:

1. 컨트롤러를 직접 인스턴스화하고 의존성을 모킹(mocking)합니다.
2. 컨트롤러 메서드를 직접 호출하고 반환값을 검증합니다.
3. Spring 컨텍스트를 로드하지 않습니다.
4. HTTP 요청/응답을 시뮬레이션하지 않습니다.

예시:
```kotlin
class MessageControllerTest {
    private val editMessageUseCase = mock(EditMessageUseCase::class.java)
    private val controller = MessageController(editMessageUseCase)

    @Test
    fun `메시지 편집 요청을 처리하고 수정된 메시지를 반환한다`() {
        // given
        val request = EditMessageRequest("id", "content")
        val mockMessage = mock(ChatMessage::class.java)
        `when`(editMessageUseCase.editMessage(any(), any())).thenReturn(mockMessage)

        // when
        val response = controller.editMessage(request)

        // then
        assertThat(response.success).isTrue()
        verify(editMessageUseCase).editMessage(any(), any())
    }
}
```

## 대안: MockMvc를 사용한 통합 테스트 접근 방식

Spring에서는 `MockMvc`를 사용한 통합 테스트 접근 방식도 널리 사용됩니다. 이 방식은 다음과 같은 특징이 있습니다:

1. `@WebMvcTest` 어노테이션을 사용하여 Spring MVC 컨텍스트를 로드합니다.
2. `MockMvc`를 사용하여 실제 HTTP 요청을 시뮬레이션합니다.
3. `@MockBean`을 사용하여 Spring 컨텍스트에 모의 객체를 등록합니다.
4. 보안, 유효성 검사, 직렬화/역직렬화 등 웹 계층의 모든 측면을 테스트할 수 있습니다.

예시:
```kotlin
@WebMvcTest(MessageController::class)
class MessageControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var editMessageUseCase: EditMessageUseCase

    @Test
    fun `메시지 편집 요청을 처리하고 수정된 메시지를 반환한다`() {
        // given
        val request = EditMessageRequest("id", "content")
        val mockMessage = mock(ChatMessage::class.java)
        `when`(editMessageUseCase.editMessage(any(), any())).thenReturn(mockMessage)

        // when & then
        mockMvc.perform(
            post("/api/messages/edit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }
}
```

## 두 접근 방식의 비교

### 단위 테스트 접근 방식 (현재 사용 중)

**장점:**
- **빠른 실행 속도**: Spring 컨텍스트를 로드하지 않아 테스트 실행이 매우 빠릅니다.
- **간단한 설정**: 의존성만 모킹하면 되므로 설정이 간단합니다.
- **격리된 테스트**: 컨트롤러 로직만 테스트하므로 다른 계층의 문제가 테스트에 영향을 주지 않습니다.
- **의존성 주입 검증**: 컨트롤러가 의존성을 올바르게 사용하는지 쉽게 검증할 수 있습니다.

**단점:**
- **HTTP 계층 테스트 부재**: 실제 HTTP 요청/응답 처리, 직렬화/역직렬화, 유효성 검사 등을 테스트하지 않습니다.
- **보안 검증 부재**: 인증/인가 관련 로직을 테스트하지 않습니다.
- **통합 문제 발견 어려움**: 컨트롤러와 다른 계층 간의 통합 문제를 발견하기 어렵습니다.

### MockMvc 접근 방식

**장점:**
- **HTTP 계층 테스트**: 실제 HTTP 요청/응답 처리, 직렬화/역직렬화, 유효성 검사 등을 테스트합니다.
- **보안 검증**: 인증/인가 관련 로직을 테스트할 수 있습니다.
- **통합 문제 발견**: 컨트롤러와 다른 계층 간의 통합 문제를 발견할 수 있습니다.
- **실제 환경과 유사**: 실제 애플리케이션 동작과 더 유사한 테스트를 수행합니다.

**단점:**
- **느린 실행 속도**: Spring 컨텍스트를 로드해야 하므로 테스트 실행이 느립니다.
- **복잡한 설정**: Spring 컨텍스트 설정, 보안 설정 등이 필요하여 설정이 복잡합니다.
- **의존성 문제**: 다른 계층의 문제가 테스트에 영향을 줄 수 있습니다.

## 어떤 접근 방식이 더 좋은가?

두 접근 방식 모두 장단점이 있으며, 프로젝트의 요구사항과 팀의 선호도에 따라 선택할 수 있습니다. 많은 프로젝트에서는 두 접근 방식을 모두 사용하는 하이브리드 전략을 채택합니다:

1. **단위 테스트**: 컨트롤러의 비즈니스 로직과 의존성 사용을 검증합니다.
2. **통합 테스트**: 주요 API 엔드포인트에 대해 MockMvc를 사용하여 전체 웹 계층을 검증합니다.

현재 프로젝트에서는 단위 테스트 접근 방식을 사용하고 있으며, 이는 다음과 같은 이유로 유효한 선택입니다:

1. **빠른 피드백 루프**: 테스트가 빠르게 실행되어 개발 생산성이 향상됩니다.
2. **간단한 테스트 작성**: 테스트 작성이 간단하여 테스트 커버리지를 쉽게 높일 수 있습니다.
3. **의존성 주입 검증**: 컨트롤러가 의존성을 올바르게 사용하는지 검증할 수 있습니다.

그러나 프로젝트가 성장함에 따라 일부 중요한 API 엔드포인트에 대해 MockMvc 기반 통합 테스트를 추가하는 것을 고려할 수 있습니다. 이를 위해서는 다음과 같은 의존성을 추가해야 합니다:

```kotlin
// build.gradle.kts
dependencies {
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test") // 보안 관련 테스트를 위한 의존성
}
```

## 결론

현재 프로젝트에서 사용 중인 단위 테스트 접근 방식은 완전히 유효하며, 많은 프로젝트에서 사용되는 표준적인 방법입니다. 이 접근 방식은 빠른 피드백 루프와 간단한 테스트 작성을 제공하여 개발 생산성을 향상시킵니다.

그러나 프로젝트의 요구사항에 따라 일부 중요한 API 엔드포인트에 대해 MockMvc 기반 통합 테스트를 추가하는 것을 고려할 수 있습니다. 이는 HTTP 계층, 보안, 직렬화/역직렬화 등을 포함한 전체 웹 계층을 검증하는 데 도움이 됩니다.

두 접근 방식은 상호 보완적이며, 프로젝트의 요구사항과 팀의 선호도에 따라 적절한 조합을 선택할 수 있습니다.
