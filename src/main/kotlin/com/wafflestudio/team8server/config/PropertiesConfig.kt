package com.wafflestudio.team8server.config

import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationPropertiesScan(basePackages = ["com.wafflestudio.team8server"])
class PropertiesConfig
