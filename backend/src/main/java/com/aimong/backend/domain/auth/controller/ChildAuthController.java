package com.aimong.backend.domain.auth.controller;

import com.aimong.backend.domain.auth.dto.ChildLoginRequest;
import com.aimong.backend.domain.auth.dto.ChildLoginResponse;
import com.aimong.backend.domain.auth.dto.FcmTokenRequest;
import com.aimong.backend.domain.auth.dto.FcmTokenResponse;
import com.aimong.backend.domain.auth.service.ChildAuthService;
import com.aimong.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/child")
public class ChildAuthController {

    private static final String CHILD_SECURITY = "bearerAuth";

    private final ChildAuthService childAuthService;

    @Operation(summary = "자녀 코드 로그인", description = "6자리 코드로 자녀 세션 토큰을 발급합니다")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "code": "482917"
                    }
                    """))
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {
                              "success": true,
                              "data": {
                                "childId": "3f1a2b4c-1111-2222-3333-444455556666",
                                "nickname": "민준",
                                "sessionToken": "eyJhbGciOiJIUzI1NiJ9...",
                                "profileImageType": "DEFAULT",
                                "totalXp": 0
                              },
                              "requestId": "req_01HXYZABC123"
                            }
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "코드 형식 오류",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "code_required", value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "BAD_REQUEST",
                                        "message": "코드를 입력해주세요"
                                      },
                                      "requestId": "req_01HXYZABC123"
                                    }
                                    """),
                            @ExampleObject(name = "code_invalid", value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "BAD_REQUEST",
                                        "message": "올바른 형식의 코드를 입력해주세요"
                                      },
                                      "requestId": "req_01HXYZABC123"
                                    }
                                    """)
                    })
            )
    })
    @PostMapping("/login")
    public ApiResponse<ChildLoginResponse> login(
            @Valid @RequestBody ChildLoginRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(childAuthService.login(request, httpServletRequest));
    }

    @Operation(
            summary = "자녀 FCM 토큰 등록",
            description = "자녀 디바이스의 FCM 토큰을 등록 또는 갱신합니다",
            security = @SecurityRequirement(name = CHILD_SECURITY)
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "fcmToken": "fCm_device_token_string"
                    }
                    """))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "등록 성공",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "data": {
                        "registered": true
                      },
                      "requestId": "req_01HXYZABC123"
                    }
                    """))
    )
    @PostMapping("/fcm-token")
    public ApiResponse<FcmTokenResponse> registerFcmToken(
            @Valid @RequestBody FcmTokenRequest request
    ) {
        return ApiResponse.success(childAuthService.registerFcmToken(
                UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName()),
                request
        ));
    }
}
