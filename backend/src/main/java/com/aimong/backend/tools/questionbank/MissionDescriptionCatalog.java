package com.aimong.backend.tools.questionbank;

import java.util.Map;

final class MissionDescriptionCatalog {

    private static final Map<String, String> DESCRIPTIONS_BY_CODE = Map.ofEntries(
            Map.entry("S0101", "생활 속 AI 도구와 AI의 기본 개념을 배워요"),
            Map.entry("S0102", "규칙 기반 도구와 학습 기반 AI의 차이를 배워요"),
            Map.entry("S0103", "AI가 데이터를 보고 배우는 방식을 배워요"),
            Map.entry("S0104", "딥러닝이 이미지와 소리를 인식하는 방식을 배워요"),
            Map.entry("S0105", "AI 답변도 틀릴 수 있음을 알고 확인 습관을 배워요"),
            Map.entry("S0201", "목적이 드러나는 좋은 질문을 만드는 법을 배워요"),
            Map.entry("S0202", "조건을 구체적으로 담아 AI에게 질문하는 법을 배워요"),
            Map.entry("S0203", "개인정보와 생체정보를 안전하게 지키는 법을 배워요"),
            Map.entry("S0204", "사진, 음성, 데이터를 바르게 모으고 사용하는 법을 배워요"),
            Map.entry("S0205", "AI 도구를 실험하고 결과를 고쳐 보는 법을 배워요"),
            Map.entry("S0206", "AI 도움을 참고해 내 답으로 정리하는 법을 배워요"),
            Map.entry("S0301", "AI 답변을 바로 믿지 않고 사실을 확인하는 법을 배워요"),
            Map.entry("S0302", "출처와 근거를 비교해 정보의 믿을 만함을 판단해요"),
            Map.entry("S0303", "데이터와 AI 결과에 숨어 있는 편향을 찾아봐요"),
            Map.entry("S0304", "AI 기술의 좋은 점과 조심할 점을 함께 생각해요"),
            Map.entry("S0305", "AI 윤리 딜레마에서 공정한 선택을 고민해요")
    );

    private MissionDescriptionCatalog() {
    }

    static String descriptionFor(String missionCode, String missionTitle) {
        String description = DESCRIPTIONS_BY_CODE.get(missionCode);
        if (description != null) {
            return description;
        }
        return missionTitle + " 학습 내용을 배워요";
    }
}
