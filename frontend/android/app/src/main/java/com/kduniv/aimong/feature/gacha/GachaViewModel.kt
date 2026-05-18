package com.kduniv.aimong.feature.gacha

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kduniv.aimong.R
import com.kduniv.aimong.core.dev.UiMode
import com.kduniv.aimong.feature.dev.mock.StubPetGachaStore
import com.kduniv.aimong.feature.gacha.data.GachaRepository
import com.kduniv.aimong.feature.gacha.data.model.FragmentGradeRow
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class GachaViewModel @Inject constructor(
    private val petRepository: PetRepository,
    private val gachaRepository: GachaRepository,
    private val getHomeStatusUseCase: GetHomeStatusUseCase,
    private val homeRefreshBus: ChildHomeRefreshBus,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    data class UiState(
        val loading: Boolean = false,
        val pets: PetListData? = null,
        val ownedPetCards: List<GachaPetCardUi> = emptyList(),
        val petCards: List<GachaPetCardUi> = emptyList(),
        val fragmentRows: List<FragmentGradeRow> = emptyList(),
        val ownedCatalogCount: Int = 0,
        val pullSummary: String? = null,
        val tickets: RemainingTicketsDto? = null,
        val selectedTicket: String = "NORMAL",
        val transientMessage: String? = null
    ) {
        val hasAnyTicket: Boolean
            get() = tickets?.let { it.normal > 0 || it.rare > 0 || it.epic > 0 } == true

        fun ticketCount(type: String): Int = when (type.uppercase(Locale.US)) {
            "RARE" -> tickets?.rare ?: 0
            "EPIC" -> tickets?.epic ?: 0
            else -> tickets?.normal ?: 0
        }
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun consumeTransientMessage() {
        _state.update { it.copy(transientMessage = null) }
    }

    fun consumePullSummary() {
        _state.update { it.copy(pullSummary = null) }
    }

    fun setTicketType(type: String) {
        _state.update { it.copy(selectedTicket = type) }
    }

    /** 홈·퀘스트 보상 후 수집 탭 티켓 칩만 GET /home 으로 맞춤 */
    fun syncTicketsFromHome() {
        viewModelScope.launch {
            loadTickets().getOrNull()?.let { tickets ->
                _state.update { it.copy(tickets = tickets) }
            }
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
            val rows = fragData?.fragments.orEmpty()
            val tickets = loadTickets().onFailure {
                if (err == null) err = it.message
            }.getOrNull()
            _state.update {
                val lists = buildPetLists(petData, rows)
                it.copy(
                    loading = false,
                    pets = petData,
                    ownedPetCards = lists.owned,
                    petCards = lists.encyclopedia,
                    fragmentRows = rows,
                    ownedCatalogCount = lists.ownedCount,
                    tickets = tickets ?: it.tickets,
                    transientMessage = err
                )
            }
        }
    }

    fun pull() {
        viewModelScope.launch {
            val ticket = _state.value.selectedTicket
            if (_state.value.ticketCount(ticket) <= 0) {
                _state.update {
                    it.copy(transientMessage = appContext.getString(R.string.gacha_ticket_insufficient))
                }
                return@launch
            }
            _state.update { it.copy(loading = true, transientMessage = null) }
            gachaRepository.pull(ticket).fold(
                onSuccess = { data -> applyPullSuccess(data) },
                onFailure = { e ->
                    _state.update { it.copy(loading = false, transientMessage = e.message) }
                }
            )
        }
    }

    fun equipPet(petId: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, transientMessage = null) }
            petRepository.equipPet(petId).fold(
                onSuccess = {
                    val petData = petRepository.getPets().getOrNull()
                    val rows = _state.value.fragmentRows
                    _state.update { s ->
                        val lists = buildPetLists(petData, rows)
                        s.copy(
                            loading = false,
                            pets = petData,
                            ownedPetCards = lists.owned,
                            petCards = lists.encyclopedia,
                            ownedCatalogCount = lists.ownedCount,
                            transientMessage = appContext.getString(R.string.gacha_equip_done)
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

    fun exchange(grade: String, petType: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, transientMessage = null) }
            gachaRepository.exchange(grade.uppercase(Locale.US), petType.trim()).fold(
                onSuccess = {
                    var err: String? = null
                    val petData = petRepository.getPets().onFailure { err = it.message }.getOrNull()
                    val fragData = gachaRepository.getFragments().onFailure { e ->
                        if (err == null) err = e.message
                    }.getOrNull()
                    val rows = fragData?.fragments.orEmpty()
                    _state.update { s ->
                        val lists = buildPetLists(petData, rows)
                        s.copy(
                            loading = false,
                            pets = petData,
                            ownedPetCards = lists.owned,
                            petCards = lists.encyclopedia,
                            fragmentRows = rows,
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
        var err: String? = null
        val petData = petRepository.getPets().onFailure { err = it.message }.getOrNull()
        val fragData = gachaRepository.getFragments().onFailure {
            if (err == null) err = it.message
        }.getOrNull()
        val rows = fragData?.fragments.orEmpty()
        val levelMsg = if (data.levelUp) {
            appContext.getString(R.string.gacha_level_up)
        } else {
            null
        }
        _state.update {
            val lists = buildPetLists(petData, rows)
            it.copy(
                loading = false,
                pets = petData,
                ownedPetCards = lists.owned,
                petCards = lists.encyclopedia,
                fragmentRows = rows,
                ownedCatalogCount = lists.ownedCount,
                pullSummary = formatPullSummary(data),
                tickets = data.remainingTickets,
                transientMessage = err ?: levelMsg
            )
        }
        if (!UiMode.useStubNav) {
            homeRefreshBus.notify(HomeRefreshTrigger.Full)
        }
    }

    private suspend fun loadTickets(): Result<RemainingTicketsDto> {
        if (UiMode.useStubNav) {
            return Result.success(StubPetGachaStore.currentTickets())
        }
        return getHomeStatusUseCase().map { home ->
            RemainingTicketsDto(
                normal = home.tickets.normal,
                rare = home.tickets.rare,
                epic = home.tickets.epic,
            )
        }
    }

    private data class PetLists(
        val owned: List<GachaPetCardUi>,
        val encyclopedia: List<GachaPetCardUi>,
        val ownedCount: Int,
    )

    private fun buildPetLists(pets: PetListData?, rows: List<FragmentGradeRow>): PetLists =
        PetLists(
            owned = buildOwnedPetCards(pets, rows),
            encyclopedia = buildEncyclopediaCards(pets, rows),
            ownedCount = countOwnedInCatalog(pets),
        )

    private fun buildOwnedPetCards(
        pets: PetListData?,
        rows: List<FragmentGradeRow>
    ): List<GachaPetCardUi> {
        if (pets == null) return emptyList()
        val equippedId = pets.equippedPet?.id
        val allOwned = buildList {
            pets.equippedPet?.let { add(it) }
            addAll(pets.pets)
        }.distinctBy { it.id }
        return allOwned.map { pet -> toOwnedCardUi(pet, equippedId, rows) }
    }

    private fun toOwnedCardUi(
        pet: PetDto,
        equippedId: String?,
        rows: List<FragmentGradeRow>
    ): GachaPetCardUi {
        val (count, threshold) = GachaUiMapper.fragmentProgress(pet, rows)
        return GachaPetCardUi(
            catalogPetType = pet.petType,
            pet = pet,
            isLocked = false,
            isEquipped = pet.id == equippedId,
            displayName = GachaPetCatalog.displayNameFor(pet.petType, pet.grade),
            emoji = GachaPetCatalog.emojiFor(pet.petType, pet.grade),
            grade = pet.grade,
            levelLabel = GachaUiMapper.displayLevel(pet),
            fragmentCount = count,
            fragmentThreshold = threshold
        )
    }

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
        rows: List<FragmentGradeRow>
    ): List<GachaPetCardUi> {
        val equippedType = pets?.equippedPet?.petType
        val ownedByType = buildMap {
            pets?.pets.orEmpty().forEach { put(it.petType, it) }
            pets?.equippedPet?.let { put(it.petType, it) }
        }
        return GachaPetCatalog.entries
            .filter { it.petType != equippedType }
            .map { entry ->
                val owned = ownedByType[entry.petType]
                val isLocked = owned == null
                val (count, threshold) = if (owned != null) {
                    GachaUiMapper.fragmentProgress(owned, rows)
                } else {
                    GachaUiMapper.fragmentProgressForGrade(entry.grade, rows)
                }
                GachaPetCardUi(
                    catalogPetType = entry.petType,
                    pet = owned,
                    isLocked = isLocked,
                    displayName = entry.displayName,
                    emoji = entry.emoji,
                    grade = entry.grade,
                    levelLabel = owned?.let { GachaUiMapper.displayLevel(it) }.orEmpty(),
                    fragmentCount = count,
                    fragmentThreshold = threshold
                )
            }
    }

    private fun formatPullSummary(data: GachaPullData): String {
        val r = data.result
        val t = data.remainingTickets
        return appContext.getString(
            R.string.gacha_pull_summary_fmt,
            r.petName?.takeIf { it.isNotBlank() } ?: r.petType,
            r.petType,
            r.grade,
            if (r.isNew) appContext.getString(R.string.gacha_new_yes) else appContext.getString(R.string.gacha_new_no),
            r.fragmentsGot,
            data.srMissCount,
            data.srBonus,
            if (data.levelUp) {
                appContext.getString(R.string.gacha_level_up_yes)
            } else {
                appContext.getString(R.string.gacha_level_up_no)
            },
            appContext.getString(R.string.gacha_tickets_fmt, t.normal, t.rare, t.epic)
        )
    }
}
