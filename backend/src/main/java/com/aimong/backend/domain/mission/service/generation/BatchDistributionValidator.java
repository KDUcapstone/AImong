package com.aimong.backend.domain.mission.service.generation;

import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class BatchDistributionValidator {

    private static final Map<QuestionType, Double> BASELINE_TYPE_RATIOS = Map.of(
            QuestionType.OX, 0.20d,
            QuestionType.MULTIPLE, 0.30d,
            QuestionType.FILL, 0.20d,
            QuestionType.SITUATION, 0.30d
    );

    public BatchDistributionReport validate(List<StructuredQuestionSchema> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return new BatchDistributionReport(100, List.of(), List.of());
        }

        List<String> warnings = new ArrayList<>();
        List<String> repairHints = new ArrayList<>();

        Map<QuestionType, Integer> typeCounts = new EnumMap<>(QuestionType.class);
        Map<DifficultyBand, Integer> bandCounts = new EnumMap<>(DifficultyBand.class);
        for (StructuredQuestionSchema candidate : candidates) {
            if (candidate.type() != null) {
                typeCounts.merge(candidate.type(), 1, Integer::sum);
            }
            if (candidate.difficultyBand() != null) {
                bandCounts.merge(candidate.difficultyBand(), 1, Integer::sum);
            }
        }

        int total = candidates.size();
        for (Map.Entry<QuestionType, Double> baseline : BASELINE_TYPE_RATIOS.entrySet()) {
            double actualRatio = typeCounts.getOrDefault(baseline.getKey(), 0) / (double) total;
            if (Math.abs(actualRatio - baseline.getValue()) > 0.20d) {
                warnings.add("batch.type_ratio_drift." + baseline.getKey().name());
                repairHints.add("Rebalance the batch so question types do not drift too far from the baseline mix.");
            }
        }

        long distinctTypes = typeCounts.values().stream().filter(count -> count > 0).count();
        if (distinctTypes <= 1 && total >= 4) {
            warnings.add("batch.type_variety_too_low");
            repairHints.add("Mix more than one question type in the same refill batch.");
        }

        int low = bandCounts.getOrDefault(DifficultyBand.LOW, 0);
        int medium = bandCounts.getOrDefault(DifficultyBand.MEDIUM, 0);
        int high = bandCounts.getOrDefault(DifficultyBand.HIGH, 0);
        if (high > 0 && high >= low && total >= 6) {
            warnings.add("batch.high_band_overrepresented");
            repairHints.add("Keep HIGH difficulty items as a minority within the refill batch.");
        }
        if (medium == 0 && high > 0 && total >= 4) {
            warnings.add("batch.missing_middle_band");
            repairHints.add("Include MEDIUM questions so the batch difficulty ramps more naturally.");
        }

        int score = Math.max(0, 100 - warnings.size() * 15);
        return new BatchDistributionReport(score, List.copyOf(warnings), repairHints.stream().distinct().toList());
    }

    public record BatchDistributionReport(
            int score,
            List<String> warnings,
            List<String> repairHints
    ) {
    }
}
