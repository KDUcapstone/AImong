package com.kduniv.aimong.feature.dev.mock

import com.kduniv.aimong.feature.gacha.data.model.FragmentGradeRow
import com.kduniv.aimong.feature.gacha.data.model.GachaExchangeData
import com.kduniv.aimong.feature.gacha.data.model.GachaFragmentsData
import com.kduniv.aimong.feature.gacha.data.model.GachaPullData
import com.kduniv.aimong.feature.gacha.data.model.GachaPullResultDto
import com.kduniv.aimong.feature.gacha.data.model.RemainingTicketsDto
import com.kduniv.aimong.feature.pet.data.model.PetDto
import com.kduniv.aimong.feature.pet.data.model.PetEquipData
import com.kduniv.aimong.feature.pet.data.model.PetListData
import kotlin.math.min

/**
 * `UiMode.useStubNav`일 때 펫·가챠 API 대신 사용하는 인메모리 목업.
 */
object StubPetGachaStore {

    private val lock = Any()

    private var equippedPetId: String? = "stub-pet-1"
    private var normalTickets = 5
    private var rareTickets = 2
    private var epicTickets = 1
    private var srMissCount = 3
    private var pullSeq = 0

    private val ownedPets = mutableListOf(
        PetDto(
            id = "stub-pet-1",
            petType = "pet_normal_001",
            grade = "NORMAL",
            xp = 12,
            stage = "GROWTH",
            mood = "HAPPY",
            crownUnlocked = false,
            crownType = null,
            obtainedAt = "2026-03-25T10:00:00Z"
        ),
        PetDto(
            id = "stub-pet-2",
            petType = "pet_rare_003",
            grade = "RARE",
            xp = 0,
            stage = "EGG",
            mood = "IDLE",
            crownUnlocked = true,
            crownType = "gold",
            obtainedAt = "2026-03-27T15:30:00Z"
        )
    )

    private val fragmentRows = mutableListOf(
        FragmentGradeRow("NORMAL", 7, 10),
        FragmentGradeRow("RARE", 12, 30),
        FragmentGradeRow("EPIC", 0, 80),
        FragmentGradeRow("LEGEND", 0, 200)
    )

    fun getPetList(): PetListData = synchronized(lock) {
        val equipped = equippedPetId?.let { id -> ownedPets.find { it.id == id } }
        val others = ownedPets.filter { it.id != equippedPetId }
        PetListData(
            equippedPet = equipped,
            pets = others,
            totalPetCount = ownedPets.size
        )
    }

    fun equipPet(petId: String): Result<PetEquipData> = synchronized(lock) {
        val pet = ownedPets.find { it.id == petId }
            ?: return Result.failure(Exception("펫을 찾을 수 없습니다."))
        equippedPetId = petId
        Result.success(
            PetEquipData(
                equippedPetId = pet.id,
                petType = pet.petType,
                grade = pet.grade,
                stage = pet.stage
            )
        )
    }

    fun getFragments(): GachaFragmentsData = synchronized(lock) {
        GachaFragmentsData(fragmentRows.map { it.copy() })
    }

    fun pull(ticketType: String): Result<GachaPullData> = synchronized(lock) {
        when (ticketType) {
            "NORMAL" -> {
                if (normalTickets <= 0) return Result.failure(Exception("티켓이 부족해요!"))
                normalTickets--
            }
            "RARE" -> {
                if (rareTickets <= 0) return Result.failure(Exception("티켓이 부족해요!"))
                rareTickets--
            }
            "EPIC" -> {
                if (epicTickets <= 0) return Result.failure(Exception("티켓이 부족해요!"))
                epicTickets--
            }
            else -> return Result.failure(Exception("티켓 종류를 선택해주세요"))
        }

        pullSeq++
        val isNew = pullSeq % 3 == 0
        val grade = if (isNew) "RARE" else "NORMAL"
        val petType = if (isNew) "pet_rare_099" else "pet_normal_005"
        val petName = if (isNew) "목업 레어" else "목업 노멀"
        val petId = "stub-pull-$pullSeq"

        if (isNew) {
            ownedPets.add(
                PetDto(
                    id = petId,
                    petType = petType,
                    grade = grade,
                    xp = 0,
                    stage = "EGG",
                    mood = "IDLE",
                    crownUnlocked = false,
                    crownType = null,
                    obtainedAt = "2026-05-06T12:00:00Z"
                )
            )
        } else {
            val row = fragmentRows.find { it.grade == "NORMAL" }
            if (row != null) {
                val idx = fragmentRows.indexOf(row)
                fragmentRows[idx] = row.copy(count = row.count + 1)
            }
        }

        val missBefore = srMissCount
        val appliedSrBonus = if (ticketType == "NORMAL" && missBefore >= 10) {
            min((missBefore - 9) * 0.01, 0.75)
        } else {
            0.0
        }
        if (grade == "EPIC" || grade == "LEGEND") {
            srMissCount = 0
        } else {
            srMissCount = missBefore + 1
        }

        val levelUp = pullSeq == 4

        val result = GachaPullResultDto(
            petId = petId,
            petType = petType,
            petName = petName,
            grade = grade,
            isNew = isNew,
            fragmentsGot = if (isNew) 0 else 1
        )
        Result.success(
            GachaPullData(
                result = result,
                srMissCount = srMissCount,
                srBonus = appliedSrBonus,
                levelUp = levelUp,
                remainingTickets = RemainingTicketsDto(normalTickets, rareTickets, epicTickets)
            )
        )
    }

    fun exchange(grade: String, petType: String): Result<GachaExchangeData> = synchronized(lock) {
        if (ownedPets.any { it.petType == petType }) {
            return Result.failure(Exception("이미 보유한 펫이에요"))
        }
        val row = fragmentRows.find { it.grade == grade }
            ?: return Result.failure(Exception("조각이 부족해요!"))
        if (row.count < row.exchangeThreshold) {
            return Result.failure(Exception("조각이 부족해요!"))
        }
        val idx = fragmentRows.indexOf(row)
        fragmentRows[idx] = row.copy(count = row.count - row.exchangeThreshold)

        val newId = "stub-ex-${petType.hashCode()}"
        ownedPets.add(
            PetDto(
                id = newId,
                petType = petType,
                grade = grade,
                xp = 0,
                stage = "EGG",
                mood = "IDLE",
                crownUnlocked = false,
                crownType = null,
                obtainedAt = "2026-05-06T12:00:00Z"
            )
        )
        Result.success(GachaExchangeData(petId = newId, petType = petType, grade = grade, stage = "EGG"))
    }
}
