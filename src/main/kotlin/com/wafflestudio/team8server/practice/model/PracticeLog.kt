package com.wafflestudio.team8server.practice.model

import com.wafflestudio.team8server.user.model.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "practice_logs")
class PracticeLog(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    @Column(name = "practice_at", nullable = false)
    val practiceAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "early_click_diff", nullable = true)
    var earlyClickDiff: Int? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
