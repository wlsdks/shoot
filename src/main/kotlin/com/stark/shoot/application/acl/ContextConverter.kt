package com.stark.shoot.application.acl

/**
 * Context Converter Interface
 *
 * DDD Anti-Corruption Layer (ACL) 패턴의 표준 인터페이스
 *
 * 목적:
 * - Bounded Context 간 타입 변환 표준화
 * - Context 독립성 보장
 * - MSA 전환 시 API 경계에서 DTO 변환 역할
 *
 * @param S Source Context의 타입
 * @param T Target Context의 타입
 */
interface ContextConverter<S, T> {

    /**
     * Source Context의 값을 Target Context로 변환
     *
     * @param source Source Context의 값
     * @return Target Context로 변환된 값
     */
    fun convert(source: S): T

    /**
     * Target Context의 값을 Source Context로 역변환
     *
     * @param target Target Context의 값
     * @return Source Context로 변환된 값
     */
    fun convertBack(target: T): S
}
