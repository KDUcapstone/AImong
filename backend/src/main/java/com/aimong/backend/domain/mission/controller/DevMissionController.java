package com.aimong.backend.domain.mission.controller;

import com.aimong.backend.domain.mission.dto.DevMissionGenerateRequest;
import com.aimong.backend.domain.mission.dto.DevMissionGenerateResponse;
import com.aimong.backend.domain.mission.service.DevMissionGenerationService;
import com.aimong.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!prod")
@RequiredArgsConstructor
@RequestMapping("/dev/missions")
public class DevMissionController {

    private final DevMissionGenerationService devMissionGenerationService;

    @PostMapping("/generate")
    public ApiResponse<DevMissionGenerateResponse> generate(
            @Valid @RequestBody DevMissionGenerateRequest request
    ) {
        return ApiResponse.success(devMissionGenerationService.generate(request));
    }
}
