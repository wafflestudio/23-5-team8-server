package com.wafflestudio.team8server.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun openApi(): OpenAPI {
        val securitySchemeName = "Bearer Authentication"

        return OpenAPI()
            .info(
                Info()
                    .title("Wafflestudio Team 8 Server API")
                    .description("Wafflestudio 23.5기 8조 서버 API 문서")
                    .version("v1.0.0"),
            ).components(
                Components()
                    .addSecuritySchemes(
                        securitySchemeName,
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("JWT 토큰을 입력하세요 (Bearer 접두사 제외)"),
                    ),
            )
    }
}
