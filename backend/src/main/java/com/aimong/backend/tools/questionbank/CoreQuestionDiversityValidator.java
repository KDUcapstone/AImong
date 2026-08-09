package com.aimong.backend.tools.questionbank;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CoreQuestionDiversityValidator {

    public CoreQuestionDiversityReport validate(List<AuditQuestion> questions) {
        Map<String, List<AuditQuestion>> byMission = new LinkedHashMap<>();
        for (AuditQuestion question : questions) {
            byMission.computeIfAbsent(question.missionCode(), ignored -> new ArrayList<>()).add(question);
        }

        Map<String, List<String>> duplicateGroups = new LinkedHashMap<>();
        int duplicateItemCount = 0;
        int identicalSixPackSlotCount = 0;
        int sameMissionNearDuplicateCount = 0;
        int sameMissionComparableCount = 0;
        Set<String> normalizedCoreQuestions = new LinkedHashSet<>();
        Map<String, Integer> slotLevelRepetitionHotspots = new LinkedHashMap<>();

        for (Map.Entry<String, List<AuditQuestion>> entry : byMission.entrySet()) {
            Map<String, List<String>> byCore = new LinkedHashMap<>();
            Map<Integer, List<AuditQuestion>> bySlot = new LinkedHashMap<>();

            for (AuditQuestion question : entry.getValue()) {
                String core = normalizeCoreQuestion(question.question());
                normalizedCoreQuestions.add(core);
                byCore.computeIfAbsent(core, ignored -> new ArrayList<>()).add(question.externalId());
                Integer slotNo = slotNo(question.externalId());
                if (slotNo != null) {
                    bySlot.computeIfAbsent(slotNo, ignored -> new ArrayList<>()).add(question);
                }
            }

            for (Map.Entry<String, List<String>> coreEntry : byCore.entrySet()) {
                if (coreEntry.getValue().size() > 1) {
                    duplicateGroups.put(entry.getKey() + "|" + coreEntry.getKey(), List.copyOf(coreEntry.getValue()));
                    duplicateItemCount += coreEntry.getValue().size();
                }
            }

            for (List<AuditQuestion> slotQuestions : bySlot.values()) {
                if (slotQuestions.size() < 6) {
                    continue;
                }
                Set<String> slotCores = slotQuestions.stream()
                        .map(question -> normalizeCoreQuestion(question.question()))
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
                if (slotCores.size() == 1) {
                    identicalSixPackSlotCount++;
                }
                if (slotCores.size() <= 3) {
                    AuditQuestion first = slotQuestions.getFirst();
                    slotLevelRepetitionHotspots.put(
                            first.missionCode() + "-slot-" + slotNo(first.externalId()),
                            6 - slotCores.size()
                    );
                }
            }

            List<AuditQuestion> missionQuestions = entry.getValue();
            for (int left = 0; left < missionQuestions.size(); left++) {
                for (int right = left + 1; right < missionQuestions.size(); right++) {
                    sameMissionComparableCount++;
                    double similarity = tokenJaccard(
                            normalizeCoreQuestion(missionQuestions.get(left).question()),
                            normalizeCoreQuestion(missionQuestions.get(right).question())
                    );
                    if (similarity >= 0.82d) {
                        sameMissionNearDuplicateCount++;
                    }
                }
            }
        }

        double nearDuplicateRate = sameMissionComparableCount == 0
                ? 0d
                : sameMissionNearDuplicateCount / (double) sameMissionComparableCount;

        return new CoreQuestionDiversityReport(
                normalizedCoreQuestions.size(),
                duplicateGroups.size(),
                duplicateItemCount,
                identicalSixPackSlotCount,
                nearDuplicateRate,
                duplicateGroups,
                slotLevelRepetitionHotspots
        );
    }

    static String normalizeCoreQuestion(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replace("보기에서", " ")
                .replace("다음 중", " ")
                .replace("문장을 읽고", " ")
                .replace("상황을 보고", " ")
                .replace("빈칸에 알맞은 말을", " ")
                .replace("고르세요", " ")
                .replace("골라 보세요", " ")
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\s가-힣]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Integer slotNo(String externalId) {
        if (externalId == null) {
            return null;
        }
        int lastDash = externalId.lastIndexOf('-');
        if (lastDash < 0 || lastDash + 1 >= externalId.length()) {
            return null;
        }
        return Integer.parseInt(externalId.substring(lastDash + 1));
    }

    private double tokenJaccard(String left, String right) {
        Set<String> leftTokens = tokenSet(left);
        Set<String> rightTokens = tokenSet(right);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0d;
        }
        Set<String> intersection = new LinkedHashSet<>(leftTokens);
        intersection.retainAll(rightTokens);
        Set<String> union = new LinkedHashSet<>(leftTokens);
        union.addAll(rightTokens);
        return union.isEmpty() ? 0d : (double) intersection.size() / union.size();
    }

    private Set<String> tokenSet(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        return new LinkedHashSet<>(List.of(text.split("\\s+")));
    }

    public record CoreQuestionDiversityReport(
            int normalizedCoreQuestionUniqueCount,
            int duplicateCoreQuestionGroupCount,
            int duplicateCoreQuestionItemCount,
            int identicalSixPackSlotCount,
            double sameMissionNearDuplicateRate,
            Map<String, List<String>> duplicateGroups,
            Map<String, Integer> slotLevelRepetitionHotspots
    ) {
    }
}
