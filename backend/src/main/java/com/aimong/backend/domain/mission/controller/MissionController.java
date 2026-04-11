package com.aimong.backend.domain.mission.controller;

import com.aimong.backend.domain.mission.dto.MissionListResponse;
import com.aimong.backend.domain.mission.dto.MissionQuestionsResponse;
import com.aimong.backend.domain.mission.dto.SubmitRequest;
import com.aimong.backend.domain.mission.dto.SubmitResponse;
import com.aimong.backend.domain.mission.service.MissionService;
import com.aimong.backend.domain.mission.service.QuizService;
import com.aimong.backend.domain.mission.service.SubmitService;
import com.aimong.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/missions")
public class MissionController {

    private final MissionService missionService;
    private final QuizService quizService;
    private final SubmitService submitService;

    @GetMapping
    public ApiResponse<MissionListResponse> getMissions(Authentication authentication) {
        return ApiResponse.success(missionService.getMissions(extractChildId(authentication)));
    }

    @GetMapping("/{missionId}/questions")
    public ApiResponse<MissionQuestionsResponse> getQuestions(
            @PathVariable UUID missionId,
            Authentication authentication
    ) {
        return ApiResponse.success(quizService.getQuestions(extractChildId(authentication), missionId));
    }

    @PostMapping("/{missionId}/submit")
    public ApiResponse<SubmitResponse> submit(
            @PathVariable UUID missionId,
            @Valid @RequestBody SubmitRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(submitService.submit(extractChildId(authentication), missionId, request));
    }

    private UUID extractChildId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
