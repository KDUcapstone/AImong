package com.aimong.backend.domain.mission.service;

import com.aimong.backend.domain.mission.config.MissionQuestionProperties;
import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.repository.QuestionBankRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuestionPoolMetricsCollector {

    private final QuestionBankRepository questionBankRepository;
    private final MissionQuestionProperties missionQuestionProperties;

    public QuestionPoolMetrics collect(UUID missionId) {
        long activeCount = questionBankRepository.countByMissionIdAndIsActiveTrue(missionId);
        List<Short> intactPackNumbers = questionBankRepository.findIntactPackNumbers(
                missionId,
                missionQuestionProperties.setSize()
        );

        return new QuestionPoolMetrics(
                missionId,
                activeCount,
                intactPackNumbers.size(),
                questionBankRepository.countByMissionIdAndIsActiveTrueAndDifficultyBand(missionId, DifficultyBand.LOW),
                questionBankRepository.countByMissionIdAndIsActiveTrueAndDifficultyBand(missionId, DifficultyBand.MEDIUM),
                questionBankRepository.countByMissionIdAndIsActiveTrueAndDifficultyBand(missionId, DifficultyBand.HIGH),
                intactPackNumbers
        );
    }

    public record QuestionPoolMetrics(
            UUID missionId,
            long activeCount,
            int intactPackCount,
            long lowBandCount,
            long mediumBandCount,
            long highBandCount,
            List<Short> intactPackNumbers
    ) {
    }
}
