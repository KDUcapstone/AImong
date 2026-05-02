package com.kduniv.aimong.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            // 시안 v1.3 데이터 반영
            _uiState.update { state ->
                state.copy(
                    nickname = "에이몽친구",
                    streakDays = 7,
                    userLevel = 7,
                    profileType = "EXPLORER",
                    
                    petName = "팩트봇",
                    petLevel = 3,
                    petXp = 340,
                    petMaxXp = 500,
                    petMessage = "오늘도 같이 공부해요! 😊",
                    
                    normalTickets = 3,
                    energyCount = 5, // 에너지 5개 만점
                    
                    srBonus = 5,
                    gachaDescription = "레전드 확률 4% (Lv.7)",
                    
                    todayQuestProgress = "2/3",
                    quests = listOf(
                        QuestItemUiState(
                            id = "1",
                            title = "AI 퀴즈 3문제 맞히기",
                            rewardSummary = "+50 EXP · 가챠 티켓 1장",
                            iconRes = null,
                            isCompleted = true,
                            canStart = false
                        ),
                        QuestItemUiState(
                            id = "2",
                            title = "팩트체크 2문제 클리어",
                            rewardSummary = "+30 EXP",
                            iconRes = null,
                            isCompleted = true,
                            canStart = false
                        ),
                        QuestItemUiState(
                            id = "3",
                            title = "GPT 프롬프트 실습 1회",
                            rewardSummary = "+40 EXP · 랜덤 아이템",
                            iconRes = null,
                            isCompleted = false,
                            canStart = true
                        )
                    )
                )
            }
        }
    }

    fun getProfileLabel(type: String): String {
        return when (type) {
            "SPROUT" -> "AI 새싹"
            "EXPLORER" -> "AI 탐험가"
            "CRITIC" -> "AI 비평가"
            "GUARDIAN" -> "AI 수호자"
            else -> "AI 입문자"
        }
    }
}
