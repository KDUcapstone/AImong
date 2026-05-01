package com.aimong.backend.tools.questionbank;

import java.util.List;

public record SyntheticValidationFixture(
        String id,
        String category,
        AuditQuestion candidate,
        List<AuditQuestion> candidates,
        List<String> expectedHardFails,
        List<String> expectedWarnings,
        List<String> expectedBatchSignals
) {
    public boolean isBatchCase() {
        return candidates != null && !candidates.isEmpty();
    }
}
