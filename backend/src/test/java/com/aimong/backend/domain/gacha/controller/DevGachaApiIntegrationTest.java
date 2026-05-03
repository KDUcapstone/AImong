package com.aimong.backend.domain.gacha.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aimong.backend.domain.gacha.dto.DevGachaGrantRequest;
import com.aimong.backend.domain.gacha.dto.DevGachaGrantResponse;
import com.aimong.backend.domain.gacha.dto.FragmentListResponse;
import com.aimong.backend.domain.gacha.dto.GachaPullResponse;
import com.aimong.backend.domain.gacha.service.DevGachaGrantService;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DevGachaController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class DevGachaApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DevGachaGrantService devGachaGrantService;

    @MockitoBean
    private FirebaseParentAuthFilter firebaseParentAuthFilter;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void grantReturnsTicketsAndFragmentsForSmokeTesting() throws Exception {
        given(devGachaGrantService.grant(any(UUID.class), any(DevGachaGrantRequest.class)))
                .willReturn(new DevGachaGrantResponse(
                        new GachaPullResponse.RemainingTickets(3, 1, 0),
                        new FragmentListResponse(List.of(
                                new FragmentListResponse.FragmentSummary("NORMAL", 10, 10),
                                new FragmentListResponse.FragmentSummary("RARE", 30, 30)
                        ))
                ));

        mockMvc.perform(post("/dev/gacha/grant")
                        .principal(new UsernamePasswordAuthenticationToken(UUID.randomUUID().toString(), null))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new DevGachaGrantRequest(
                                3, 1, 0, 10, 30, 0, 0
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.remainingTickets.normal").value(3))
                .andExpect(jsonPath("$.data.remainingTickets.rare").value(1))
                .andExpect(jsonPath("$.data.fragments.fragments[0].grade").value("NORMAL"))
                .andExpect(jsonPath("$.data.fragments.fragments[0].count").value(10));
    }
}
