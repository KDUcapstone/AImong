package com.kduniv.aimong.feature.gacha

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kduniv.aimong.R
import com.kduniv.aimong.feature.gacha.data.GachaRepository
import com.kduniv.aimong.feature.gacha.data.model.GachaPullData
import com.kduniv.aimong.feature.gacha.data.model.RemainingTicketsDto
import com.kduniv.aimong.feature.home.data.PetRepository
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
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    data class UiState(
        val loading: Boolean = false,
        val pets: PetListData? = null,
        val fragmentsText: String = "",
        val pullSummary: String? = null,
        val lastRemainingTickets: RemainingTicketsDto? = null,
        val selectedTicket: String = "NORMAL",
        val transientMessage: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun consumeTransientMessage() {
        _state.update { it.copy(transientMessage = null) }
    }

    fun setTicketType(type: String) {
        _state.update { it.copy(selectedTicket = type) }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, transientMessage = null) }
            var err: String? = null
            val petData = petRepository.getPets().onFailure { err = it.message }.getOrNull()
            val fragData = gachaRepository.getFragments().onFailure {
                if (err == null) err = it.message
            }.getOrNull()
            val fragText = fragData?.fragments?.joinToString("\n") { row ->
                "${row.grade}: ${row.count} / ${row.exchangeThreshold}"
            }.orEmpty()
            _state.update {
                it.copy(
                    loading = false,
                    pets = petData,
                    fragmentsText = fragText,
                    transientMessage = err
                )
            }
        }
    }

    fun pull() {
        viewModelScope.launch {
            val ticket = _state.value.selectedTicket
            _state.update { it.copy(loading = true, transientMessage = null) }
            gachaRepository.pull(ticket).fold(
                onSuccess = { data ->
                    var err: String? = null
                    val petData = petRepository.getPets().onFailure { err = it.message }.getOrNull()
                    val fragData = gachaRepository.getFragments().onFailure {
                        if (err == null) err = it.message
                    }.getOrNull()
                    val fragText = fragData?.fragments?.joinToString("\n") { row ->
                        "${row.grade}: ${row.count} / ${row.exchangeThreshold}"
                    }.orEmpty()
                    val levelMsg = if (data.levelUp) {
                        appContext.getString(R.string.gacha_level_up)
                    } else {
                        null
                    }
                    _state.update {
                        it.copy(
                            loading = false,
                            pets = petData,
                            fragmentsText = fragText,
                            pullSummary = formatPullSummary(data),
                            lastRemainingTickets = data.remainingTickets,
                            transientMessage = err ?: levelMsg
                        )
                    }
                },
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
                    _state.update { s ->
                        s.copy(
                            loading = false,
                            pets = petData,
                            transientMessage = appContext.getString(R.string.gacha_equip_done)
                        )
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
                    val fragText = fragData?.fragments?.joinToString("\n") { row ->
                        "${row.grade}: ${row.count} / ${row.exchangeThreshold}"
                    }.orEmpty()
                    _state.update { s ->
                        s.copy(
                            loading = false,
                            pets = petData,
                            fragmentsText = fragText,
                            transientMessage = err ?: appContext.getString(R.string.gacha_exchange_done)
                        )
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(loading = false, transientMessage = e.message) }
                }
            )
        }
    }

    private fun formatPullSummary(data: GachaPullData): String {
        val r = data.result
        val t = data.remainingTickets
        return appContext.getString(
            R.string.gacha_pull_summary_fmt,
            r.petName,
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
