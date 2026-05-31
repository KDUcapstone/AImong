package com.kduniv.aimong.feature.onboarding.child

import android.content.Context
import com.kduniv.aimong.core.local.SessionManager
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
    private val sessionManager: SessionManager,
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    /** [evaluateEntry]·로그인 시 확정 — [markCompleted]에서 동기 접근 */
    private var activeChildId: String? = null

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

    suspend fun isCompletedForCurrentChild(): Boolean {
        val childId = sessionManager.currentChildId() ?: return false
        return prefs.getBoolean(completedKey(childId), false)
    }

    /**
     * @param homeTicketHint 홈 API가 이미 로드된 경우 티켓 수(재시도·레이스 완화).
     */
    suspend fun evaluateEntry(
        homeTicketHint: Int? = null,
        profileChildId: String? = null,
    ): ChildGachaOnboardingEntry {
        val childId = sessionManager.currentChildId()
            ?: profileChildId?.takeIf { it.isNotBlank() }
        if (childId.isNullOrBlank()) {
            return ChildGachaOnboardingEntry.Skip
        }
        if (sessionManager.currentChildId().isNullOrBlank()) {
            sessionManager.saveChildId(childId)
        }
        activeChildId = childId
        if (prefs.getBoolean(completedKey(childId), false)) {
            return ChildGachaOnboardingEntry.Skip
        }
        val pets = petRepository.getPets().getOrNull()
        if (pets?.equippedPet != null) {
            markCompleted()
            return ChildGachaOnboardingEntry.Skip
        }
        val home = getHomeStatusUseCase().getOrNull()
        val tickets = when {
            home != null -> home.tickets.normal
            homeTicketHint != null && homeTicketHint > 0 -> homeTicketHint
            else -> 0
        }
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
        val childId = activeChildId ?: return
        prefs.edit().putBoolean(completedKey(childId), true).apply()
        _phase.value = ChildGachaOnboardingPhase.Inactive
    }

    /** 로그아웃·역할 전환 시 진행 중 코치마크만 초기화(자녀별 완료 플래그는 유지). */
    fun resetActivePhase() {
        activeChildId = null
        _phase.value = ChildGachaOnboardingPhase.Inactive
    }

    private fun completedKey(childId: String): String = "completed_$childId"

    companion object {
        private const val PREFS_NAME = "child_gacha_onboarding"
    }
}
