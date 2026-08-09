package com.kduniv.aimong.feature.dev.mock.repository

import com.kduniv.aimong.feature.home.data.PetRepository
import com.kduniv.aimong.feature.pet.data.model.PetDto
import com.kduniv.aimong.feature.pet.data.model.PetEquipData
import com.kduniv.aimong.feature.pet.data.model.PetListData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PetRepositoryStub @Inject constructor() : PetRepository {

    private var equippedId: String = "mock-pet-1"

    override suspend fun getPets(): Result<PetListData> {
        val pets = listOf(
            mockPet("mock-pet-1", "SPROUT"),
            mockPet("mock-pet-2", "BLOOM")
        )
        return Result.success(
            PetListData(
                equippedPet = pets.firstOrNull { it.id == equippedId },
                pets = pets,
                totalPetCount = pets.size
            )
        )
    }

    override suspend fun equipPet(petId: String): Result<PetEquipData> {
        equippedId = petId.ifBlank { equippedId }
        val p = mockPet(equippedId, if (equippedId.contains("2")) "BLOOM" else "SPROUT")
        return Result.success(
            PetEquipData(
                equippedPetId = p.id,
                petType = p.petType,
                grade = p.grade,
                stage = p.stage
            )
        )
    }

    private fun mockPet(id: String, type: String) = PetDto(
        id = id,
        petType = type,
        grade = "NORMAL",
        xp = 120,
        stage = "GROWTH",
        mood = "HAPPY",
        crownUnlocked = false,
        crownType = null,
        obtainedAt = "2026-01-01T00:00:00Z"
    )
}
