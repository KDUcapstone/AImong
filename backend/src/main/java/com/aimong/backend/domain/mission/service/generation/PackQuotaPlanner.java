package com.aimong.backend.domain.mission.service.generation;

import com.aimong.backend.domain.mission.config.QuestionGenerationProperties;
import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PackQuotaPlanner {

    private final QuestionGenerationProperties properties;

    public PackQuota planForPack(int packNo) {
        Map<QuestionType, Integer> typeQuota = new EnumMap<>(QuestionType.class);
        typeQuota.put(QuestionType.OX, 2);
        typeQuota.put(QuestionType.MULTIPLE, 3);
        typeQuota.put(QuestionType.FILL, 2);
        typeQuota.put(QuestionType.SITUATION, 3);

        Map<DifficultyBand, Integer> bandQuota = new EnumMap<>(DifficultyBand.class);
        bandQuota.put(DifficultyBand.LOW, 5);
        bandQuota.put(DifficultyBand.MEDIUM, packNo <= 4 ? 3 : 4);
        bandQuota.put(DifficultyBand.HIGH, packNo <= 4 ? 2 : 1);

        return new PackQuota(packNo, properties.questionsPerPack(), typeQuota, bandQuota);
    }

    public MissionQuota planForMission() {
        Map<QuestionType, Integer> typeQuota = new EnumMap<>(QuestionType.class);
        typeQuota.put(QuestionType.OX, 12);
        typeQuota.put(QuestionType.MULTIPLE, 18);
        typeQuota.put(QuestionType.FILL, 12);
        typeQuota.put(QuestionType.SITUATION, 18);

        Map<DifficultyBand, Integer> bandQuota = new EnumMap<>(DifficultyBand.class);
        bandQuota.put(DifficultyBand.LOW, 30);
        bandQuota.put(DifficultyBand.MEDIUM, 20);
        bandQuota.put(DifficultyBand.HIGH, 10);

        return new MissionQuota(
                properties.targetPacksPerMission(),
                properties.targetPoolPerMission(),
                typeQuota,
                bandQuota,
                List.of(
                        planForPack(1),
                        planForPack(2),
                        planForPack(3),
                        planForPack(4),
                        planForPack(5),
                        planForPack(6)
                )
        );
    }

    public record PackQuota(
            int packNo,
            int questionCount,
            Map<QuestionType, Integer> typeQuota,
            Map<DifficultyBand, Integer> difficultyQuota
    ) {
    }

    public record MissionQuota(
            int packCount,
            int questionCount,
            Map<QuestionType, Integer> typeQuota,
            Map<DifficultyBand, Integer> difficultyQuota,
            List<PackQuota> packs
    ) {
    }
}
