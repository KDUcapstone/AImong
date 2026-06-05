package com.kduniv.aimong.feature.home.domain

/** 홈 학습 경로·단계 보상 팝업 등에 쓰는 섹션(챕터) 제목 */
object HomeStageTitles {
    private val BY_STAGE = mapOf(
        1 to "AI 알아보기",
        2 to "AI 안전하게 쓰고 판단하기",
        3 to "AI에게 잘 질문하고 활용하기",
    )

    fun title(stage: Int): String = BY_STAGE[stage] ?: (stage.toString() + "\uB2E8\uACC4")
}
