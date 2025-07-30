package com.stark.shoot.adapter.`in`.rest.notification

import com.stark.shoot.adapter.`in`.rest.dto.ResponseDto
import com.stark.shoot.adapter.`in`.rest.dto.notification.NotificationResponse
import com.stark.shoot.application.port.`in`.notification.NotificationQueryUseCase
import com.stark.shoot.application.port.`in`.notification.command.*
import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.notification.type.SourceType
import com.stark.shoot.domain.user.vo.UserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@Tag(name = "알림", description = "알림 관련 API")
@RestController
@RequestMapping("/api/v1/notifications")
class NotificationQueryController(
    private val notificationQueryUseCase: NotificationQueryUseCase
) {

    /**
     * Retrieves a paginated list of notifications for the authenticated user.
     *
     * @param limit The maximum number of notifications to return. Defaults to 20.
     * @param offset The number of notifications to skip before starting to collect the result set. Defaults to 0.
     * @return A response containing the user's notifications and a success message.
     */
    @Operation(
        summary = "알림 조회",
        description = """
            - 사용자의 알림을 조회합니다.
            - 기본적으로 최신 20개의 알림을 조회합니다.
            - limit과 offset을 사용하여 페이지네이션을 지원합니다.
        """
    )
    @GetMapping
    fun getNotifications(
        authentication: Authentication,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): ResponseDto<List<NotificationResponse>> {
        val userId = authentication.name.toLong()
        val command = GetNotificationsCommand(
            userId = UserId.from(userId),
            limit = limit,
            offset = offset
        )
        val notifications = notificationQueryUseCase.getNotificationsForUser(command)
        return ResponseDto.success(NotificationResponse.from(notifications), "알림이 성공적으로 조회되었습니다.")
    }


    /**
     * Retrieves a paginated list of unread notifications for the authenticated user.
     *
     * @param limit The maximum number of unread notifications to return. Defaults to 20.
     * @param offset The number of unread notifications to skip before starting to collect the result set. Defaults to 0.
     * @return A response containing the list of unread notifications.
     */
    @Operation(
        summary = "읽지 않은 알림 조회",
        description = """
            - 사용자의 읽지 않은 알림을 조회합니다.
            - 기본적으로 최신 20개의 읽지 않은 알림을 조회합니다.
            - limit과 offset을 사용하여 페이지네이션을 지원합니다.
        """
    )
    @GetMapping("/unread")
    fun getUnreadNotifications(
        authentication: Authentication,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): ResponseDto<List<NotificationResponse>> {
        val userId = authentication.name.toLong()
        val command = GetUnreadNotificationsCommand(
            userId = UserId.from(userId),
            limit = limit,
            offset = offset
        )
        val notifications = notificationQueryUseCase.getUnreadNotificationsForUser(command)
        return ResponseDto.success(NotificationResponse.from(notifications), "읽지 않은 알림이 성공적으로 조회되었습니다.")
    }


    /**
     * Retrieves the count of unread notifications for the authenticated user.
     *
     * @return A response containing the number of unread notifications.
     */
    @Operation(
        summary = "읽지 않은 알림 개수 조회",
        description = """
            - 사용자의 읽지 않은 알림 개수를 조회합니다.
        """
    )
    @GetMapping("/unread/count")
    fun getUnreadNotificationCount(authentication: Authentication): ResponseDto<Int> {
        val userId = authentication.name.toLong()
        val command = GetUnreadNotificationCountCommand(
            userId = UserId.from(userId)
        )
        val count = notificationQueryUseCase.getUnreadNotificationCount(command)
        return ResponseDto.success(count, "읽지 않은 알림 개수가 성공적으로 조회되었습니다.")
    }


    /**
     * Retrieves notifications for the authenticated user filtered by notification type, with pagination support.
     *
     * @param type The notification type to filter by.
     * @param limit The maximum number of notifications to return.
     * @param offset The number of notifications to skip before starting to collect the result set.
     * @return A response containing a list of notifications matching the specified type.
     */
    @Operation(
        summary = "알림 타입별 조회",
        description = """
            - 사용자의 알림을 타입별로 조회합니다.
            - 기본적으로 최신 20개의 알림을 조회합니다.
            - limit과 offset을 사용하여 페이지네이션을 지원합니다.
        """
    )
    @GetMapping("/type/{type}")
    fun getNotificationsByType(
        authentication: Authentication,
        @PathVariable type: String,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): ResponseDto<List<NotificationResponse>> {
        val userId = authentication.name.toLong()
        val notificationType = NotificationType.valueOf(type)
        val command = GetNotificationsByTypeCommand(
            userId = UserId.from(userId),
            type = notificationType,
            limit = limit,
            offset = offset
        )
        val notifications = notificationQueryUseCase.getNotificationsByType(command)
        return ResponseDto.success(NotificationResponse.from(notifications), "타입별 알림이 성공적으로 조회되었습니다.")
    }


    /**
     * Retrieves notifications for the authenticated user filtered by source type and optionally by source ID, with pagination support.
     *
     * @param sourceType The type of the notification source to filter by.
     * @param sourceId Optional identifier of the specific source to further filter notifications.
     * @param limit The maximum number of notifications to return.
     * @param offset The number of notifications to skip before starting to collect the result set.
     * @return A response containing a list of notifications matching the specified source criteria.
     */
    @Operation(
        summary = "알림 소스별 조회",
        description = """
            - 사용자의 알림을 소스별로 조회합니다.
            - 기본적으로 최신 20개의 알림을 조회합니다.
            - limit과 offset을 사용하여 페이지네이션을 지원합니다.
        """
    )
    @GetMapping("/source/{sourceType}")
    fun getNotificationsBySource(
        authentication: Authentication,
        @PathVariable sourceType: String,
        @RequestParam(required = false) sourceId: String?,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): ResponseDto<List<NotificationResponse>> {
        val userId = authentication.name.toLong()
        val source = SourceType.valueOf(sourceType)
        val command = GetNotificationsBySourceCommand(
            userId = UserId.from(userId),
            sourceType = source,
            sourceId = sourceId,
            limit = limit,
            offset = offset
        )
        val notifications = notificationQueryUseCase.getNotificationsBySource(command)
        return ResponseDto.success(NotificationResponse.from(notifications), "소스별 알림이 성공적으로 조회되었습니다.")
    }

}