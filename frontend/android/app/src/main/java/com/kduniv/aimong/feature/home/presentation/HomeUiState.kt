package com.kduniv.aimong.feature.home.presentation

import com.kduniv.aimong.feature.mission.domain.model.MissionStarLevel

/** 홈 화면 UI 상태 (API 연동 전 기본값은 비어 있음) */
data class HomeUiState(
    val nickname: String = "",
    val totalXp: Int = 0,
    val streakDays: Int = 0,
    val profileType: String = "DEFAULT",
    val userLevel: Int = 1,
    
    // 펫 정보 및 상태
    val petName: String = "",
    val petXp: Int = 0,
    val petMaxXp: Int = 10,
    val petLevel: Int = 1,
    val petStage: String = "EGG",
    /** GET /home·/pet 의 equippedPet 없음 — XP 미적립 안내용 */
    val hasEquippedPet: Boolean = false,
    val homeState: HomeState = HomeState.IDLE,
    val petMessage: String = "",
    
    /** 상단 에너지 칩 — topStatus.energy / maxEnergy */
    val energyCurrent: Int = 0,
    val energyMax: Int = 20,
    /** GET /energy 의 missionStartCost (없으면 [DEFAULT_MISSION_START_COST]) */
    val missionStartCost: Int = DEFAULT_MISSION_START_COST,
    /** ISO8601, 에너지 바텀시트 등에서 표시 */
    val nextEnergyRecoverAt: String? = null,
    /** 상단 XP 칩 — topStatus.xp */
    val topStatusXp: Int = 0,

    /** 상단 톱니바퀴 칩 — GET /wallet */
    val gearBalance: Int = 0,
    val heartReviveCost: Int = WalletBalanceDefaults.HEART_REVIVE_COST,
    val streakShieldCost: Int = WalletBalanceDefaults.STREAK_SHIELD_COST,

    val normalTickets: Int = 0,
    val shieldCount: Int = 0,
    
    /**
     * 레어·에픽 티켓 장수 합(홈 요약용).
     * `POST /gacha/pull` 응답의 `srBonus`(확률 보정 실수)와는 무관하다.
     */
    val rareEpicTicketCount: Int = 0,
    val gachaDescription: String = "",

    // 오늘의 퀘스트
    val todayQuestProgress: String = "0/0",
    val quests: List<QuestItemUiState> = emptyList(),

    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** serverDate(KST)가 직전 저장값과 달라졌을 때 1회 안내 */
    val subtleNotice: String? = null,

    /** API 원본 보조 (추가 UI·디버그용) */
    val serverDate: String? = null,
    /** 상단 고정 티켓 요약(있으면 표시 규칙에 활용) */
    val topTicketCount: Int = 0,
    val canStartMission: Boolean = false,
    val returnRewardPending: Boolean = false,
    val dailyQuestClaimableCount: Int = 0,

    /** PM 시안 학습 경로 노드 */
    val pathItems: List<HomePathItem> = emptyList(),

    /** missionId → 별 난이도(1~3) — 난이도 피커 잠금/퀘스트 학습 진입에 사용 */
    val missionStarLevelsById: Map<String, List<MissionStarLevel>> = emptyMap(),

    /** missionId → 소단원(챕터 슬롯) 해금 — GET /missions·status 의 isUnlocked */
    val missionUnlockedById: Map<String, Boolean> = emptyMap(),
) {
    fun isMissionUnlocked(missionId: String): Boolean =
        missionId.isBlank() || missionUnlockedById[missionId] != false
    fun hasEnoughEnergyForMissionStart(): Boolean = energyCurrent >= missionStartCost

    /** 복습(REVIEW) 진입은 에너지 차감 없음 — v2.7 */
    fun canAttemptMissionStart(skipEnergyBecauseReview: Boolean = false): Boolean =
        canStartMission && (skipEnergyBecauseReview || hasEnoughEnergyForMissionStart())

    /** 난이도 피커를 열 수 있는지(복습 가능 난이도가 있으면 에너지 부족해도 PER_STAR 허용) */
    fun canOpenMissionPicker(
        unlockMode: DifficultyUnlockMode,
        starLevels: List<MissionStarLevel> = emptyList(),
    ): Boolean {
        if (!canStartMission) return false
        return when (unlockMode) {
            DifficultyUnlockMode.REVIEW -> true
            DifficultyUnlockMode.NEW_PLAY -> hasEnoughEnergyForMissionStart()
            DifficultyUnlockMode.PER_STAR ->
                hasEnoughEnergyForMissionStart() || starLevels.any { it.isReviewable }
        }
    }

    companion object {
        const val DEFAULT_MISSION_START_COST = 5
    }
}

object WalletBalanceDefaults {
    const val HEART_REVIVE_COST = 10
    const val STREAK_SHIELD_COST = 30
}

data class QuestItemUiState(
    val id: String,
    val title: String,
    val rewardSummary: String,
    val iconRes: Int?, // 실제 앱에서는 아이콘 리소스 ID
    val isCompleted: Boolean,
    val canStart: Boolean
)
