package com.kduniv.aimong.core.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "child_profiles")
data class ChildProfileEntity(
    @PrimaryKey val id: String,
    val nickname: String,
    val xp: Int,
    val level: Int,
    val sessionVersion: Int,
    val code: String? = null
)
