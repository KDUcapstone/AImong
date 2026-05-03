package com.aimong.backend.domain.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aimong.backend.domain.auth.dto.ChildLoginRequest;
import com.aimong.backend.domain.auth.dto.ChildLoginResponse;
import com.aimong.backend.domain.auth.dto.FcmTokenRequest;
import com.aimong.backend.domain.auth.dto.FcmTokenResponse;
import com.aimong.backend.domain.auth.dto.ParentChildrenResponse;
import com.aimong.backend.domain.auth.dto.ParentRegisterRequest;
import com.aimong.backend.domain.auth.dto.ParentRegisterResponse;
import com.aimong.backend.domain.auth.dto.RegenerateCodeResponse;
import com.aimong.backend.domain.auth.service.ChildAuthService;
import com.aimong.backend.domain.auth.service.ParentAuthService;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
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
import org.springframework.security.core.context.SecurityContextHolder;
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
        ParentRegisterResponse response = new ParentRegisterResponse(childId, "민준", "482917", 3);

        given(parentAuthService.register(eq("Bearer parent-token"), any(ParentRegisterRequest.class)))
                .willReturn(response);

        mockMvc.perform(post("/parent/register")
                        .contentType(APPLICATION_JSON)
                        .header("Authorization", "Bearer parent-token")
                        .content(objectMapper.writeValueAsBytes(new ParentRegisterRequest("민준"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.data.childId").value(childId.toString()))
                .andExpect(jsonPath("$.data.nickname").value("민준"))
                .andExpect(jsonPath("$.data.code").value("482917"))
                .andExpect(jsonPath("$.data.starterTickets").value(3));
    }

    @Test
    void childLoginReturnsSessionPayloadFromSpec() throws Exception {
        UUID childId = UUID.randomUUID();
        ChildLoginResponse response = new ChildLoginResponse(
                childId,
                "민준",
                "child-session-token",
                "DEFAULT",
                25
        );

        given(childAuthService.login(any(ChildLoginRequest.class), any())).willReturn(response);

        mockMvc.perform(post("/child/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ChildLoginRequest("482917"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.data.childId").value(childId.toString()))
                .andExpect(jsonPath("$.data.nickname").value("민준"))
                .andExpect(jsonPath("$.data.sessionToken").value("child-session-token"))
                .andExpect(jsonPath("$.data.profileImageType").value("DEFAULT"))
                .andExpect(jsonPath("$.data.totalXp").value(25));
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
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.data.newCode").value("135790"));
    }

    @Test
    void childLoginValidationFailureReturnsSpecMessage() throws Exception {
        given(childAuthService.login(any(ChildLoginRequest.class), any()))
                .willThrow(new AimongException(ErrorCode.CHILD_CODE_INVALID_FORMAT));

        mockMvc.perform(post("/child/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new ChildLoginRequest("12"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.error.message").value("올바른 형식의 코드를 입력해 주세요."));
    }

    @Test
    void registerParentFcmTokenReturnsRegisteredTrue() throws Exception {
        given(parentAuthService.registerFcmToken(eq("firebase-parent"), any(FcmTokenRequest.class)))
                .willReturn(new FcmTokenResponse(true));

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("firebase-parent", null));
        try {
            mockMvc.perform(post("/parent/fcm-token")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(new FcmTokenRequest("parent-fcm-token"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.requestId").exists())
                    .andExpect(jsonPath("$.data.registered").value(true));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void registerChildFcmTokenReturnsRegisteredTrue() throws Exception {
        given(childAuthService.registerFcmToken(any(UUID.class), any(FcmTokenRequest.class)))
                .willReturn(new FcmTokenResponse(true));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(UUID.randomUUID().toString(), null)
        );
        try {
            mockMvc.perform(post("/child/fcm-token")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(new FcmTokenRequest("child-fcm-token"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.requestId").exists())
                    .andExpect(jsonPath("$.data.registered").value(true));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void getParentChildrenReturnsExactContract() throws Exception {
        UUID childId = UUID.randomUUID();
        given(parentAuthService.getChildren("firebase-parent"))
                .willReturn(new ParentChildrenResponse(
                        List.of(new ParentChildrenResponse.ChildSummary(
                                childId,
                                "싼준",
                                "482917",
                                "SPROUT",
                                150
                        ))
                ));

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("firebase-parent", null));
        try {
            mockMvc.perform(get("/parent/children"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.requestId").exists())
                    .andExpect(jsonPath("$.data.children[0].childId").value(childId.toString()))
                    .andExpect(jsonPath("$.data.children[0].nickname").value("싼준"))
                    .andExpect(jsonPath("$.data.children[0].code").value("482917"))
                    .andExpect(jsonPath("$.data.children[0].profileImageType").value("SPROUT"))
                    .andExpect(jsonPath("$.data.children[0].totalXp").value(150));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
