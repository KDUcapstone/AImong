package com.aimong.backend.domain.customquest.controller;

import com.aimong.backend.domain.customquest.dto.ChildCustomQuestListResponse;
import com.aimong.backend.domain.customquest.dto.CreateCustomQuestRequest;
import com.aimong.backend.domain.customquest.dto.CustomQuestItemResponse;
import com.aimong.backend.domain.customquest.dto.CustomQuestListResponse;
import com.aimong.backend.domain.customquest.dto.CustomQuestStatusResponse;
import com.aimong.backend.domain.customquest.service.CustomQuestService;
import com.aimong.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CustomQuestController {

    private final CustomQuestService customQuestService;

    @PostMapping("/parent/children/{childId}/custom-quests")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CustomQuestItemResponse> createParentQuest(
            @PathVariable UUID childId,
            @Valid @RequestBody CreateCustomQuestRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(customQuestService.createParentQuest(authentication.getName(), childId, request));
    }

    @GetMapping("/parent/children/{childId}/custom-quests")
    public ApiResponse<CustomQuestListResponse> getParentQuests(
            @PathVariable UUID childId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        return ApiResponse.success(customQuestService.getParentQuests(
                authentication.getName(),
                childId,
                status,
                page,
                size
        ));
    }

    @PatchMapping("/parent/custom-quests/{questId}/confirm")
    public ApiResponse<CustomQuestStatusResponse> confirmParentQuest(
            @PathVariable UUID questId,
            Authentication authentication
    ) {
        return ApiResponse.success(customQuestService.confirmParentQuest(authentication.getName(), questId));
    }

    @DeleteMapping("/parent/custom-quests/{questId}")
    public ApiResponse<CustomQuestStatusResponse> cancelParentQuest(
            @PathVariable UUID questId,
            Authentication authentication
    ) {
        return ApiResponse.success(customQuestService.cancelParentQuest(authentication.getName(), questId));
    }

    @GetMapping("/child/custom-quests")
    public ApiResponse<ChildCustomQuestListResponse> getChildQuests(Authentication authentication) {
        return ApiResponse.success(customQuestService.getChildQuests(extractChildId(authentication)));
    }

    @PostMapping("/child/custom-quests/{questId}/complete")
    public ApiResponse<CustomQuestStatusResponse> completeChildQuest(
            @PathVariable UUID questId,
            Authentication authentication
    ) {
        return ApiResponse.success(customQuestService.completeChildQuest(extractChildId(authentication), questId));
    }

    private UUID extractChildId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
