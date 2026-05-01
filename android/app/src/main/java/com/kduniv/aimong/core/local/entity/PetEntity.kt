package com.kduniv.aimong.core.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey val id: String,
    val type: String,
    val stage: Int, // 0: EGG, 1: BABY, 2: CHILD, 3: ADULT, 4: GUARDIAN
    val mood: String, // HAPPY, IDLE, SAD_LIGHT, SAD_DEEP
    val isEquipped: Boolean
)
