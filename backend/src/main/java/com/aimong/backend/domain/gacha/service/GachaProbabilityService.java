package com.aimong.backend.domain.gacha.service;

import com.aimong.backend.domain.pet.entity.PetGrade;
import com.aimong.backend.domain.gacha.entity.TicketType;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class GachaProbabilityService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final Map<PetGrade, List<PetDefinition>> PET_POOL = Map.of(
            PetGrade.NORMAL, List.of(
                    new PetDefinition("pet_normal_001", "몽실토끼"),
                    new PetDefinition("pet_normal_002", "방울펭귄"),
                    new PetDefinition("pet_normal_003", "잎새여우"),
                    new PetDefinition("pet_normal_004", "젤리곰"),
                    new PetDefinition("pet_normal_005", "별콩새"),
                    new PetDefinition("pet_normal_006", "조개물개"),
                    new PetDefinition("pet_normal_007", "밤송이햄"),
                    new PetDefinition("pet_normal_008", "바람다람"),
                    new PetDefinition("pet_normal_009", "달빛냥"),
                    new PetDefinition("pet_normal_010", "꽃사슴")
            ),
            PetGrade.RARE, List.of(
                    new PetDefinition("pet_rare_001", "번개람쥐"),
                    new PetDefinition("pet_rare_002", "눈꽃여우"),
                    new PetDefinition("pet_rare_003", "수정사슴"),
                    new PetDefinition("pet_rare_004", "구름양"),
                    new PetDefinition("pet_rare_005", "해초용"),
                    new PetDefinition("pet_rare_006", "그림자냥")
            ),
            PetGrade.EPIC, List.of(
                    new PetDefinition("pet_epic_001", "화염늑대"),
                    new PetDefinition("pet_epic_002", "폭풍매"),
                    new PetDefinition("pet_epic_003", "흑요호랑"),
                    new PetDefinition("pet_epic_004", "루미드래곤")
            ),
            PetGrade.LEGEND, List.of(
                    new PetDefinition("pet_legend_001", "태양봉황"),
                    new PetDefinition("pet_legend_002", "월광기린")
            )
    );

    private static final double[][] NORMAL_TICKET_PROBABILITIES = {
            {0.75d, 0.21d, 0.035d, 0.005d},
            {0.66d, 0.24d, 0.075d, 0.025d},
            {0.56d, 0.27d, 0.13d, 0.04d},
            {0.44d, 0.29d, 0.22d, 0.05d}
    };

    private static final PetGrade[] GRADES = {
            PetGrade.NORMAL,
            PetGrade.RARE,
            PetGrade.EPIC,
            PetGrade.LEGEND
    };

    public DrawResult draw(TicketType ticketType, int nextPullCount, int srMissCount) {
        double[] probabilities = probabilitiesFor(ticketType, nextPullCount, srMissCount);
        PetGrade grade = weightedRandom(probabilities);
        List<PetDefinition> pool = PET_POOL.get(grade);
        String petType = pool.get(SECURE_RANDOM.nextInt(pool.size())).code();
        return new DrawResult(petType, grade, appliedSrBonus(ticketType, nextPullCount, srMissCount));
    }

    public boolean isValidPetTypeForGrade(PetGrade grade, String petType) {
        return PET_POOL.getOrDefault(grade, List.of()).stream()
                .anyMatch(pet -> pet.code().equals(petType));
    }

    public String petNameOf(String petType) {
        return PET_POOL.values().stream()
                .flatMap(List::stream)
                .filter(pet -> pet.code().equals(petType))
                .map(PetDefinition::name)
                .findFirst()
                .orElse(petType);
    }

    private double[] probabilitiesFor(TicketType ticketType, int nextPullCount, int srMissCount) {
        double[] probabilities = baseNormalProbabilities(nextPullCount);
        double appliedSrBonus = appliedSrBonus(ticketType, nextPullCount, srMissCount);
        probabilities[0] -= appliedSrBonus;
        probabilities[2] += appliedSrBonus;
        return probabilities;
    }

    private double appliedSrBonus(TicketType ticketType, int nextPullCount, int srMissCount) {
        if (ticketType != TicketType.NORMAL) {
            return 0d;
        }
        double normalProbability = baseNormalProbabilities(nextPullCount)[0];
        double rawBonus = srMissCount >= 10 ? (srMissCount - 9) * 0.01d : 0d;
        return Math.min(rawBonus, normalProbability);
    }

    private double[] baseNormalProbabilities(int nextPullCount) {
        int level = nextPullCount < 20 ? 0 : nextPullCount < 50 ? 1 : nextPullCount < 100 ? 2 : 3;
        return NORMAL_TICKET_PROBABILITIES[level].clone();
    }

    private PetGrade weightedRandom(double[] probabilities) {
        double random = SECURE_RANDOM.nextDouble();
        double cumulative = 0d;
        for (int index = 0; index < probabilities.length; index++) {
            cumulative += probabilities[index];
            if (random < cumulative) {
                return GRADES[index];
            }
        }
        return PetGrade.LEGEND;
    }

    public record DrawResult(
            String petType,
            PetGrade grade,
            double appliedSrBonus
    ) {
    }

    private record PetDefinition(
            String code,
            String name
    ) {
    }
}
