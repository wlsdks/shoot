package com.stark.shoot.adapter.`in`.rest.admin

import com.stark.shoot.application.service.monitoring.CDCHealthCheckService
import com.stark.shoot.application.service.monitoring.CDCHealthStatus
import com.stark.shoot.application.service.monitoring.ConnectorStatus
import com.stark.shoot.application.service.monitoring.ReplicationLagInfo
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * CDC 헬스체크 Admin API
 *
 * CDC 인프라 상태를 조회하는 관리자 엔드포인트입니다.
 *
 * **엔드포인트:**
 * - GET /api/admin/cdc/health - 전체 CDC 상태
 * - GET /api/admin/cdc/connector/status - Debezium 커넥터 상태
 * - GET /api/admin/cdc/replication/lag - Replication Slot 지연 정보
 */
@RestController
@RequestMapping("/api/admin/cdc")
class CDCHealthCheckController(
    private val cdcHealthCheckService: CDCHealthCheckService
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 전체 CDC 헬스 상태 조회
     *
     * @return CDC 전체 상태 (커넥터 + Replication)
     */
    @GetMapping("/health")
    fun getCDCHealth(): ResponseEntity<CDCHealthStatus> {
        return try {
            val status = cdcHealthCheckService.getCDCStatus()

            if (status.healthy) {
                ResponseEntity.ok(status)
            } else {
                ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(status)
            }

        } catch (e: Exception) {
            logger.error(e) { "CDC 헬스체크 조회 실패" }
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    /**
     * Debezium Connector 상태 조회
     *
     * @return 커넥터 상태
     */
    @GetMapping("/connector/status")
    fun getConnectorStatus(): ResponseEntity<ConnectorStatus> {
        return try {
            val status = cdcHealthCheckService.checkConnectorStatus()
            ResponseEntity.ok(status)

        } catch (e: Exception) {
            logger.error(e) { "커넥터 상태 조회 실패" }
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    /**
     * Replication Slot 지연 정보 조회
     *
     * @return Replication Lag 정보
     */
    @GetMapping("/replication/lag")
    fun getReplicationLag(): ResponseEntity<ReplicationLagInfo> {
        return try {
            val lag = cdcHealthCheckService.checkReplicationLag()
            ResponseEntity.ok(lag)

        } catch (e: Exception) {
            logger.error(e) { "Replication Lag 조회 실패" }
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}
