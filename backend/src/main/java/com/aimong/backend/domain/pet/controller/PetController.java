package com.aimong.backend.domain.pet.controller;

import com.aimong.backend.domain.pet.dto.EquipPetRequest;
import com.aimong.backend.domain.pet.dto.EquipPetResponse;
import com.aimong.backend.domain.pet.dto.PetListResponse;
import com.aimong.backend.domain.pet.service.PetService;
import com.aimong.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/pet")
public class PetController {

    private static final String CHILD_SECURITY = "bearerAuth";

    private final PetService petService;

    @Operation(
            summary = "펫 목록 조회",
            description = "현재 장착 펫과 보유 펫 목록을 조회합니다.",
            security = @SecurityRequirement(name = CHILD_SECURITY)
    )
    @GetMapping
    public ApiResponse<PetListResponse> getPets(Authentication authentication) {
        return ApiResponse.success(petService.getPets(extractChildId(authentication)));
    }

    @Operation(
            summary = "펫 장착 변경",
            description = "보유 중인 펫을 현재 장착 펫으로 설정합니다.",
            security = @SecurityRequirement(name = CHILD_SECURITY)
    )
    @PutMapping("/equip")
    public ApiResponse<EquipPetResponse> equipPet(
            @Valid @RequestBody EquipPetRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(petService.equipPet(extractChildId(authentication), request.petId()));
    }

    private UUID extractChildId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
