package com.kduniv.aimong.feature.dev.mock

import com.kduniv.aimong.feature.home.presentation.HomePathItem
import com.kduniv.aimong.feature.home.presentation.HomeQuizNavigation
import com.kduniv.aimong.feature.home.presentation.HomeUiState
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
        return HomeUiState(
            nickname = "목업",
            streakDays = 5,
            profileType = "SPROUT",
            userLevel = 4,
            petName = "별이",
            petXp = 120,
            petMaxXp = 200,
            petLevel = 3,
            petMessage = "오늘도 AI 탐험 화이팅!",
            energyCurrent = mockEnergyCurrent,
            energyMax = MOCK_ENERGY_MAX,
            nextEnergyRecoverAt = null,
            topStatusXp = 1520,
            normalTickets = 2,
            topTicketCount = 4,
            gachaDescription = "목업: 실제 연동 시 서버 문구가 표시됩니다.",
            todayQuestProgress = "2/3",
            quests = listOf(
                QuestItemUiState("q1", "출석하기", "+10 XP", null, isCompleted = false, canStart = true),
                QuestItemUiState("q2", "친구와 대화", "+15 XP", null, isCompleted = true, canStart = false),
                QuestItemUiState("q3", "복습 미션", "+20 XP", null, isCompleted = false, canStart = false)
            ),
            pathItems = buildList {
                add(HomePathItem.SectionHeader(1, "AI가 뭐예요?"))
                add(
                    HomePathItem.Completed(
                        1,
                        "입문 미션 완료",
                        "mock-a",
                        HomeQuizNavigation("", "mock-a", 1),
                        "📖"
                    )
                )
                add(
                    HomePathItem.Completed(
                        2,
                        "AI 란 무엇인가",
                        "mock-b",
                        HomeQuizNavigation("", "mock-b", 1),
                        "🤖"
                    )
                )
                add(
                    HomePathItem.Completed(
                        3,
                        "데이터의 이해",
                        "mock-c",
                        HomeQuizNavigation("", "mock-c", 1),
                        "📊"
                    )
                )
                add(
                    HomePathItem.Completed(
                        4,
                        "머신러닝의 기초",
                        "mock-d",
                        HomeQuizNavigation("", "mock-d", 1),
                        "🧠"
                    )
                )
                add(
                    HomePathItem.Completed(
                        5,
                        "딥러닝 알아보기",
                        "mock-e",
                        HomeQuizNavigation("", "mock-e", 1),
                        "💡"
                    )
                )
                add(
                    HomePathItem.TodayStart(
                        quizNav = HomeQuizNavigation("100", "mock-mission-1", -1),
                        missionTitle = "오늘의 AI 탐험",
                        enabled = true,
                        icon = "🌟"
                    )
                )
                add(
                    HomePathItem.Review(
                        quizNav = HomeQuizNavigation("", "mock-mission-2", 2),
                        subtitle = "틀린 문제 복습"
                    )
                )
                add(HomePathItem.Locked(hint = "내일 열림"))
                add(HomePathItem.Locked(hint = "이후 오픈"))
                add(HomePathItem.Locked(hint = "다음 챕터"))
                add(HomePathItem.SectionHeader(2, "AI 잘 쓰기"))
                add(HomePathItem.InterStageDivider)
                repeat(10) { add(HomePathItem.Locked(hint = "준비 중")) }
                add(HomePathItem.SectionHeader(3, "비판적으로 생각하기"))
                add(HomePathItem.InterStageDivider)
                repeat(10) { add(HomePathItem.Locked(hint = "준비 중")) }
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
