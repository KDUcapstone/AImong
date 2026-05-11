package com.aimong.backend.domain.home.controller;

import com.aimong.backend.domain.home.dto.BootstrapResponse;
import com.aimong.backend.domain.home.dto.EnergyResponse;
import com.aimong.backend.domain.home.dto.HomeResponse;
import com.aimong.backend.domain.home.dto.StreakCalendarResponse;
import com.aimong.backend.domain.home.service.BootstrapService;
import com.aimong.backend.domain.home.service.HomeService;
import com.aimong.backend.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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
}
