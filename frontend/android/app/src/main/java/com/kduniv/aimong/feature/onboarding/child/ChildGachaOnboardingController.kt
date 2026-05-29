package com.kduniv.aimong.feature.onboarding.child

import android.content.Context
import com.kduniv.aimong.feature.home.data.PetRepository
import com.kduniv.aimong.feature.home.domain.GetHomeStatusUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class ChildGachaOnboardingEntry {
    data object Skip : ChildGachaOnboardingEntry()
    data class StartWelcome(val ticketCount: Int) : ChildGachaOnboardingEntry()
    data object NoTickets : ChildGachaOnboardingEntry()
}

@Singleton
class ChildGachaOnboardingController @Inject constructor(
    private val petRepository: PetRepository,
    private val getHomeStatusUseCase: GetHomeStatusUseCase,
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _phase = MutableStateFlow(ChildGachaOnboardingPhase.Inactive)
    val phase: StateFlow<ChildGachaOnboardingPhase> = _phase.asStateFlow()

    val isNavLockedToGacha: Boolean
        get() = when (_phase.value) {
            ChildGachaOnboardingPhase.Welcome,
            ChildGachaOnboardingPhase.GachaPullCoachmark,
            ChildGachaOnboardingPhase.GachaEquipCoachmark,
            ChildGachaOnboardingPhase.Completing,
            -> true
            ChildGachaOnboardingPhase.Inactive -> false
        }

    val isActive: Boolean
        get() = _phase.value != ChildGachaOnboardingPhase.Inactive

    suspend fun evaluateEntry(): ChildGachaOnboardingEntry {
        if (prefs.getBoolean(KEY_COMPLETED, false)) {
            return ChildGachaOnboardingEntry.Skip
        }
        val pets = petRepository.getPets().getOrNull()
        if (pets?.equippedPet != null) {
            markCompleted()
            return ChildGachaOnboardingEntry.Skip
        }
        val tickets = getHomeStatusUseCase().getOrNull()?.tickets?.normal ?: 0
        return when {
            tickets <= 0 -> ChildGachaOnboardingEntry.NoTickets
            else -> ChildGachaOnboardingEntry.StartWelcome(tickets)
        }
    }

    suspend fun refreshEquippedFromServer(): Boolean {
        val equipped = petRepository.getPets().getOrNull()?.equippedPet != null
        if (equipped) {
            markCompleted()
        }
        return equipped
    }

    fun onWelcomeShown() {
        _phase.value = ChildGachaOnboardingPhase.Welcome
    }

    fun onGachaPullCoachmark() {
        _phase.value = ChildGachaOnboardingPhase.GachaPullCoachmark
    }

    fun onGachaEquipCoachmark() {
        _phase.value = ChildGachaOnboardingPhase.GachaEquipCoachmark
    }

    fun onCompleting() {
        _phase.value = ChildGachaOnboardingPhase.Completing
    }

    fun markCompleted() {
        prefs.edit().putBoolean(KEY_COMPLETED, true).apply()
        _phase.value = ChildGachaOnboardingPhase.Inactive
    }

    companion object {
        private const val PREFS_NAME = "child_gacha_onboarding"
        private const val KEY_COMPLETED = "completed"
    }
}
