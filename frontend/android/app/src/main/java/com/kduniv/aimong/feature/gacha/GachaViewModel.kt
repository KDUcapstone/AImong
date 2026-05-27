package com.kduniv.aimong.feature.gacha

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kduniv.aimong.R
import com.kduniv.aimong.core.dev.UiMode
import com.kduniv.aimong.feature.dev.mock.StubPetGachaStore
import com.kduniv.aimong.feature.gacha.data.GachaPullCountStore
import com.kduniv.aimong.feature.gacha.data.GachaRepository
import com.kduniv.aimong.feature.gacha.data.model.GachaPullData
import com.kduniv.aimong.feature.gacha.data.model.RemainingTicketsDto
import com.kduniv.aimong.feature.home.data.PetRepository
import com.kduniv.aimong.feature.home.domain.ChildHomeRefreshBus
import com.kduniv.aimong.feature.home.domain.GetHomeStatusUseCase
import com.kduniv.aimong.feature.home.domain.HomeRefreshTrigger
import com.kduniv.aimong.feature.pet.data.model.PetDto
import com.kduniv.aimong.feature.pet.data.model.PetListData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class GachaViewModel @Inject constructor(
    private val petRepository: PetRepository,
    private val gachaRepository: GachaRepository,
    private val gachaPullCountStore: GachaPullCountStore,
    private val getHomeStatusUseCase: GetHomeStatusUseCase,
    private val homeRefreshBus: ChildHomeRefreshBus,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    data class UiState(
        val loading: Boolean = false,
        val pets: PetListData? = null,
        val petCards: List<GachaPetCardUi> = emptyList(),
        val fragmentBalance: GachaFragmentBalance = GachaFragmentBalance.from(null),
        val ownedCatalogCount: Int = 0,
        val pullReveal: GachaPullRevealUi? = null,
        val tickets: RemainingTicketsDto? = null,
        val gachaPullCount: Int = 0,
        val transientMessage: String? = null,
    ) {
        val normalTicketCount: Int
            get() = tickets?.normal ?: 0

        val hasAnyTicket: Boolean
            get() = normalTicketCount > 0

        val gachaLevel: Int
            get() = GachaProbabilityTable.levelFromPullCount(gachaPullCount)
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        refresh()
        prewarmPetArtCache()
        viewModelScope.launch {
            homeRefreshBus.events
                .debounce(80)
                .collect { trigger ->
                    when (trigger) {
                        is HomeRefreshTrigger.TicketsUpdated ->
                            applyTickets(trigger.normal)
                        HomeRefreshTrigger.Full,
                        is HomeRefreshTrigger.MissionCompleted,
                        is HomeRefreshTrigger.PetAimongAchieved,
                        -> {
                            syncTicketsFromHome()
                            refreshCatalogFromServer()
                        }
                    }
                }
        }
    }

    private fun prewarmPetArtCache() {
        viewModelScope.launch(Dispatchers.Default) {
            val stages = listOf("EGG", "GROWTH", "AIMONG")
            for (entry in GachaPetCatalog.entries) {
                for (stage in stages) {
                    PetArtAssets.drawableRes(appContext, entry.petType, stage, allowStageFallback = true)
                    PetArtAssets.drawableRes(appContext, entry.petType, stage, allowStageFallback = false)
                }
            }
        }
    }

    fun consumeTransientMessage() {
        _state.update { it.copy(transientMessage = null) }
    }

    fun consumePullReveal() {
        _state.update { it.copy(pullReveal = null) }
    }

    /**
     * 탭 복귀 — 티켓·장착 배지만 동기화. 도감 전체는 비어 있을 때·[invalidateGachaCatalog] 때만.
     */
    fun onGachaResumed() {
        syncTicketsFromHome()
        if (_state.value.petCards.isEmpty() || _state.value.pets == null) {
            refresh()
        } else {
            reloadEquippedPet()
        }
    }

    /** 뽑기·교환·장착·홈 [HomeRefreshTrigger] 후 도감 Lv·스프라이트 전체 갱신 */
    fun invalidateGachaCatalog() {
        refreshCatalogFromServer()
    }

    /** 홈·퀘스트 보상 후 수집 탭 티켓 칩만 GET /home 으로 맞춤 */
    fun syncTicketsFromHome() {
        viewModelScope.launch {
            loadTickets().getOrNull()?.let { tickets -> applyTickets(tickets.normal) }
        }
    }

    private fun applyTickets(normal: Int) {
        val count = normal.coerceAtLeast(0)
        if (UiMode.useStubNav) {
            StubPetGachaStore.setNormalTickets(count)
        }
        _state.update { it.copy(tickets = RemainingTicketsDto(normal = count)) }
    }

    /** 홈·다른 탭에서 장착이 바뀐 뒤 수집 상단 장착 영역 동기화 */
    fun reloadEquippedPet() {
        viewModelScope.launch {
            val petData = withContext(Dispatchers.IO) { petRepository.getPets().getOrNull() }
                ?: return@launch
            applyEquippedPetChange(petData, message = null)
        }
    }

    /** 미션·진화 후 수집 탭 복귀 — GET /pet 으로 도감 Lv·스프라이트 갱신 */
    fun refreshCatalogFromServer() {
        viewModelScope.launch {
            val petData = withContext(Dispatchers.IO) { petRepository.getPets().getOrNull() }
                ?: return@launch
            _state.update { s ->
                val lists = buildPetLists(petData, s.fragmentBalance)
                s.copy(
                    pets = petData,
                    petCards = lists.encyclopedia,
                    ownedCatalogCount = lists.ownedCount,
                )
            }
        }
    }

    /** 장착 변경 시 도감 전체를 다시 그리지 않고 장착 배지만 갱신 */
    private fun applyEquippedPetChange(petData: PetListData?, message: String?) {
        _state.update { s ->
            val equippedId = petData?.equippedPet?.id
            val cards = if (s.petCards.isNotEmpty() && petData != null) {
                s.petCards.map { card ->
                    card.copy(isEquipped = card.pet?.id == equippedId)
                }
            } else {
                buildPetLists(petData, s.fragmentBalance).encyclopedia
            }
            s.copy(
                pets = petData,
                petCards = cards,
                ownedCatalogCount = countOwnedInCatalog(petData),
                transientMessage = message ?: s.transientMessage,
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, transientMessage = null) }
            var err: String? = null
            val petData = petRepository.getPets().onFailure { err = it.message }.getOrNull()
            val fragData = gachaRepository.getFragments().onFailure {
                if (err == null) err = it.message
            }.getOrNull()
            val balance = GachaFragmentBalance.from(fragData)
            val tickets = loadTickets().onFailure {
                if (err == null) err = it.message
            }.getOrNull()
            val pullCount = gachaPullCountStore.getPullCount()
            _state.update {
                val lists = buildPetLists(petData, balance)
                it.copy(
                    loading = false,
                    pets = petData,
                    petCards = lists.encyclopedia,
                    fragmentBalance = balance,
                    ownedCatalogCount = lists.ownedCount,
                    tickets = tickets ?: it.tickets,
                    gachaPullCount = pullCount,
                    transientMessage = err,
                )
            }
        }
    }

    fun pull() {
        viewModelScope.launch {
            if (_state.value.normalTicketCount <= 0) {
                _state.update {
                    it.copy(transientMessage = appContext.getString(R.string.gacha_ticket_insufficient))
                }
                return@launch
            }
            _state.update { it.copy(loading = true, transientMessage = null) }
            gachaRepository.pull().fold(
                onSuccess = { data -> applyPullSuccess(data) },
                onFailure = { e ->
                    _state.update { it.copy(loading = false, transientMessage = e.message) }
                }
            )
        }
    }

    fun equipPet(petId: String) {
        viewModelScope.launch {
            petRepository.equipPet(petId).fold(
                onSuccess = {
                    val petData = withContext(Dispatchers.IO) { petRepository.getPets().getOrNull() }
                    applyEquippedPetChange(
                        petData = petData,
                        message = appContext.getString(R.string.gacha_equip_done),
                    )
                    homeRefreshBus.notify(HomeRefreshTrigger.Full)
                },
                onFailure = { e ->
                    _state.update { it.copy(transientMessage = e.message) }
                }
            )
        }
    }

    fun exchange(grade: String, petType: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, transientMessage = null) }
            gachaRepository.exchange(grade.uppercase(), petType.trim()).fold(
                onSuccess = {
                    var err: String? = null
                    val petData = petRepository.getPets().onFailure { err = it.message }.getOrNull()
                    val fragData = gachaRepository.getFragments().onFailure { e ->
                        if (err == null) err = e.message
                    }.getOrNull()
                    val balance = GachaFragmentBalance.from(fragData)
                    _state.update { s ->
                        val lists = buildPetLists(petData, balance)
                        s.copy(
                            loading = false,
                            pets = petData,
                            petCards = lists.encyclopedia,
                            fragmentBalance = balance,
                            ownedCatalogCount = lists.ownedCount,
                            transientMessage = err ?: appContext.getString(R.string.gacha_exchange_done)
                        )
                    }
                    if (!UiMode.useStubNav) {
                        homeRefreshBus.notify(HomeRefreshTrigger.Full)
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(loading = false, transientMessage = e.message) }
                }
            )
        }
    }

    private suspend fun applyPullSuccess(data: GachaPullData) {
        gachaPullCountStore.incrementPullCount()
        val pullCount = gachaPullCountStore.getPullCount()
        var err: String? = null
        val petData = petRepository.getPets().onFailure { err = it.message }.getOrNull()
        val fragData = gachaRepository.getFragments().onFailure {
            if (err == null) err = it.message
        }.getOrNull()
        val balance = GachaFragmentBalance.from(fragData)
        _state.update {
            val lists = buildPetLists(petData, balance)
            it.copy(
                loading = false,
                pets = petData,
                petCards = lists.encyclopedia,
                fragmentBalance = balance,
                ownedCatalogCount = lists.ownedCount,
                pullReveal = buildPullReveal(data),
                tickets = data.remainingTickets,
                gachaPullCount = pullCount,
                transientMessage = err,
            )
        }
        val remaining = data.remainingTickets.normal.coerceAtLeast(0)
        homeRefreshBus.notify(HomeRefreshTrigger.TicketsUpdated(remaining))
        if (!UiMode.useStubNav) {
            homeRefreshBus.notify(HomeRefreshTrigger.Full)
        }
    }

    private fun buildPullReveal(data: GachaPullData): GachaPullRevealUi {
        val r = data.result
        return GachaPullRevealUi(
            displayName = GachaUiMapper.resolvePetDisplayName(r.petType, r.petName, r.grade),
            petType = r.petType,
            grade = r.grade,
            emoji = GachaPetCatalog.emojiFor(r.petType, r.grade),
            isNew = r.isNew,
            fragmentsGot = r.fragmentsGot,
            levelUp = data.levelUp,
            remainingTickets = data.remainingTickets.normal,
        )
    }

    private suspend fun loadTickets(): Result<RemainingTicketsDto> {
        if (UiMode.useStubNav) {
            return Result.success(StubPetGachaStore.currentTickets())
        }
        return getHomeStatusUseCase().map { home ->
            RemainingTicketsDto(normal = home.tickets.normal)
        }
    }

    private data class PetLists(
        val encyclopedia: List<GachaPetCardUi>,
        val ownedCount: Int,
    )

    private fun buildPetLists(pets: PetListData?, balance: GachaFragmentBalance): PetLists =
        PetLists(
            encyclopedia = buildEncyclopediaCards(pets, balance),
            ownedCount = countOwnedInCatalog(pets),
        )

    private fun countOwnedInCatalog(pets: PetListData?): Int {
        if (pets == null) return 0
        val ownedTypes = buildSet {
            pets.pets.forEach { add(it.petType) }
            pets.equippedPet?.let { add(it.petType) }
        }
        return GachaPetCatalog.entries.count { it.petType in ownedTypes }
    }

    private fun buildEncyclopediaCards(
        pets: PetListData?,
        balance: GachaFragmentBalance,
    ): List<GachaPetCardUi> {
        val equippedId = pets?.equippedPet?.id
        val ownedByType = buildMap {
            pets?.pets.orEmpty().forEach { put(it.petType, it) }
            pets?.equippedPet?.let { put(it.petType, it) }
        }
        return GachaPetCatalog.entries.map { entry ->
            val owned = ownedByType[entry.petType]
            val isLocked = owned == null
            val (count, threshold) = if (owned != null) {
                GachaUiMapper.fragmentProgress(owned, balance)
            } else {
                GachaUiMapper.fragmentProgressForGrade(entry.grade, balance)
            }
            GachaPetCardUi(
                catalogPetType = entry.petType,
                pet = owned,
                isLocked = isLocked,
                isEquipped = owned != null && owned.id == equippedId,
                displayName = GachaUiMapper.displayNameForPetType(entry.petType, entry.grade),
                emoji = entry.emoji,
                grade = entry.grade,
                levelLabel = owned?.let { pet ->
                    GachaUiMapper.displayCardLevelLabel(appContext, pet)
                }.orEmpty(),
                fragmentCount = count,
                fragmentThreshold = threshold,
            )
        }
    }

}
