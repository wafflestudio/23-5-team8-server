package com.wafflestudio.team8server.preenroll.model

import com.wafflestudio.team8server.course.model.Course
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
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "pre_enrolls",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_pre_enrolls_user_course",
            columnNames = ["user_id", "course_id"],
        ),
    ],
)
class PreEnroll(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    val course: Course,
    @Column(name = "cart_count", nullable = false)
    var cartCount: Int = 0,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
