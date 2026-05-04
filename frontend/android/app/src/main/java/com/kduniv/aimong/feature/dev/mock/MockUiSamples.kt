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
            pathItems = listOf(
                HomePathItem.Completed(1, "입문 미션 완료", "📖"),
                HomePathItem.Completed(2, "AI 란 무엇인가", "🤖"),
                HomePathItem.Completed(3, "데이터의 이해", "📊"),
                HomePathItem.Completed(4, "머신러닝의 기초", "🧠"),
                HomePathItem.Completed(5, "딥러닝 알아보기", "💡"),
                HomePathItem.TodayStart(
                    missionId = "mock-mission-1",
                    missionTitle = "오늘의 AI 탐험",
                    enabled = true,
                    icon = "🌟"
                ),
                HomePathItem.Review(missionId = "mock-mission-2", subtitle = "틀린 문제 복습"),
                HomePathItem.Locked(hint = "내일 열림"),
                HomePathItem.Locked(hint = "이후 오픈"),
                HomePathItem.Locked(hint = "다음 챕터")
            )
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
