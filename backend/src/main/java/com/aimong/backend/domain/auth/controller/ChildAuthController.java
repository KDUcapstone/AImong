package com.aimong.backend.domain.auth.controller;

import com.aimong.backend.domain.auth.dto.ChildLoginRequest;
import com.aimong.backend.domain.auth.dto.ChildLoginResponse;
import com.aimong.backend.domain.auth.service.ChildAuthService;
import com.aimong.backend.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/child")
public class ChildAuthController {

    private final ChildAuthService childAuthService;

    @PostMapping("/login")
    public ApiResponse<ChildLoginResponse> login(
            @Valid @RequestBody ChildLoginRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(childAuthService.login(request, httpServletRequest));
    }
}
