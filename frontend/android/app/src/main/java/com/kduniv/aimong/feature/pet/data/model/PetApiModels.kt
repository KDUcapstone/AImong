package com.kduniv.aimong.feature.pet.data.model

import com.google.gson.annotations.SerializedName

data class PetDto(
    @SerializedName("id") val id: String,
    @SerializedName(value = "petType", alternate = ["pet_type"]) val petType: String,
    @SerializedName("grade") val grade: String,
    @SerializedName("xp") val xp: Int,
    @SerializedName("stage") val stage: String,
    @SerializedName("mood") val mood: String,
    @SerializedName("crownUnlocked") val crownUnlocked: Boolean,
    @SerializedName("crownType") val crownType: String?,
    @SerializedName("obtainedAt") val obtainedAt: String?
)

data class PetListData(
    @SerializedName("equippedPet") val equippedPet: PetDto?,
    @SerializedName("pets") val pets: List<PetDto>,
    @SerializedName("totalPetCount") val totalPetCount: Int
)

data class PetEquipRequest(
    @SerializedName("petId") val petId: String
)

data class PetEquipData(
    @SerializedName("equippedPetId") val equippedPetId: String,
    @SerializedName(value = "petType", alternate = ["pet_type"]) val petType: String,
    @SerializedName("grade") val grade: String,
    @SerializedName("stage") val stage: String
)
