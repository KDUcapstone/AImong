package com.aimong.backend.domain.gacha.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aimong.backend.domain.gacha.dto.FragmentListResponse;
import com.aimong.backend.domain.gacha.dto.GachaExchangeRequest;
import com.aimong.backend.domain.gacha.dto.GachaExchangeResponse;
import com.aimong.backend.domain.gacha.dto.GachaPullRequest;
import com.aimong.backend.domain.gacha.dto.GachaPullResponse;
import com.aimong.backend.domain.gacha.entity.TicketType;
import com.aimong.backend.domain.gacha.service.GachaPullService;
import com.aimong.backend.domain.pet.entity.PetGrade;
import com.aimong.backend.global.filter.FirebaseParentAuthFilter;
import com.aimong.backend.global.filter.JwtAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GachaController.class)
@AutoConfigureMockMvc(addFilters = false)
class GachaApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GachaPullService gachaPullService;

    @MockitoBean
    private FirebaseParentAuthFilter firebaseParentAuthFilter;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void pullReturnsGachaResult() throws Exception {
        UUID petId = UUID.randomUUID();
        given(gachaPullService.pull(any(UUID.class), any(TicketType.class))).willReturn(new GachaPullResponse(
                new GachaPullResponse.Result(petId, "pet_rare_003", "번개몽", "RARE", true, 0),
                2,
                0.0d,
                false,
                new GachaPullResponse.RemainingTickets(2, 0, 1)
        ));

        mockMvc.perform(post("/gacha/pull")
                        .principal(childPrincipal())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new GachaPullRequest(TicketType.NORMAL))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.result.petId").value(petId.toString()))
                .andExpect(jsonPath("$.data.result.petType").value("pet_rare_003"))
                .andExpect(jsonPath("$.data.result.petName").value("번개몽"))
                .andExpect(jsonPath("$.data.result.grade").value("RARE"))
                .andExpect(jsonPath("$.data.result.isNew").value(true))
                .andExpect(jsonPath("$.data.remainingTickets.normal").value(2));
    }

    @Test
    void getFragmentsReturnsFragmentInventory() throws Exception {
        given(gachaPullService.getFragments(any(UUID.class))).willReturn(new FragmentListResponse(List.of(
                new FragmentListResponse.FragmentSummary("NORMAL", 7, 10),
                new FragmentListResponse.FragmentSummary("RARE", 12, 30)
        )));

        mockMvc.perform(get("/gacha/fragments")
                        .principal(childPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fragments[0].grade").value("NORMAL"))
                .andExpect(jsonPath("$.data.fragments[0].count").value(7))
                .andExpect(jsonPath("$.data.fragments[0].exchangeThreshold").value(10));
    }

    @Test
    void exchangeReturnsNewPet() throws Exception {
        UUID petId = UUID.randomUUID();
        given(gachaPullService.exchange(any(UUID.class), any(PetGrade.class), any(String.class)))
                .willReturn(new GachaExchangeResponse(petId, "pet_normal_005", "NORMAL", "EGG"));

        mockMvc.perform(post("/gacha/exchange")
                        .principal(childPrincipal())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new GachaExchangeRequest(PetGrade.NORMAL, "pet_normal_005"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.petId").value(petId.toString()))
                .andExpect(jsonPath("$.data.petType").value("pet_normal_005"))
                .andExpect(jsonPath("$.data.grade").value("NORMAL"))
                .andExpect(jsonPath("$.data.stage").value("EGG"));
    }

    private UsernamePasswordAuthenticationToken childPrincipal() {
        return new UsernamePasswordAuthenticationToken(UUID.randomUUID().toString(), null);
    }
}
