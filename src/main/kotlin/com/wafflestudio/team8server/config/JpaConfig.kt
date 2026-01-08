package com.wafflestudio.team8server.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@Configuration
@EnableJpaAuditing // JPA Auditing 활성화 (BaseEntity의 타임스탬프 자동 관리)
class JpaConfig
