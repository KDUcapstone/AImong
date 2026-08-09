package com.aimong.backend.domain.pet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aimong.backend.domain.pet.dto.EquipPetRequest;
import com.aimong.backend.domain.pet.dto.EquipPetResponse;
import com.aimong.backend.domain.pet.dto.PetListResponse;
import com.aimong.backend.domain.pet.dto.PetSummaryResponse;
import com.aimong.backend.domain.pet.service.PetService;
import com.aimong.backend.global.filter.FirebaseParentAuthFilter;
import com.aimong.backend.global.filter.JwtAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PetController.class)
@AutoConfigureMockMvc(addFilters = false)
class PetApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PetService petService;

    @MockitoBean
    private FirebaseParentAuthFilter firebaseParentAuthFilter;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void getPetReturnsEquippedPetAndOwnedPets() throws Exception {
        UUID equippedPetId = UUID.randomUUID();
        UUID otherPetId = UUID.randomUUID();
        given(petService.getPets(any(UUID.class))).willReturn(new PetListResponse(
                new PetSummaryResponse(
                        equippedPetId,
                        "pet_normal_001",
                        "NORMAL",
                        95,
                        "GROWTH",
                        "HAPPY",
                        false,
                        null,
                        Instant.parse("2026-03-25T10:00:00Z")
                ),
                List.of(
                        new PetSummaryResponse(
                                equippedPetId,
                                "pet_normal_001",
                                "NORMAL",
                                95,
                                "GROWTH",
                                "HAPPY",
                                false,
                                null,
                                Instant.parse("2026-03-25T10:00:00Z")
                        ),
                        new PetSummaryResponse(
                                otherPetId,
                                "pet_rare_003",
                                "RARE",
                                0,
                                "EGG",
                                "SAD_LIGHT",
                                true,
                                "gold",
                                Instant.parse("2026-03-27T15:30:00Z")
                        )
                ),
                2
        ));

        mockMvc.perform(get("/pet")
                        .principal(childPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.data.equippedPet.id").value(equippedPetId.toString()))
                .andExpect(jsonPath("$.data.equippedPet.petType").value("pet_normal_001"))
                .andExpect(jsonPath("$.data.equippedPet.grade").value("NORMAL"))
                .andExpect(jsonPath("$.data.equippedPet.xp").value(95))
                .andExpect(jsonPath("$.data.equippedPet.stage").value("GROWTH"))
                .andExpect(jsonPath("$.data.equippedPet.mood").value("HAPPY"))
                .andExpect(jsonPath("$.data.pets[1].id").value(otherPetId.toString()))
                .andExpect(jsonPath("$.data.pets[1].mood").value("SAD_LIGHT"))
                .andExpect(jsonPath("$.data.pets[1].crownUnlocked").value(true))
                .andExpect(jsonPath("$.data.pets[1].crownType").value("gold"))
                .andExpect(jsonPath("$.data.totalPetCount").value(2));
    }

    @Test
    void equipPetReturnsEquippedPetPayload() throws Exception {
        UUID petId = UUID.randomUUID();
        given(petService.equipPet(any(UUID.class), any(UUID.class)))
                .willReturn(new EquipPetResponse(petId, "pet_rare_003", "RARE", "GROWTH"));

        mockMvc.perform(put("/pet/equip")
                        .principal(childPrincipal())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new EquipPetRequest(petId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.data.equippedPetId").value(petId.toString()))
                .andExpect(jsonPath("$.data.petType").value("pet_rare_003"))
                .andExpect(jsonPath("$.data.grade").value("RARE"))
                .andExpect(jsonPath("$.data.stage").value("GROWTH"));
    }

    private UsernamePasswordAuthenticationToken childPrincipal() {
        return new UsernamePasswordAuthenticationToken(UUID.randomUUID().toString(), null);
    }
}
