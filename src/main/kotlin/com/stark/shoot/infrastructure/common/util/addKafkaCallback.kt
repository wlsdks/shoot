package com.stark.shoot.infrastructure.common.util

import org.springframework.kafka.support.SendResult
import java.util.concurrent.CompletableFuture

// Kafka 메시지 전송 결과 처리
fun <K, V> CompletableFuture<SendResult<K, V>>.handleCompletion(
    onSuccess: (SendResult<K, V>) -> Unit = {},
    onFailure: (Throwable) -> Unit = {}
) {
    whenComplete { result, ex ->
        when {
            ex != null -> onFailure(ex)
            else -> onSuccess(result)
        }
    }
}