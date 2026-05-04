package com.aimong.backend.domain.streak.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PartnerConnectRequest(
        @NotBlank(message = "partnerCode is required.")
        @Pattern(regexp = "\\d{6}", message = "partnerCode must be 6 digits.")
        String partnerCode
) {
}
