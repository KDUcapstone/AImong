package com.kduniv.aimong.feature.home.presentation

import com.kduniv.aimong.feature.mission.domain.model.MissionStarLevel
import com.kduniv.aimong.feature.pet.domain.PetGrowthRules

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
    val petMaxXp: Int = PetGrowthRules.EGG_EVOLUTION_XP,
    val petLevel: Int = 1,
    val petStage: String = "EGG",
    /** 아이몽(Lv.3) 등 성장 종료 단계 — XP 바 미표시 */
    val showPetXpProgress: Boolean = true,
    val petCrownUnlocked: Boolean = false,
    /** 장착 펫 PNG — `pet_normal_001` 형식 */
    val equippedPetType: String = "",
    val equippedPetGrade: String = "NORMAL",
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

    /** GET /home `tickets.normal` — 가챠·보상 동기화용 */
    val normalTickets: Int = 0,
    val shieldCount: Int = 0,

    // 오늘의 퀘스트
    val todayQuestProgress: String = "0/0",
    val quests: List<QuestItemUiState> = emptyList(),

    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    /** serverDate(KST)가 직전 저장값과 달라졌을 때 1회 안내 */
    val subtleNotice: String? = null,

    /** API 원본 보조 (추가 UI·디버그용) */
    val serverDate: String? = null,
    /** 상단 뽑기 티켓 칩 — `topStatus.ticketCount` / `tickets.normal` (v2.3) */
    val topTicketCount: Int = 0,
    val canStartMission: Boolean = false,
    val returnRewardPending: Boolean = false,
    /** GET /home `dailyQuestSummary.claimableCount` — 수령 가능한 일일 퀘스트 */
    val dailyQuestClaimableCount: Int = 0,
    /** GET /quests/child/custom `hasPendingConfirm` — 부모 확인 대기 커스텀 퀘스트 */
    val hasPendingCustomQuest: Boolean = false,
    /**
     * 퀘스트 시트를 한 번 열어 확인했으면 true.
     * 알림 개수가 0이 되기 전까지 홈 FAB·시트 탭 배지를 다시 띄우지 않습니다.
     */
    val questNotificationsAcknowledged: Boolean = false,

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

    /**
     * 난이도 피커 표시 여부.
     * [canStartMission]·로컬 에너지로 막지 않는다 — 경로 UI와 /home 갱신 타이밍이 어긋나면
     * “클릭은 되는데 팝업만 안 뜨는” 상태가 생기기 때문.
     * 실제 진입 가능 여부는 [HomeViewModel.validateMissionQuizNav]에서 서버 status·에너지로 검증.
     */
    @Suppress("UNUSED_PARAMETER")
    fun canOpenMissionPicker(
        unlockMode: DifficultyUnlockMode,
        starLevels: List<MissionStarLevel> = emptyList(),
    ): Boolean = true

    /** 홈 퀘스트 FAB에 표시할 알림 개수 (미완료 퀘스트 수가 아님) */
    fun questNotificationCount(): Int =
        dailyQuestClaimableCount + if (hasPendingCustomQuest) 1 else 0

    fun shouldShowQuestFabBadge(): Boolean =
        questNotificationCount() > 0 && !questNotificationsAcknowledged

    /** 확인 후에도 미처리 알림이 남아 있을 때 시트 탭 배지를 숨깁니다. */
    fun shouldSuppressQuestSheetTabBadges(): Boolean =
        questNotificationsAcknowledged && questNotificationCount() > 0

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
