package com.kduniv.aimong.feature.gacha

/** 장착 펫 스프라이트 표시용 (홈·퀴즈·스트릭 등 공통) */
data class EquippedPetVisual(
    val petType: String = "",
    val stage: String = "EGG",
    val grade: String = "NORMAL",
)
