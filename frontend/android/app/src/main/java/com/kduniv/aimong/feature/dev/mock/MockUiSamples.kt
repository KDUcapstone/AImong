package com.kduniv.aimong.feature.dev.mock

import com.kduniv.aimong.feature.home.presentation.HomePathItem
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
                add(HomePathItem.SectionHeader(1, "AI가 뭐예요?"))
                add(HomePathItem.Completed(1, "입문 미션 완료", "📖"))
                add(HomePathItem.Completed(2, "AI 란 무엇인가", "🤖"))
                add(HomePathItem.Completed(3, "데이터의 이해", "📊"))
                add(HomePathItem.Completed(4, "머신러닝의 기초", "🧠"))
                add(HomePathItem.Completed(5, "딥러닝 알아보기", "💡"))
                add(
                    HomePathItem.TodayStart(
                        missionId = "mock-mission-1",
                        missionTitle = "오늘의 AI 탐험",
                        enabled = true,
                        icon = "🌟"
                    )
                )
                add(HomePathItem.Review(missionId = "mock-mission-2", subtitle = "틀린 문제 복습"))
                add(HomePathItem.Locked(hint = "내일 열림"))
                add(HomePathItem.Locked(hint = "이후 오픈"))
                add(HomePathItem.Locked(hint = "다음 챕터"))
                add(HomePathItem.SectionHeader(2, "AI 잘 쓰기"))
                repeat(10) { add(HomePathItem.Locked(hint = "준비 중")) }
                add(HomePathItem.SectionHeader(3, "비판적으로 생각하기"))
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
