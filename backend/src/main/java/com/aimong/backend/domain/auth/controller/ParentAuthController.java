package com.aimong.backend.domain.auth.controller;

import com.aimong.backend.domain.auth.dto.FcmTokenRequest;
import com.aimong.backend.domain.auth.dto.FcmTokenResponse;
import com.aimong.backend.domain.auth.dto.ParentChildrenResponse;
import com.aimong.backend.domain.auth.dto.ParentRegisterRequest;
import com.aimong.backend.domain.auth.dto.ParentRegisterResponse;
import com.aimong.backend.domain.auth.dto.RegenerateCodeResponse;
import com.aimong.backend.domain.auth.service.ParentAuthService;
import com.aimong.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/parent")
public class ParentAuthController {

    private static final String PARENT_SECURITY = "bearerAuth";

    private final ParentAuthService parentAuthService;

    @Operation(
            summary = "부모 온보딩",
            description = "Google 로그인 후 자녀 프로필 생성, 6자리 코드 발급, 스타터 티켓 3장 지급. 부모 계정당 자녀는 최대 3명까지 등록할 수 있습니다.",
            security = @SecurityRequirement(name = PARENT_SECURITY)
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "nickname": "민준"
                    }
                    """))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "생성 성공",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "data": {
                        "childId": "3f1a2b4c-1111-2222-3333-444455556666",
                        "nickname": "민준",
                        "code": "482917",
                        "starterTickets": 3
                      },
                      "requestId": "req_01HXYZABC123"
                    }
                    """))
    )
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ParentRegisterResponse> register(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody ParentRegisterRequest request
    ) {
        return ApiResponse.success(parentAuthService.register(authorizationHeader, request));
    }

    @Operation(
            summary = "자녀 코드 재발급",
            description = "기존 코드를 무효화하고 새 6자리 코드를 발급합니다",
            security = @SecurityRequirement(name = PARENT_SECURITY)
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "재발급 성공",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "data": {
                        "newCode": "719284"
                      },
                      "requestId": "req_01HXYZABC123"
                    }
                    """))
    )
    @PutMapping("/child/{childId}/regenerate-code")
    public ApiResponse<RegenerateCodeResponse> regenerateCode(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String childId
    ) {
        return ApiResponse.success(parentAuthService.regenerateCode(authorizationHeader, childId));
    }

    @Operation(
            summary = "부모 FCM 토큰 등록",
            description = "부모 디바이스의 FCM 토큰을 등록 또는 갱신합니다",
            security = @SecurityRequirement(name = PARENT_SECURITY)
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
        return ApiResponse.success(parentAuthService.registerFcmToken(currentPrincipalName(), request));
    }

    @Operation(
            summary = "부모 자녀 목록 조회",
            description = "부모가 등록한 전체 자녀 목록을 조회합니다",
            security = @SecurityRequirement(name = PARENT_SECURITY)
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "children", value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "children": [
                                          {
                                            "childId": "3f1a2b4c-1111-2222-3333-444455556666",
                                            "nickname": "싼준",
                                            "code": "482917",
                                            "profileImageType": "SPROUT",
                                            "totalXp": 150
                                          }
                                        ]
                                      },
                                      "requestId": "req_01HXYZABC123"
                                    }
                                    """),
                            @ExampleObject(name = "empty", value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "children": []
                                      },
                                      "requestId": "req_01HXYZABC123"
                                    }
                                    """)
                    })
            )
    })
    @GetMapping("/children")
    public ApiResponse<ParentChildrenResponse> getChildren() {
        return ApiResponse.success(parentAuthService.getChildren(currentPrincipalName()));
    }

    private String currentPrincipalName() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
