package com.kduniv.aimong.feature.onboarding.child

import com.kduniv.aimong.core.local.SessionManager
import com.kduniv.aimong.feature.home.data.PetRepository
import com.kduniv.aimong.feature.home.domain.GetHomeStatusUseCase
import com.kduniv.aimong.feature.pet.data.model.PetListData
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
) {
    /** [evaluateEntry]에서 확인한 티켓 수 — 수집 탭 진입 직후 API 응답 전 뽑기 버튼 활성화용 */
    var onboardingTicketHint: Int = 0
        private set

    /** [evaluateEntry]·로그인 시 확정 */
    private var activeChildId: String? = null

    /** 튜토리얼 장착 단계 — 서버 자동 장착이어도 사용자가 장착 버튼을 누를 때까지 완료하지 않음 */
    private var manualEquipConfirmed: Boolean = false

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

        val pets = petRepository.getPets().getOrNull()
        if (pets.hasAnyPet()) {
            finishOnboardingIfNeeded()
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
            else -> {
                onboardingTicketHint = tickets
                ChildGachaOnboardingEntry.StartWelcome(tickets)
            }
        }
    }

    suspend fun refreshEquippedFromServer(): Boolean {
        val hasPet = petRepository.getPets().getOrNull().hasAnyPet()
        if (hasPet) {
            finishOnboardingIfNeeded()
        }
        return hasPet
    }

    fun onWelcomeShown() {
        _phase.value = ChildGachaOnboardingPhase.Welcome
    }

    fun onGachaPullCoachmark() {
        _phase.value = ChildGachaOnboardingPhase.GachaPullCoachmark
    }

    fun onGachaEquipCoachmark() {
        manualEquipConfirmed = false
        _phase.value = ChildGachaOnboardingPhase.GachaEquipCoachmark
    }

    fun requiresManualEquipBeforeComplete(): Boolean =
        _phase.value == ChildGachaOnboardingPhase.GachaEquipCoachmark && !manualEquipConfirmed

    fun onManualEquipConfirmed() {
        manualEquipConfirmed = true
    }

    fun onCompleting() {
        _phase.value = ChildGachaOnboardingPhase.Completing
    }

    fun markCompleted() {
        finishOnboardingIfNeeded()
    }

    /** 로그아웃·역할 전환 시 진행 중 코치마크만 초기화 */
    fun resetActivePhase() {
        activeChildId = null
        onboardingTicketHint = 0
        manualEquipConfirmed = false
        _phase.value = ChildGachaOnboardingPhase.Inactive
    }

    private fun finishOnboardingIfNeeded() {
        _phase.value = ChildGachaOnboardingPhase.Inactive
    }

    private fun PetListData?.hasAnyPet(): Boolean {
        if (this == null) return false
        return totalPetCount > 0 || pets.isNotEmpty() || equippedPet != null
    }
}
