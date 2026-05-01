package com.aimong.backend.domain.mission.service.generation;

import java.util.List;

record ValidationSubResult(
        int score,
        List<String> hardFailReasons,
        List<String> softWarnings,
        List<String> repairHints
) {
}
