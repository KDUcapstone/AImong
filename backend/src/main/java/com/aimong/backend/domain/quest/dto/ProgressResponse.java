package com.aimong.backend.domain.quest.dto;

public record ProgressResponse(
        int current,
        int required
) {
}
