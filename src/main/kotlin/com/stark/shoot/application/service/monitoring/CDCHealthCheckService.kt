package com.stark.shoot.application.service.monitoring

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.application.port.out.notification.SlackNotificationPort
import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import javax.sql.DataSource

/**
 * CDC 인프라 헬스체크 서비스
 *
 * Debezium 커넥터, PostgreSQL Replication Slot 등 CDC 인프라 상태를 모니터링합니다.
 *
 * **모니터링 항목:**
 * 1. Debezium Connector 상태 (RUNNING/FAILED)
 * 2. Connector Task 상태
 * 3. PostgreSQL Replication Slot 지연 (lag)
 * 4. Publication 상태
 */
@Service
class CDCHealthCheckService(
    private val slackNotificationPort: SlackNotificationPort,
    private val dataSource: DataSource,
    private val objectMapper: ObjectMapper,
    @Value("\${cdc.kafka-connect.url:http://localhost:8083}")
    private val kafkaConnectUrl: String,
    @Value("\${cdc.connector.name:shoot-outbox-connector}")
    private val connectorName: String,
    @Value("\${cdc.replication-slot.name:shoot_outbox_slot}")
    private val replicationSlotName: String,
    @Value("\${cdc.replication-slot.lag-threshold-mb:10}")
    private val lagThresholdMb: Long
) {
    private val logger = KotlinLogging.logger {}
    private val restTemplate = RestTemplate()

    /**
     * CDC 커넥터 헬스체크
     * 매 5분마다 실행
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000)  // 5분
    @SchedulerLock(name = "cdcHealthCheck", lockAtMostFor = "4m", lockAtLeastFor = "1s")
    fun checkCDCHealth() {
        try {
            // 1. Debezium Connector 상태 확인
            val connectorStatus = checkConnectorStatus()

            // 2. Replication Slot 지연 확인
            val replicationLag = checkReplicationLag()

            // 3. 헬스체크 결과 로깅
            logger.info {
                "CDC 헬스체크 완료 - " +
                "커넥터: ${connectorStatus.state}, " +
                "복제 지연: ${replicationLag.lagMb}MB"
            }

            // 4. 이상 감지 시 알림
            if (connectorStatus.state != "RUNNING") {
                notifyConnectorFailure(connectorStatus)
            }

            if (replicationLag.lagMb > lagThresholdMb) {
                notifyReplicationLag(replicationLag)
            }

        } catch (e: Exception) {
            logger.error(e) { "CDC 헬스체크 실패" }
        }
    }

    /**
     * Debezium Connector 상태 확인
     */
    fun checkConnectorStatus(): ConnectorStatus {
        return try {
            val url = "$kafkaConnectUrl/connectors/$connectorName/status"
            val response = restTemplate.getForObject(url, String::class.java)
            val json = objectMapper.readTree(response)

            val connectorState = json.get("connector")?.get("state")?.asText() ?: "UNKNOWN"
            val tasks = json.get("tasks")?.map { task ->
                TaskStatus(
                    id = task.get("id")?.asInt() ?: -1,
                    state = task.get("state")?.asText() ?: "UNKNOWN",
                    workerId = task.get("worker_id")?.asText() ?: "UNKNOWN"
                )
            } ?: emptyList()

            ConnectorStatus(
                name = connectorName,
                state = connectorState,
                tasks = tasks
            )

        } catch (e: Exception) {
            logger.error(e) { "커넥터 상태 확인 실패: $connectorName" }
            ConnectorStatus(
                name = connectorName,
                state = "UNREACHABLE",
                tasks = emptyList(),
                error = e.message
            )
        }
    }

    /**
     * PostgreSQL Replication Slot 지연 확인
     */
    fun checkReplicationLag(): ReplicationLagInfo {
        return try {
            dataSource.connection.use { conn ->
                val stmt = conn.prepareStatement(
                    """
                    SELECT
                        slot_name,
                        active,
                        pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn) AS lag_bytes,
                        pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn) / 1024 / 1024 AS lag_mb
                    FROM pg_replication_slots
                    WHERE slot_name = ?
                    """.trimIndent()
                )
                stmt.setString(1, replicationSlotName)

                val rs = stmt.executeQuery()
                if (rs.next()) {
                    ReplicationLagInfo(
                        slotName = rs.getString("slot_name"),
                        active = rs.getBoolean("active"),
                        lagBytes = rs.getLong("lag_bytes"),
                        lagMb = rs.getLong("lag_mb")
                    )
                } else {
                    ReplicationLagInfo(
                        slotName = replicationSlotName,
                        active = false,
                        lagBytes = -1,
                        lagMb = -1,
                        error = "Replication slot not found"
                    )
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Replication Slot 조회 실패" }
            ReplicationLagInfo(
                slotName = replicationSlotName,
                active = false,
                lagBytes = -1,
                lagMb = -1,
                error = e.message
            )
        }
    }

    /**
     * 전체 CDC 상태 조회 (API 엔드포인트용)
     */
    fun getCDCStatus(): CDCHealthStatus {
        val connectorStatus = checkConnectorStatus()
        val replicationLag = checkReplicationLag()

        val isHealthy = connectorStatus.state == "RUNNING"
            && replicationLag.active
            && replicationLag.lagMb < lagThresholdMb

        return CDCHealthStatus(
            healthy = isHealthy,
            connector = connectorStatus,
            replication = replicationLag,
            checkedAt = java.time.Instant.now()
        )
    }

    /**
     * 커넥터 장애 알림
     */
    private fun notifyConnectorFailure(status: ConnectorStatus) {
        val message = """
            커넥터: ${status.name}
            상태: ${status.state}
            태스크: ${status.tasks.joinToString { "task-${it.id}: ${it.state}" }}
            ${status.error?.let { "에러: $it" } ?: ""}
        """.trimIndent()

        slackNotificationPort.notifyError(
            title = "CDC 커넥터 장애",
            message = message
        )

        logger.error { "CDC 커넥터 장애 - $message" }
    }

    /**
     * Replication Lag 알림
     */
    private fun notifyReplicationLag(lag: ReplicationLagInfo) {
        val message = """
            Replication Slot: ${lag.slotName}
            지연: ${lag.lagMb} MB (임계값: ${lagThresholdMb} MB)
            활성: ${lag.active}
            ${lag.error?.let { "에러: $it" } ?: ""}
        """.trimIndent()

        slackNotificationPort.notifyError(
            title = "CDC Replication Lag 임계값 초과",
            message = message
        )

        logger.warn { "CDC Replication Lag 높음 - $message" }
    }
}

/**
 * Connector 상태 정보
 */
data class ConnectorStatus(
    val name: String,
    val state: String,  // RUNNING, FAILED, PAUSED, UNASSIGNED
    val tasks: List<TaskStatus>,
    val error: String? = null
)

/**
 * Task 상태 정보
 */
data class TaskStatus(
    val id: Int,
    val state: String,  // RUNNING, FAILED, PAUSED
    val workerId: String
)

/**
 * Replication Lag 정보
 */
data class ReplicationLagInfo(
    val slotName: String,
    val active: Boolean,
    val lagBytes: Long,
    val lagMb: Long,
    val error: String? = null
)

/**
 * 전체 CDC 헬스 상태
 */
data class CDCHealthStatus(
    val healthy: Boolean,
    val connector: ConnectorStatus,
    val replication: ReplicationLagInfo,
    val checkedAt: java.time.Instant
)
