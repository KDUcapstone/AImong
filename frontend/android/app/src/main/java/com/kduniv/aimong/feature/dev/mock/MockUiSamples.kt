package com.kduniv.aimong.feature.dev.mock

import com.kduniv.aimong.R
import com.kduniv.aimong.feature.home.presentation.HomePathItem
import com.kduniv.aimong.feature.home.presentation.HomeQuizNavigation
import com.kduniv.aimong.feature.home.presentation.HomeUiState
import com.kduniv.aimong.feature.home.presentation.QuestItemUiState

object MockUiSamples {

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
            heartCount = 3,
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
                add(
                    HomePathItem.SectionHeader(
                        stage = 1,
                        islandEmoji = "🏝️",
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
                        icon = "📖",
                        starsFilled = 3,
                    )
                )
                add(
                    HomePathItem.Completed(
                        order = 2,
                        title = "AI 란 무엇인가",
                        missionId = "mock-b",
                        quizNav = HomeQuizNavigation("", "mock-b", 1),
                        icon = "🤖",
                        starsFilled = 2,
                    )
                )
                add(
                    HomePathItem.Completed(
                        order = 3,
                        title = "데이터의 이해",
                        missionId = "mock-c",
                        quizNav = HomeQuizNavigation("", "mock-c", 1),
                        icon = "📊",
                        starsFilled = 1,
                    )
                )
                add(
                    HomePathItem.Completed(
                        order = 4,
                        title = "머신러닝의 기초",
                        missionId = "mock-d",
                        quizNav = HomeQuizNavigation("", "mock-d", 1),
                        icon = "🧠",
                        starsFilled = 0,
                    )
                )
                add(
                    HomePathItem.Completed(
                        order = 5,
                        title = "딥러닝 알아보기",
                        missionId = "mock-e",
                        quizNav = HomeQuizNavigation("", "mock-e", 1),
                        icon = "💡",
                        starsFilled = 0,
                    )
                )
                add(
                    HomePathItem.TodayStart(
                        quizNav = HomeQuizNavigation("100", "mock-mission-1", -1),
                        missionTitle = "오늘의 AI 탐험",
                        enabled = true,
                        icon = "🌟",
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
                        islandEmoji = "🌋",
                        islandName = "탐험의 화산섬",
                        progressCompleted = 0,
                        progressTotal = 10,
                        themeHint = "AI 잘 쓰기",
                        bannerDrawableRes = R.drawable.bg_home_section_banner_stage2,
                    )
                )
                add(HomePathItem.InterStageDivider)
                repeat(10) { add(HomePathItem.Locked(hint = "준비 중")) }
                add(
                    HomePathItem.SectionHeader(
                        stage = 3,
                        islandEmoji = "⭐",
                        islandName = "마스터의 별섬",
                        progressCompleted = 0,
                        progressTotal = 10,
                        themeHint = "비판적으로 생각하기",
                        bannerDrawableRes = R.drawable.bg_home_section_banner_stage3,
                    )
                )
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
