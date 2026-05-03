package com.aimong.backend.domain.gacha.controller;

import com.aimong.backend.domain.gacha.dto.DevGachaGrantRequest;
import com.aimong.backend.domain.gacha.dto.DevGachaGrantResponse;
import com.aimong.backend.domain.gacha.service.DevGachaGrantService;
import com.aimong.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!prod")
@RequiredArgsConstructor
@RequestMapping("/dev/gacha")
public class DevGachaController {

    private final DevGachaGrantService devGachaGrantService;

    @PostMapping("/grant")
    public ApiResponse<DevGachaGrantResponse> grant(
            @Valid @RequestBody DevGachaGrantRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(devGachaGrantService.grant(UUID.fromString(authentication.getName()), request));
    }
}
