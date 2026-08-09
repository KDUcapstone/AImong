package com.kduniv.aimong.feature.home

import androidx.lifecycle.viewModelScope
import com.kduniv.aimong.core.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : BaseViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            // TODO: 실제 API 또는 Local DB에서 데이터 로드
            _uiState.value = HomeUiState.Success(
                petStage = PetStage.EGG,
                petMood = PetMood.IDLE,
                totalXp = 100,
                continuousDays = 5,
                tickets = TicketCount(normal = 3, rare = 1, epic = 0)
            )
        }
    }
}
