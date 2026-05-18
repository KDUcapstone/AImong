package com.kduniv.aimong.feature.gacha

import com.kduniv.aimong.feature.pet.data.model.PetDto

data class GachaPetCardUi(
    val catalogPetType: String,
    val pet: PetDto?,
    val isLocked: Boolean,
    val isEquipped: Boolean = false,
    val displayName: String,
    val emoji: String,
    val grade: String,
    val levelLabel: String,
    val fragmentCount: Int,
    val fragmentThreshold: Int,
)
