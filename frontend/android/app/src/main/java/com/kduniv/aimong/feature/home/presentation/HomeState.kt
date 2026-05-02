package com.kduniv.aimong.feature.home.presentation

/**
 * 홈 화면 및 펫의 상태 정의 (기능 명세서 v2.3 섹션 13 반영)
 */
enum class HomeState {
    LOADING,          // 데이터 로딩 중
    HAPPY,            // 오늘 미션 1개 이상 완료 (활기찬 상태)
    IDLE,             // 오늘 미션 미완료 (일반 상태)
    SAD_LIGHT,        // 1일 미완료 (눈물방울)
    SAD_DEEP,         // 2일 이상 미완료 (회색 + 슬픈 표정)
    PET_EVOLVED,      // 방금 펫 진화 (알 -> 성장 or 성장 -> 아이몽)
    LEVEL_UP          // 가챠 횟수 구간 진입 (레벨업 팝업)
}
