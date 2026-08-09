package com.kduniv.aimong.feature.home

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val petStage: PetStage,   // EGG, GROWTH, AIMONG
        val petMood: PetMood,     // HAPPY, IDLE, SAD_LIGHT, SAD_DEEP
        val totalXp: Int,
        val continuousDays: Int,
        val tickets: TicketCount
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

enum class PetStage { EGG, GROWTH, AIMONG }
enum class PetMood { HAPPY, IDLE, SAD_LIGHT, SAD_DEEP }

data class TicketCount(
    val normal: Int,
    val rare: Int,
    val epic: Int
)
