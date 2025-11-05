package com.stark.shoot.infrastructure.config.domain

import com.stark.shoot.domain.shared.constants.DomainConstants
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * 도메인 설정 프로퍼티를 활성화하는 설정 클래스
 */
@Configuration
@EnableConfigurationProperties(DomainConstants::class)
class DomainPropertiesConfig