package com.kduniv.aimong.feature.dev.mock

import com.kduniv.aimong.R
import com.kduniv.aimong.feature.home.presentation.HomePathItem
import com.kduniv.aimong.feature.home.presentation.HomeQuizNavigation
import com.kduniv.aimong.feature.gacha.GachaPetCatalog
import com.kduniv.aimong.feature.home.presentation.HomeUiState
import com.kduniv.aimong.feature.home.presentation.WalletBalanceDefaults
import com.kduniv.aimong.feature.home.presentation.QuestItemUiState

object MockUiSamples {

    /** 목업 홈·에너지 시트에서만 사용. `addMockEnergy`로 변경 가능 */
    var mockEnergyCurrent: Int = 12
        private set

    const val MOCK_ENERGY_MAX: Int = 20

    fun addMockEnergy(amount: Int) {
        mockEnergyCurrent = (mockEnergyCurrent + amount).coerceIn(0, MOCK_ENERGY_MAX)
    }

    fun homeUiState(): HomeUiState {
        val userXp = MockXpLedger.userTotalXp
        val equipped = StubPetGachaStore.getPetList().equippedPet
        val petType = equipped?.petType?.takeIf { it.isNotBlank() } ?: "pet_normal_002"
        val petGrade = equipped?.grade?.takeIf { it.isNotBlank() } ?: "NORMAL"
        val petStage = equipped?.stage?.takeIf { it.isNotBlank() } ?: "GROWTH"
        val petLevel = equipped?.let { (it.xp / 10).coerceAtLeast(1) } ?: 1
        return HomeUiState(
            nickname = "목업",
            totalXp = userXp,
            streakDays = 5,
            profileType = "SPROUT",
            userLevel = 1 + (userXp / 80).coerceIn(0, 99),
            petName = GachaPetCatalog.displayNameFor(petType, petGrade),
            petXp = MockXpLedger.petXp,
            petMaxXp = 10,
            hasEquippedPet = equipped != null,
            equippedPetType = petType,
            equippedPetGrade = petGrade,
            petStage = petStage,
            petLevel = petLevel,
            petMessage = "오늘도 AI 탐험 화이팅!",
            energyCurrent = mockEnergyCurrent,
            energyMax = MOCK_ENERGY_MAX,
            gearBalance = MockGearBalance.gear,
            heartReviveCost = WalletBalanceDefaults.HEART_REVIVE_COST,
            streakShieldCost = WalletBalanceDefaults.STREAK_SHIELD_COST,
            missionStartCost = HomeUiState.DEFAULT_MISSION_START_COST,
            nextEnergyRecoverAt = null,
            topStatusXp = userXp,
            normalTickets = 2,
            topTicketCount = 4,
            canStartMission = true,
            todayQuestProgress = "2/3",
            quests = listOf(
                QuestItemUiState("q1", "출석하기", "+10 XP", null, isCompleted = false, canStart = true),
                QuestItemUiState("q2", "친구와 대화", "+15 XP", null, isCompleted = true, canStart = false),
                QuestItemUiState("q3", "복습 미션", "+20 XP", null, isCompleted = false, canStart = false)
            ),
            pathItems = buildList {
                add(
                    HomePathItem.SectionHeader(
                        stage = 1,
                        islandIconRes = R.drawable.ic_nav_home_color,
                        islandName = "시작의 섬",
                        progressCompleted = 3,
                        progressTotal = 5,
                        themeHint = "AI가 뭐예요?",
                        bannerDrawableRes = R.drawable.bg_home_section_banner_stage1,
                    )
                )
                add(
                    HomePathItem.Completed(
                        order = 1,
                        title = "입문 미션 완료",
                        missionId = "mock-a",
                        quizNav = HomeQuizNavigation("", "mock-a", 1),
                        starsFilled = 3,
                    )
                )
                add(
                    HomePathItem.Completed(
                        order = 2,
                        title = "AI 란 무엇인가",
                        missionId = "mock-b",
                        quizNav = HomeQuizNavigation("", "mock-b", 1),
                        starsFilled = 2,
                    )
                )
                add(
                    HomePathItem.Completed(
                        order = 3,
                        title = "데이터의 이해",
                        missionId = "mock-c",
                        quizNav = HomeQuizNavigation("", "mock-c", 1),
                        starsFilled = 1,
                    )
                )
                add(
                    HomePathItem.Completed(
                        order = 4,
                        title = "머신러닝의 기초",
                        missionId = "mock-d",
                        quizNav = HomeQuizNavigation("", "mock-d", 1),
                        starsFilled = 0,
                    )
                )
                add(
                    HomePathItem.Completed(
                        order = 5,
                        title = "딥러닝 알아보기",
                        missionId = "mock-e",
                        quizNav = HomeQuizNavigation("", "mock-e", 1),
                        starsFilled = 0,
                    )
                )
                add(
                    HomePathItem.TodayStart(
                        quizNav = HomeQuizNavigation("100", "mock-mission-1", -1),
                        missionTitle = "오늘의 AI 탐험",
                        enabled = true,
                        starsFilled = 0,
                    )
                )
                add(
                    HomePathItem.Review(
                        quizNav = HomeQuizNavigation("", "mock-mission-2", 2),
                        subtitle = "틀린 문제 복습",
                        starsFilled = 2,
                    )
                )
                add(HomePathItem.Locked(hint = "내일 열림"))
                add(HomePathItem.Locked(hint = "이후 오픈"))
                add(HomePathItem.Locked(hint = "다음 챕터"))
                add(
                    HomePathItem.SectionHeader(
                        stage = 2,
                        islandIconRes = R.drawable.ic_nav_ai_color,
                        islandName = "탐험의 화산섬",
                        progressCompleted = 0,
                        progressTotal = 1,
                        themeHint = "AI 잘 쓰기",
                        bannerDrawableRes = R.drawable.bg_home_section_banner_stage2,
                    )
                )
                add(
                    HomePathItem.Start(
                        quizNav = HomeQuizNavigation("", "mock-stage2", 1),
                        missionTitle = "2단계 체험 미션",
                        enabled = true,
                        starsFilled = 0,
                    )
                )
                add(HomePathItem.InterStageDivider)
                add(
                    HomePathItem.SectionHeader(
                        stage = 3,
                        islandIconRes = R.drawable.ic_nav_study_color,
                        islandName = "마스터의 별섬",
                        progressCompleted = 0,
                        progressTotal = 1,
                        themeHint = "비판적으로 생각하기",
                        bannerDrawableRes = R.drawable.bg_home_section_banner_stage3,
                    )
                )
                add(
                    HomePathItem.Start(
                        quizNav = HomeQuizNavigation("", "mock-stage3", 1),
                        missionTitle = "3단계 체험 미션",
                        enabled = true,
                        starsFilled = 0,
                    )
                )
            }
        )
    }

    fun profileLabel(type: String): String {
        return when (type) {
            "SPROUT" -> "AI 새싹"
            "EXPLORER" -> "AI 탐험가"
            "CRITIC" -> "AI 비평가"
            "GUARDIAN" -> "AI 수호자"
            else -> "AI 입문자"
        }
    }
}
