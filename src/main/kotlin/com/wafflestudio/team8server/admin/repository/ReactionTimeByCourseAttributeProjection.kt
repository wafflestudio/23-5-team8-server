package com.wafflestudio.team8server.admin.repository

interface ReactionTimeByCourseAttributeProjection {
    fun getAttribute(): String?

    fun getCount(): Long

    fun getAvgReactionTime(): Double

    fun getMinReactionTime(): Int

    fun getMaxReactionTime(): Int

    fun getSuccessCount(): Long
}

interface ReactionTimeByCourseNumberProjection : ReactionTimeByCourseAttributeProjection {
    fun getCourseName(): String?
}
