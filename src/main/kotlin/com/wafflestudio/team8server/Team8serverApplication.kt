package com.wafflestudio.team8server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
class Team8serverApplication

fun main(args: Array<String>) {
    runApplication<Team8serverApplication>(*args)
}
