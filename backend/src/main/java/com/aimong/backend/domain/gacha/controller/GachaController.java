package com.aimong.backend.domain.gacha.controller;

import com.aimong.backend.domain.gacha.dto.FragmentListResponse;
import com.aimong.backend.domain.gacha.dto.GachaExchangeRequest;
import com.aimong.backend.domain.gacha.dto.GachaExchangeResponse;
import com.aimong.backend.domain.gacha.dto.GachaPullRequest;
import com.aimong.backend.domain.gacha.dto.GachaPullResponse;
import com.aimong.backend.domain.gacha.service.GachaPullService;
import com.aimong.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/gacha")
public class GachaController {

    private final GachaPullService gachaPullService;

    @PostMapping("/pull")
    public ApiResponse<GachaPullResponse> pull(
            @Valid @RequestBody GachaPullRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(gachaPullService.pull(extractChildId(authentication), request.ticketType()));
    }

    @GetMapping("/fragments")
    public ApiResponse<FragmentListResponse> getFragments(Authentication authentication) {
        return ApiResponse.success(gachaPullService.getFragments(extractChildId(authentication)));
    }

    @PostMapping("/exchange")
    public ApiResponse<GachaExchangeResponse> exchange(
            @Valid @RequestBody GachaExchangeRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(gachaPullService.exchange(
                extractChildId(authentication),
                request.grade(),
                request.petType()
        ));
    }

    private UUID extractChildId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
