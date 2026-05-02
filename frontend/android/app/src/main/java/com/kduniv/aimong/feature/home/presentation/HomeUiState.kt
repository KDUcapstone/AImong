package com.kduniv.aimong.feature.home.presentation

/**
 * 홈 화면 UI 상태 (디자인 시안 v1.3 반영)
 */
data class HomeUiState(
    val nickname: String = "",
    val totalXp: Int = 0,
    val streakDays: Int = 0,
    val profileType: String = "DEFAULT",
    val userLevel: Int = 1,
    
    // 펫 정보 및 상태
    val petName: String = "",
    val petXp: Int = 0,
    val petMaxXp: Int = 500,
    val petLevel: Int = 1,
    val petStage: String = "EGG",
    val homeState: HomeState = HomeState.IDLE,
    val petMessage: String = "오늘도 같이 공부해요! 😊",
    
    // 에너지 및 재화 현황
    val normalTickets: Int = 0,
    val shieldCount: Int = 0,
    val energyCount: Int = 0, // 상단 번개 아이콘 대응
    
    // 가챠 관련
    val srBonus: Int = 0,
    val gachaDescription: String = "레전드 확률 4% (Lv.7)",
    
    // 오늘의 퀘스트
    val todayQuestProgress: String = "0/3",
    val quests: List<QuestItemUiState> = emptyList()
)

data class QuestItemUiState(
    val id: String,
    val title: String,
    val rewardSummary: String,
    val iconRes: Int?, // 실제 앱에서는 아이콘 리소스 ID
    val isCompleted: Boolean,
    val canStart: Boolean
)
