package com.wafflestudio.team8server.user.repository

import com.wafflestudio.team8server.user.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Long>
