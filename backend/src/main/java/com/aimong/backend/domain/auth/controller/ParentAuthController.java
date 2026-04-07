package com.aimong.backend.domain.auth.controller;

import com.aimong.backend.domain.auth.dto.ParentRegisterRequest;
import com.aimong.backend.domain.auth.dto.ParentRegisterResponse;
import com.aimong.backend.domain.auth.dto.RegenerateCodeResponse;
import com.aimong.backend.domain.auth.service.ParentAuthService;
import com.aimong.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    private final ParentAuthService parentAuthService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ParentRegisterResponse> register(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody ParentRegisterRequest request
    ) {
        return ApiResponse.success(parentAuthService.register(authorizationHeader, request));
    }

    @PutMapping("/child/{childId}/regenerate-code")
    public ApiResponse<RegenerateCodeResponse> regenerateCode(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String childId
    ) {
        return ApiResponse.success(parentAuthService.regenerateCode(authorizationHeader, childId));
    }
}
