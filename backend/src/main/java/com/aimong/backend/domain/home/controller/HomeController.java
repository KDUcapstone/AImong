package com.aimong.backend.domain.home.controller;

import com.aimong.backend.domain.home.dto.BootstrapResponse;
import com.aimong.backend.domain.home.dto.EnergyAddRequest;
import com.aimong.backend.domain.home.dto.EnergyAddResponse;
import com.aimong.backend.domain.home.dto.EnergyResponse;
import com.aimong.backend.domain.home.dto.HomeResponse;
import com.aimong.backend.domain.home.dto.StreakCalendarResponse;
import com.aimong.backend.domain.home.service.BootstrapService;
import com.aimong.backend.domain.home.service.HomeService;
import com.aimong.backend.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;
    private final BootstrapService bootstrapService;

    @GetMapping("/app/bootstrap")
    public ApiResponse<BootstrapResponse> bootstrap(HttpServletRequest request) {
        return ApiResponse.success(bootstrapService.bootstrap(request.getHeader("Authorization")));
    }

    @GetMapping("/home")
    public ApiResponse<HomeResponse> getHome(Authentication authentication) {
        return ApiResponse.success(homeService.getHome(UUID.fromString(authentication.getName())));
    }

    @GetMapping("/home/streak-calendar")
    public ApiResponse<StreakCalendarResponse> getStreakCalendar(
            Authentication authentication,
            @RequestParam(required = false) String yearMonth
    ) {
        return ApiResponse.success(homeService.getStreakCalendar(
                UUID.fromString(authentication.getName()),
                yearMonth
        ));
    }

    @GetMapping("/energy")
    public ApiResponse<EnergyResponse> getEnergy(Authentication authentication) {
        return ApiResponse.success(homeService.getEnergy(UUID.fromString(authentication.getName())));
    }

    @PostMapping("/energy/add")
    public ApiResponse<EnergyAddResponse> addEnergy(
            @Valid @RequestBody EnergyAddRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(homeService.addEnergy(UUID.fromString(authentication.getName()), request));
    }
}
