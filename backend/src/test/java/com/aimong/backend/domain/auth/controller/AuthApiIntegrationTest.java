package com.aimong.backend.domain.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aimong.backend.domain.auth.dto.ChildLoginRequest;
import com.aimong.backend.domain.auth.dto.ChildLoginResponse;
import com.aimong.backend.domain.auth.dto.ParentRegisterRequest;
import com.aimong.backend.domain.auth.dto.ParentRegisterResponse;
import com.aimong.backend.domain.auth.dto.RegenerateCodeResponse;
import com.aimong.backend.domain.auth.service.ChildAuthService;
import com.aimong.backend.domain.auth.service.ParentAuthService;
import com.aimong.backend.global.filter.FirebaseParentAuthFilter;
import com.aimong.backend.global.filter.JwtAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({ParentAuthController.class, ChildAuthController.class})
@AutoConfigureMockMvc(addFilters = false)
class AuthApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ParentAuthService parentAuthService;

    @MockitoBean
    private ChildAuthService childAuthService;

    @MockitoBean
    private FirebaseParentAuthFilter firebaseParentAuthFilter;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void registerReturnsCreatedPayloadFromApiSpec() throws Exception {
        UUID childId = UUID.randomUUID();
        ParentRegisterResponse response = new ParentRegisterResponse(childId, "민지", "482917", 3);

        given(parentAuthService.register(eq("Bearer parent-token"), any(ParentRegisterRequest.class)))
                .willReturn(response);

        mockMvc.perform(post("/parent/register")
                        .contentType(APPLICATION_JSON)
                        .header("Authorization", "Bearer parent-token")
                        .content(objectMapper.writeValueAsBytes(new ParentRegisterRequest("민지"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.data.childId").value(childId.toString()))
                .andExpect(jsonPath("$.data.nickname").value("민지"))
                .andExpect(jsonPath("$.data.code").value("482917"))
                .andExpect(jsonPath("$.data.starterTickets").value(3));
    }

    @Test
    void childLoginReturnsLevelAndSessionPayload() throws Exception {
        UUID childId = UUID.randomUUID();
        ChildLoginResponse response = new ChildLoginResponse(
                childId,
                "민지",
                "child-session-token",
                "LEVEL_1",
                25,
                1
        );

        given(childAuthService.login(any(ChildLoginRequest.class), any())).willReturn(response);

        mockMvc.perform(post("/child/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ChildLoginRequest("482917"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.data.childId").value(childId.toString()))
                .andExpect(jsonPath("$.data.nickname").value("민지"))
                .andExpect(jsonPath("$.data.sessionToken").value("child-session-token"))
                .andExpect(jsonPath("$.data.profileImageType").value("LEVEL_1"))
                .andExpect(jsonPath("$.data.totalXp").value(25))
                .andExpect(jsonPath("$.data.level").value(1));
    }

    @Test
    void regenerateCodeReturnsUpdatedCodePayload() throws Exception {
        UUID childId = UUID.randomUUID();
        RegenerateCodeResponse response = new RegenerateCodeResponse("135790");

        given(parentAuthService.regenerateCode("Bearer parent-token", childId.toString())).willReturn(response);

        mockMvc.perform(put("/parent/child/{childId}/regenerate-code", childId)
                        .header("Authorization", "Bearer parent-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.data.newCode").value("135790"));
    }

    @Test
    void childLoginValidationFailureReturnsErrorWithoutData() throws Exception {
        mockMvc.perform(post("/child/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ChildLoginRequest("12"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }
}
