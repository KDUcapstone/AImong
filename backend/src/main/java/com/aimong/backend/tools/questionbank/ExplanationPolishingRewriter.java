package com.aimong.backend.tools.questionbank;

import java.util.List;
import java.util.Map;

public final class ExplanationPolishingRewriter {

    private static final List<String> STEP1_TEMPLATES = List.of(
            "다음에도 먼저 %s 떠올려 보세요.",
            "비슷한 장면에서도 %s 먼저 살펴보세요.",
            "헷갈릴 때는 %s 다시 생각해 보세요.",
            "같은 활동이 나오면 %s 먼저 적어 보세요.",
            "고를 때도 %s 먼저 따져 보세요.",
            "비교할 때는 %s 먼저 보세요.",
            "판단하기 전에 %s 먼저 확인해 보세요.",
            "답을 찾기 전에 %s 다시 보세요.",
            "핵심은 %s 먼저 챙기는 거예요.",
            "결국 %s 먼저 살피는 거예요."
    );
    private static final List<String> STEP2_TEMPLATES = List.of(
            "다음에도 먼저 %s 확인해 보세요.",
            "비슷한 상황에서도 %s 먼저 살펴보세요.",
            "헷갈릴 때는 %s 다시 떠올려 보세요.",
            "같은 장면이 나오면 %s 먼저 적어 보세요.",
            "고를 때도 %s 먼저 따져 보세요.",
            "비교할 때는 %s 먼저 보세요.",
            "판단하기 전에 %s 먼저 확인해 보세요.",
            "답을 고르기 전에 %s 다시 보세요.",
            "핵심은 %s 먼저 챙기는 거예요.",
            "결국 %s 먼저 살피는 거예요."
    );
    private static final List<String> STEP3_TEMPLATES = List.of(
            "다음에도 먼저 %s 비교해 보세요.",
            "비슷한 상황에서도 %s 먼저 살펴보세요.",
            "헷갈릴 때는 %s 다시 떠올려 보세요.",
            "같은 장면이 나오면 %s 먼저 적어 보세요.",
            "고를 때도 %s 먼저 따져 보세요.",
            "비교할 때는 %s 먼저 보세요.",
            "판단하기 전에 %s 먼저 확인해 보세요.",
            "답을 고르기 전에 %s 다시 보세요.",
            "핵심은 %s 먼저 챙기는 거예요.",
            "결국 %s 먼저 살피는 거예요."
    );
    private static final Map<String, String> FOCUS_BY_MISSION = Map.ofEntries(
            Map.entry("S0101", "인공지능의 특징을"),
            Map.entry("S0102", "규칙과 학습의 차이를"),
            Map.entry("S0103", "레이블과 학습 흐름을"),
            Map.entry("S0104", "딥러닝이 쓰이는 뜻을"),
            Map.entry("S0105", "다시 확인할 기준을"),
            Map.entry("S0201", "질문의 목적을"),
            Map.entry("S0202", "질문에 넣을 조건을"),
            Map.entry("S0203", "개인정보인지 아닌지를"),
            Map.entry("S0204", "데이터가 고른지를"),
            Map.entry("S0205", "결과를 다시 시험하는 까닭을"),
            Map.entry("S0206", "내 말로 바꿔 보는 습관을"),
            Map.entry("S0301", "핵심 주장과 근거를"),
            Map.entry("S0302", "출처와 날짜를"),
            Map.entry("S0303", "치우친 자료의 문제를"),
            Map.entry("S0304", "좋은 점과 걱정되는 점을"),
            Map.entry("S0305", "누가 영향을 받는지를")
    );

    public String rewrite(AuditQuestion question) {
        String firstSentence = firstSentence(question.explanation());
        String focus = FOCUS_BY_MISSION.getOrDefault(question.missionCode(), "핵심 기준을");
        List<String> templates = templates(question.stage());
        int slotIndex = Math.max(0, Math.min(9, slotNo(question.externalId()) - 1));
        String secondSentence = templates.get(slotIndex).formatted(focus);
        if (firstSentence.isBlank()) {
            return secondSentence;
        }
        return firstSentence + " " + secondSentence;
    }

    private List<String> templates(short stage) {
        return switch (stage) {
            case 1 -> STEP1_TEMPLATES;
            case 2 -> STEP2_TEMPLATES;
            case 3 -> STEP3_TEMPLATES;
            default -> STEP2_TEMPLATES;
        };
    }

    private String firstSentence(String explanation) {
        if (explanation == null || explanation.isBlank()) {
            return "";
        }
        int dot = explanation.indexOf('.');
        if (dot >= 0) {
            return explanation.substring(0, dot + 1).trim();
        }
        return explanation.trim();
    }

    private int slotNo(String externalId) {
        if (externalId == null) {
            return 1;
        }
        int lastDash = externalId.lastIndexOf('-');
        if (lastDash < 0 || lastDash + 1 >= externalId.length()) {
            return 1;
        }
        return Integer.parseInt(externalId.substring(lastDash + 1));
    }
}
