package com.wafflestudio.team8server.config

import com.wafflestudio.team8server.common.auth.LoggedInUserIdResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Spring MVC 설정을 커스터마이징합니다.
 */
@Configuration
class WebMvcConfig(
    private val loggedInUserIdResolver: LoggedInUserIdResolver,
) : WebMvcConfigurer {
    /**
     * 커스텀 ArgumentResolver를 등록합니다.
     */
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(loggedInUserIdResolver)
    }
}
