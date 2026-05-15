package com.aimong.backend.domain.auth.dto;

import com.aimong.backend.domain.auth.entity.ProfileImageType;
import jakarta.validation.constraints.Size;

public record UpdateChildProfileRequest(
        @Size(max = 20, message = "닉네임은 20자 이하여야 합니다.")
        String nickname,
        ProfileImageType profileImageType
) {
    public boolean hasNoValues() {
        return nickname == null && profileImageType == null;
    }
}
