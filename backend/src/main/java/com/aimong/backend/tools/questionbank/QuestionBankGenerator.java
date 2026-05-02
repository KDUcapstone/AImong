package com.aimong.backend.tools.questionbank;

import java.util.ArrayList;
import java.util.List;

public final class QuestionBankGenerator {

    private static final List<String> TAG_SEQUENCE = List.of(
            "SAFETY", "PRIVACY", "VERIFICATION", "PROMPT", "FACT",
            "SAFETY", "VERIFICATION", "PROMPT", "FACT", "PRIVACY"
    );

    public QuestionBankDraft generate(CurriculumManifest manifest) {
        List<QuestionDraft> questions = new ArrayList<>();
        for (CurriculumUnit unit : manifest.units()) {
            for (CurriculumLesson lesson : unit.lessons()) {
                questions.addAll(generateForLesson(unit, lesson));
            }
        }
        return new QuestionBankDraft(
                manifest.sourceTitle(),
                manifest.sourceReference(),
                questions.size(),
                questions
        );
    }

    private List<QuestionDraft> generateForLesson(CurriculumUnit unit, CurriculumLesson lesson) {
        List<QuestionDraft> questions = new ArrayList<>();
        List<String> keywords = lesson.keywords();
        String keywordA = keywords.get(0);
        String keywordB = keywords.size() > 1 ? keywords.get(1) : lesson.keyConcept();
        String keywordC = keywords.size() > 2 ? keywords.get(2) : lesson.promptTip();

        questions.add(build(
                lesson, unit, 1, "OX",
                "AI를 사용할 때 '" + lesson.safetyRule() + "' 규칙은 지켜야 한다.",
                null,
                Boolean.TRUE,
                lesson.safetyRule() + "는 " + lesson.studentObjective() + "와 연결되는 기본 약속이다.",
                tags(0, 2)
        ));
        questions.add(build(
                lesson, unit, 2, "OX",
                lesson.keyConcept() + "를 배울 때 결과를 확인하는 습관은 필요 없다.",
                null,
                Boolean.FALSE,
                lesson.verificationHabit() + "처럼 결과를 다시 확인해야 실수를 줄일 수 있다.",
                tags(1, 2)
        ));
        questions.add(build(
                lesson, unit, 3, "MULTIPLE",
                "'" + lesson.missionTitle() + "' 활동에서 가장 알맞은 행동은 무엇일까요?",
                List.of(
                        lesson.verificationHabit(),
                        "AI가 준 답을 그대로 제출한다.",
                        "친구의 개인정보를 넣어 본다.",
                        "질문과 상관없는 말을 계속 입력한다."
                ),
                Integer.valueOf(0),
                "이 차시는 " + lesson.studentObjective() + "를 연습하는 활동이라서 올바른 사용 습관이 중요하다.",
                tags(2, 2)
        ));
        questions.add(build(
                lesson, unit, 4, "MULTIPLE",
                lesson.keyConcept() + "를 더 잘 배우기 위한 질문 방법으로 가장 좋은 것은 무엇일까요?",
                List.of(
                        lesson.promptTip(),
                        "그냥 알아서 해 줘.",
                        "아무 말이나 길게 써 줘.",
                        "누구의 정보인지 적지 않고 비밀 번호를 넣어 줘."
                ),
                Integer.valueOf(0),
                "좋은 질문은 목적과 조건이 분명해야 원하는 결과를 얻기 쉽다.",
                tags(3, 2)
        ));
        questions.add(build(
                lesson, unit, 5, "MULTIPLE",
                "'" + keywordA + "'와 관련해 확인해야 할 것으로 가장 알맞은 것은 무엇일까요?",
                List.of(
                        "출처, 날짜, 이름을 다시 본다.",
                        "첫 답이 길면 무조건 믿는다.",
                        "친한 친구 한 명 말만 듣는다.",
                        "확인하지 않고 바로 공유한다."
                ),
                Integer.valueOf(0),
                lesson.verificationHabit() + "처럼 근거를 다시 보는 습관이 필요하다.",
                tags(4, 2)
        ));
        questions.add(build(
                lesson, unit, 6, "FILL",
                "'" + lesson.missionTitle() + "' 시간에는 결과를 바로 믿지 말고 ____ 하는 습관이 필요합니다.",
                List.of("확인", "복사", "공유", "추측"),
                List.of(0),
                lesson.verificationHabit() + "은 결과를 확인하는 습관으로 이어진다.",
                tags(5, 2)
        ));
        questions.add(build(
                lesson, unit, 7, "FILL",
                keywordB + "를 배울 때 좋은 질문을 만들려면 ____ 하게 요청해야 합니다.",
                List.of("구체", "대충", "몰래", "무작정"),
                List.of(0),
                lesson.promptTip() + "처럼 구체적으로 요청할수록 답이 분명해진다.",
                tags(6, 2)
        ));
        questions.add(build(
                lesson, unit, 8, "SITUATION",
                "수업에서 " + keywordA + "를 조사하는데 AI가 자신 있게 말했습니다. 어떻게 하는 것이 가장 좋을까요?",
                List.of(
                        "출처를 찾아 보고 다른 자료와 비교한다.",
                        "자신감 있게 말했으니 바로 발표한다.",
                        "친구 이름과 연락처를 넣어 다시 물어본다."
                ),
                Integer.valueOf(0),
                lesson.verificationHabit() + "을 실천해야 정확한 정보를 고를 수 있다.",
                tags(7, 2)
        ));
        questions.add(build(
                lesson, unit, 9, "SITUATION",
                "친구가 '" + lesson.missionTitle() + "' 과제를 하면서 AI에 '" + keywordC + "' 대신 개인정보를 넣으려고 합니다. 어떻게 말해 주어야 할까요?",
                List.of(
                        lesson.safetyRule() + " 규칙을 지키고 개인정보는 넣지 말자고 말한다.",
                        "한 번쯤은 괜찮다고 말한다.",
                        "더 자세한 개인정보를 넣어 보자고 말한다."
                ),
                Integer.valueOf(0),
                lesson.safetyRule() + "는 AI 사용에서 가장 먼저 지켜야 하는 안전 규칙이다.",
                tags(8, 2)
        ));
        questions.add(build(
                lesson, unit, 10, "SITUATION",
                "AI가 " + lesson.keyConcept() + "에 대해 두 가지 다른 답을 주었습니다. 가장 알맞은 행동은 무엇일까요?",
                List.of(
                        lesson.verificationHabit(),
                        "마음에 드는 답 하나만 고른다.",
                        "둘 다 사실이라고 믿는다."
                ),
                Integer.valueOf(0),
                "답이 다르면 비교하고 다시 확인해야 한다.",
                tags(9, 2)
        ));
        return questions;
    }

    private QuestionDraft build(
            CurriculumLesson lesson,
            CurriculumUnit unit,
            int sequence,
            String type,
            String question,
            List<String> options,
            Object answer,
            String explanation,
            List<String> tags
    ) {
        return new QuestionDraft(
                lesson.lessonCode() + "-" + String.format("%02d", sequence),
                lesson.lessonCode(),
                unit.stage(),
                lesson.missionTitle(),
                type,
                question,
                options,
                answer,
                explanation,
                tags,
                "STATIC",
                lesson.lessonCode()
        );
    }

    private List<String> tags(int index, int count) {
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tags.add(TAG_SEQUENCE.get((index + i) % TAG_SEQUENCE.size()));
        }
        return tags;
    }
}
