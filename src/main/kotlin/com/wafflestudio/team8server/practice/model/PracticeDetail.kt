package com.wafflestudio.team8server.practice.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "practice_details")
class PracticeDetail(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_id", nullable = false)
    val practiceLog: PracticeLog,
    @Column(name = "course_id", nullable = true)
    val courseId: Long?,
    @Column(name = "is_success", nullable = false)
    val isSuccess: Boolean,
    @Column(name = "reaction_time", nullable = false)
    val reactionTime: Int,
    @Column(name = "early_click_diff", nullable = true)
    val earlyClickDiff: Int? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
