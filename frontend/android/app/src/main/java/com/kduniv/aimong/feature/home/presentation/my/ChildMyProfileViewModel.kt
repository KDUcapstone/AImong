package com.kduniv.aimong.feature.home.presentation.my

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kduniv.aimong.R
import com.kduniv.aimong.core.dev.UiMode
import com.kduniv.aimong.feature.dev.mock.MockUiSamples
import com.kduniv.aimong.feature.home.domain.GetHomeStatusUseCase
import com.kduniv.aimong.feature.home.presentation.HomeUiMapper
import com.kduniv.aimong.feature.home.data.PetRepository
import com.kduniv.aimong.feature.mission.domain.model.Mission
import com.kduniv.aimong.feature.mission.domain.repository.MissionRepository
import com.kduniv.aimong.feature.quest.data.model.AchievementItemDto
import com.kduniv.aimong.feature.quest.domain.repository.QuestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ChildMyProfileViewModel @Inject constructor(
    private val getHomeStatusUseCase: GetHomeStatusUseCase,
    private val petRepository: PetRepository,
    private val questRepository: QuestRepository,
    private val missionRepository: MissionRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChildMyProfileUiState())
    val uiState: StateFlow<ChildMyProfileUiState> = _uiState.asStateFlow()

    private var loadJobActive = false

    fun onScreenVisible() {
        if (loadJobActive) return
        loadProfile()
    }

    fun loadProfile() {
        loadJobActive = true
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                if (UiMode.useStubNav) {
                    bindStubProfile()
                } else {
                    val result = withContext(Dispatchers.IO) {
                        coroutineScope {
                            val homeDeferred = async { getHomeStatusUseCase() }
                            val petsDeferred = async { petRepository.getPets() }
                            val achievementsDeferred = async { questRepository.getAchievements() }
                            val missionsDeferred = async { loadMissionsSafely() }

                            ProfileLoadResult(
                                home = homeDeferred.await(),
                                pets = petsDeferred.await(),
                                achievements = achievementsDeferred.await(),
                                missions = missionsDeferred.await()
                            )
                        }
                    }
                    bindLoaded(result)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "프로필을 불러오지 못했어요."
                    )
                }
            } finally {
                loadJobActive = false
            }
        }
    }

    private suspend fun loadMissionsSafely(): List<Mission> =
        runCatching { missionRepository.getMissionsFlow().first() }.getOrDefault(emptyList())

    private suspend fun bindStubProfile() {
        val home = MockUiSamples.homeUiState()
        val pets = withContext(Dispatchers.IO) { petRepository.getPets().getOrNull() }
        val achievements = withContext(Dispatchers.IO) {
            questRepository.getAchievements().getOrNull()?.achievements.orEmpty()
        }
        publishProfile(
            nickname = home.nickname,
            profileType = home.profileType,
            completedMissionCount = 1,
            totalXp = home.totalXp.takeIf { it > 0 } ?: home.topStatusXp,
            petCount = pets?.totalPetCount ?: pets?.pets?.size ?: 3,
            streakDays = home.streakDays,
            badges = achievements.map { it.toBadgeUi() }
        )
    }

    private suspend fun bindLoaded(result: ProfileLoadResult) {
        val homeData = result.home.getOrNull()
        if (homeData == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = result.home.exceptionOrNull()?.message
                        ?: "프로필을 불러오지 못했어요."
                )
            }
            return
        }

        val homeUi = HomeUiMapper.toUiState(homeData)
        val achievements = result.achievements.getOrNull()?.achievements.orEmpty()
        publishProfile(
            nickname = homeUi.nickname.ifBlank {
                appContext.getString(R.string.home_pet_name_default)
            },
            profileType = homeUi.profileType,
            completedMissionCount = countCompletedMissions(result.missions),
            totalXp = homeUi.totalXp,
            petCount = result.pets.getOrNull()?.totalPetCount
                ?: result.pets.getOrNull()?.pets?.size
                ?: 0,
            streakDays = homeUi.streakDays,
            badges = achievements.map { it.toBadgeUi() }
        )
    }

    private fun publishProfile(
        nickname: String,
        profileType: String,
        completedMissionCount: Int,
        totalXp: Int,
        petCount: Int,
        streakDays: Int,
        badges: List<ChildMyBadgeUi>
    ) {
        _uiState.update {
            it.copy(
                nickname = nickname,
                profileSubtitle = appContext.getString(
                    R.string.child_my_profile_subtitle_fmt,
                    profileLabelFor(profileType)
                ),
                completedMissionCount = completedMissionCount,
                totalXp = totalXp,
                petCount = petCount,
                streakDays = streakDays,
                badges = badges,
                isLoading = false,
                errorMessage = null
            )
        }
    }

    private fun profileLabelFor(type: String): String = when (type) {
        "SPROUT" -> "AI 새싹"
        "EXPLORER" -> "AI 탐험가"
        "CRITIC" -> "AI 비평가"
        "GUARDIAN" -> "AI 수호자"
        else -> "AI 입문자"
    }

    private fun countCompletedMissions(missions: List<Mission>): Int =
        missions.count { mission -> mission.starLevels.any { it.isCompleted } }

    private fun AchievementItemDto.toBadgeUi() = ChildMyBadgeUi(
        achievementType = achievementType,
        label = label,
        iconRes = ChildMyAchievementIcons.iconFor(achievementType),
        isUnlocked = isCompleted
    )

    private data class ProfileLoadResult(
        val home: Result<com.kduniv.aimong.feature.home.data.model.HomeScreenData>,
        val pets: Result<com.kduniv.aimong.feature.pet.data.model.PetListData>,
        val achievements: Result<com.kduniv.aimong.feature.quest.data.model.AchievementsResponseData>,
        val missions: List<Mission>
    )
}
