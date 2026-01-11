package com.wafflestudio.team8server.practice.model

import com.wafflestudio.team8server.course.model.Course
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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = true)
    val course: Course?,
    @Column(name = "is_success", nullable = false)
    val isSuccess: Boolean,
    @Column(name = "reaction_time", nullable = false)
    val reactionTime: Int,
    @Column(name = "early_click_diff", nullable = true)
    val earlyClickDiff: Int? = null,
    @Column(name = "user_rank", nullable = true)
    val rank: Int? = null,
    @Column(name = "percentile", nullable = true)
    val percentile: Double? = null,
    @Column(name = "capacity", nullable = false)
    val capacity: Int,
    @Column(name = "total_competitors", nullable = false)
    val totalCompetitors: Int,
    @Column(name = "distribution_scale", nullable = false)
    val distributionScale: Double,
    @Column(name = "distribution_shape", nullable = false)
    val distributionShape: Double,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
