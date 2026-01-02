package com.wafflestudio.team8server

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@Import(TestcontainersConfiguration::class)
@SpringBootTest
class Team8serverApplicationTests {

	@Test
	fun contextLoads() {
	}

}
