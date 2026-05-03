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

    private static final Map<PetGrade, List<String>> PET_POOL = Map.of(
            PetGrade.NORMAL, List.of("RABBIT", "TURTLE", "HEDGEHOG", "SQUIRREL"),
            PetGrade.RARE, List.of("FOX", "OWL", "CAT"),
            PetGrade.EPIC, List.of("DRAGON", "UNICORN"),
            PetGrade.LEGEND, List.of("PHOENIX")
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
        List<String> pool = PET_POOL.get(grade);
        String petType = pool.get(SECURE_RANDOM.nextInt(pool.size()));
        return new DrawResult(petType, grade, appliedSrBonus(ticketType, nextPullCount, srMissCount));
    }

    public boolean isValidPetTypeForGrade(PetGrade grade, String petType) {
        return PET_POOL.getOrDefault(grade, List.of()).contains(petType);
    }

    private double[] probabilitiesFor(TicketType ticketType, int nextPullCount, int srMissCount) {
        double[] probabilities = baseNormalProbabilities(nextPullCount);
        double appliedSrBonus = appliedSrBonus(TicketType.NORMAL, nextPullCount, srMissCount);
        probabilities[0] -= appliedSrBonus;
        probabilities[2] += appliedSrBonus;

        if (ticketType == TicketType.RARE || probabilities[0] <= 0d) {
            return renormalize(probabilities, PetGrade.RARE);
        }
        if (ticketType == TicketType.EPIC) {
            return renormalize(probabilities, PetGrade.EPIC);
        }
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

    private double[] renormalize(double[] probabilities, PetGrade minimumGrade) {
        double sum = 0d;
        for (int index = 0; index < GRADES.length; index++) {
            if (GRADES[index].ordinal() >= minimumGrade.ordinal()) {
                sum += probabilities[index];
            } else {
                probabilities[index] = 0d;
            }
        }
        if (sum <= 0d) {
            throw new IllegalStateException("Invalid gacha probability table");
        }
        for (int index = 0; index < probabilities.length; index++) {
            probabilities[index] = probabilities[index] / sum;
        }
        return probabilities;
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
}
