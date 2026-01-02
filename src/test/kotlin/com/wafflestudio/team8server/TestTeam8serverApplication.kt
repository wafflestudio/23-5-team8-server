package com.wafflestudio.team8server

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
	fromApplication<Team8serverApplication>().with(TestcontainersConfiguration::class).run(*args)
}
