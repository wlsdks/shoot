package com.stark.shoot.adapter.`in`.rest.admin

import com.stark.shoot.adapter.out.persistence.postgres.entity.OutboxDeadLetterEntity
import com.stark.shoot.adapter.out.persistence.postgres.repository.OutboxDeadLetterRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/admin/outbox-dlq")
class OutboxDeadLetterController(
    private val deadLetterRepository: OutboxDeadLetterRepository
) {
    private val logger = KotlinLogging.logger {}

    @GetMapping
    fun getUnresolvedDLQ(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<OutboxDeadLetterEntity>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val dlqPage = deadLetterRepository.findByResolvedFalse(pageable)
        return ResponseEntity.ok(dlqPage)
    }

    @GetMapping("/{id}")
    fun getDLQById(@PathVariable id: Long): ResponseEntity<OutboxDeadLetterEntity> {
        val dlq = deadLetterRepository.findById(id).orElse(null) 
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(dlq)
    }

    @GetMapping("/saga/{sagaId}")
    fun getDLQBySagaId(@PathVariable sagaId: String): ResponseEntity<List<OutboxDeadLetterEntity>> {
        val dlqList = deadLetterRepository.findBySagaIdOrderByCreatedAtDesc(sagaId)
        return ResponseEntity.ok(dlqList)
    }

    @PostMapping("/{id}/resolve")
    fun resolveDLQ(@PathVariable id: Long, @RequestBody request: ResolveDLQRequest): ResponseEntity<OutboxDeadLetterEntity> {
        val dlq = deadLetterRepository.findById(id).orElse(null) 
            ?: return ResponseEntity.notFound().build()
        if (dlq.resolved) {
            return ResponseEntity.badRequest().build()
        }
        dlq.markAsResolved(request.resolvedBy, request.note)
        val savedDLQ = deadLetterRepository.save(dlq)
        logger.info { "DLQ 해결 처리: id=$id, resolvedBy=${request.resolvedBy}" }
        return ResponseEntity.ok(savedDLQ)
    }

    @GetMapping("/stats")
    fun getDLQStats(): ResponseEntity<DLQStatsResponse> {
        val unresolvedCount = deadLetterRepository.countByResolvedFalse()
        val last24hCount = deadLetterRepository.countDLQSince(Instant.now().minusSeconds(24 * 3600))
        val failuresByType = deadLetterRepository.getFailureStatsByEventType()
        val stats = DLQStatsResponse(
            unresolvedCount = unresolvedCount,
            last24hCount = last24hCount,
            failuresByType = failuresByType.map {
                EventTypeStats(eventType = it["eventType"] as String, count = (it["count"] as Long).toInt())
            }
        )
        return ResponseEntity.ok(stats)
    }

    @GetMapping("/recent")
    fun getRecentDLQ(): ResponseEntity<List<OutboxDeadLetterEntity>> {
        val recentDLQ = deadLetterRepository.findTop10ByResolvedFalseOrderByCreatedAtDesc()
        return ResponseEntity.ok(recentDLQ)
    }
}

data class ResolveDLQRequest(val resolvedBy: String, val note: String? = null)
data class DLQStatsResponse(val unresolvedCount: Long, val last24hCount: Long, val failuresByType: List<EventTypeStats>)
data class EventTypeStats(val eventType: String, val count: Int)
