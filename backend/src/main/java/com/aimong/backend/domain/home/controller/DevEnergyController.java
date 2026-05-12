package com.aimong.backend.domain.home.controller;

import com.aimong.backend.domain.home.dto.DevEnergyAddRequest;
import com.aimong.backend.domain.home.dto.DevEnergyAddResponse;
import com.aimong.backend.domain.home.service.DevEnergyService;
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
@Profile({"local", "dev", "test"})
@RequiredArgsConstructor
@RequestMapping("/dev/energy")
public class DevEnergyController {

    private final DevEnergyService devEnergyService;

    @PostMapping("/add")
    public ApiResponse<DevEnergyAddResponse> add(
            @Valid @RequestBody DevEnergyAddRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(devEnergyService.add(UUID.fromString(authentication.getName()), request));
    }
}
