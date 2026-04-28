package com.aimong.backend.domain.mission.service.question;

import com.aimong.backend.domain.mission.entity.QuestionBank;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RecompositionSelector {

    private static final int REQUIRED_LOW = 5;
    private static final int REQUIRED_MEDIUM = 3;
    private static final int REQUIRED_HIGH = 2;
    private static final String SHORTAGE_REASON = "INSUFFICIENT_DIFFICULTY_POOL";

    public SelectionResult select(
            List<QuestionBank> lowPool,
            List<QuestionBank> mediumPool,
            List<QuestionBank> highPool
    ) {
        int lowAvailable = lowPool.size();
        int mediumAvailable = mediumPool.size();
        int highAvailable = highPool.size();

        int lowMissing = Math.max(0, REQUIRED_LOW - lowAvailable);
        int mediumMissing = Math.max(0, REQUIRED_MEDIUM - mediumAvailable);
        int highMissing = Math.max(0, REQUIRED_HIGH - highAvailable);

        if (lowMissing > 0 || mediumMissing > 0 || highMissing > 0) {
            return new Shortage(
                    new ShortageDetails(
                            lowMissing,
                            mediumMissing,
                            highMissing,
                            SHORTAGE_REASON
                    )
            );
        }

        List<QuestionBank> selected = new ArrayList<>(REQUIRED_LOW + REQUIRED_MEDIUM + REQUIRED_HIGH);
        selected.addAll(lowPool.subList(0, REQUIRED_LOW));
        selected.addAll(mediumPool.subList(0, REQUIRED_MEDIUM));
        selected.addAll(highPool.subList(0, REQUIRED_HIGH));
        return new Composed(List.copyOf(selected));
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
            String reason
    ) {
        public int totalMissing() {
            return lowMissing + mediumMissing + highMissing;
        }
    }
}
