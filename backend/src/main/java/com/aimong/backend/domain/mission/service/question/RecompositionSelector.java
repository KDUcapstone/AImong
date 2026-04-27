package com.aimong.backend.domain.mission.service.question;

import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecompositionSelector {

    private static final int REQUIRED_LOW = 5;
    private static final int REQUIRED_MEDIUM = 3;
    private static final int REQUIRED_HIGH = 2;
    private static final String SHORTAGE_REASON = "INSUFFICIENT_DIFFICULTY_BAND_POOL";

    public SelectionResult select(ApprovedQuestionProvider.ApprovedQuestionPool approvedPool) {
        Map<DifficultyBand, List<QuestionBank>> byBand = splitByBand(approvedPool.questions());
        int lowAvailable = byBand.get(DifficultyBand.LOW).size();
        int mediumAvailable = byBand.get(DifficultyBand.MEDIUM).size();
        int highAvailable = byBand.get(DifficultyBand.HIGH).size();

        int lowMissing = Math.max(0, REQUIRED_LOW - lowAvailable);
        int mediumMissing = Math.max(0, REQUIRED_MEDIUM - mediumAvailable);
        int highMissing = Math.max(0, REQUIRED_HIGH - highAvailable);

        if (lowMissing > 0 || mediumMissing > 0 || highMissing > 0) {
            return new Shortage(
                    new ShortageDetails(
                            lowMissing,
                            mediumMissing,
                            highMissing,
                            approvedPool.excludedBySolved(),
                            SHORTAGE_REASON,
                            new CandidatePoolCounts(
                                    approvedPool.questions().size(),
                                    lowAvailable,
                                    mediumAvailable,
                                    highAvailable
                            )
                    )
            );
        }

        List<QuestionBank> selected = new ArrayList<>(REQUIRED_LOW + REQUIRED_MEDIUM + REQUIRED_HIGH);
        selected.addAll(byBand.get(DifficultyBand.LOW).subList(0, REQUIRED_LOW));
        selected.addAll(byBand.get(DifficultyBand.MEDIUM).subList(0, REQUIRED_MEDIUM));
        selected.addAll(byBand.get(DifficultyBand.HIGH).subList(0, REQUIRED_HIGH));
        return new Composed(List.copyOf(selected));
    }

    private Map<DifficultyBand, List<QuestionBank>> splitByBand(List<QuestionBank> questions) {
        Map<DifficultyBand, List<QuestionBank>> byBand = new EnumMap<>(DifficultyBand.class);
        byBand.put(DifficultyBand.LOW, new ArrayList<>());
        byBand.put(DifficultyBand.MEDIUM, new ArrayList<>());
        byBand.put(DifficultyBand.HIGH, new ArrayList<>());
        for (QuestionBank question : questions) {
            DifficultyBand band = question.getDifficultyBand();
            if (band == null || !byBand.containsKey(band)) {
                continue;
            }
            byBand.get(band).add(question);
        }
        return byBand;
    }

    public sealed interface SelectionResult permits Composed, Shortage {
    }

    public record Composed(List<QuestionBank> questionSet) implements SelectionResult {
    }

    public record Shortage(ShortageDetails details) implements SelectionResult {
    }

    public record ShortageDetails(
            int lowMissing,
            int mediumMissing,
            int highMissing,
            int excludedBySolved,
            String reason,
            CandidatePoolCounts candidatePoolCounts
    ) {
        public int totalMissing() {
            return lowMissing + mediumMissing + highMissing;
        }
    }

    public record CandidatePoolCounts(
            int total,
            int low,
            int medium,
            int high
    ) {
    }
}
