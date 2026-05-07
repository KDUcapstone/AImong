package com.aimong.backend.domain.privacy.controller;

import com.aimong.backend.domain.privacy.dto.PrivacyEventRequest;
import com.aimong.backend.domain.privacy.dto.PrivacyEventResponse;
import com.aimong.backend.domain.privacy.service.PrivacyEventService;
import com.aimong.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/privacy")
public class PrivacyController {

    private final PrivacyEventService privacyEventService;

    @PostMapping("/event")
    public ApiResponse<PrivacyEventResponse> record(
            Authentication authentication,
            @Valid @RequestBody PrivacyEventRequest request
    ) {
        return ApiResponse.success(privacyEventService.record(
                UUID.fromString(authentication.getName()),
                request.detectedType(),
                request.masked()
        ));
    }
}
