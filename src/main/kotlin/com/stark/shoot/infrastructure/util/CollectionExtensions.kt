package com.stark.shoot.infrastructure.util

/**
 * 컬렉션 확장 함수들
 */

/**
 * Set에 요소를 안전하게 추가 (이미 존재하지 않는 경우에만)
 */
fun <T> Set<T>.addIfNotExists(element: T): Set<T> =
    if (contains(element)) this else this + element

/**
 * Set에서 요소를 안전하게 제거 (존재하는 경우에만)
 */
fun <T> Set<T>.removeIfExists(element: T): Set<T> =
    if (contains(element)) this - element else this

/**
 * Map에 키-값 쌍을 안전하게 추가/업데이트
 */
fun <K, V> Map<K, V>.putIfNotNull(key: K, value: V?): Map<K, V> =
    value?.let { this + (key to it) } ?: this

/**
 * 컬렉션이 비어있지 않을 때만 주어진 동작 실행
 */
inline fun <T, C : Collection<T>> C.ifNotEmpty(action: (C) -> Unit): C {
    if (isNotEmpty()) action(this)
    return this
}