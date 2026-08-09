package com.aimong.backend.tools.questionbank;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class QuestionBank1056Rebuilder {

    private static final String TOP_SOURCE_REFERENCE = String.join(" / ",
            "question/2021년 인공지능(AI)기본 역량 강화 연수 교재(초등).pdf",
            "question/[GM 2024-05] 생성형 AI를 활용한 교수학습 운영 가이드_f.pdf",
            "question/[KR 2026-01] 2025년 학생 디지털 리터러시 수준측정 연구_FF.pdf",
            "question/[별책본] 디지털 리터러시 구성 체계 및 교과별 성취기준 연계.pdf",
            "question/초등 교사를 위한 KERIS와 시작하는 인공지능 교육 1.pdf",
            "question/초등 교사를 위한 KERIS와 시작하는 인공지능 교육 2.pdf",
            "question/AI 리터러시 문제 구성 개편안 36abca9939c881c1ae32eb562d626376.md"
    );
    private static final Map<String, Integer> PROMPT_OCCURRENCES = new LinkedHashMap<>();
    private static final Map<String, Integer> GLOBAL_PROMPT_OCCURRENCES = new LinkedHashMap<>();
    private static final List<String> SIMILARITY_STOP_WORDS = List.of(
            "ai", "인공지능", "문제", "보기", "다음", "가장", "고르세요", "골라",
            "보세요", "좋아요", "때는", "할까요", "무엇일까요", "무엇을", "어떤",
            "같은", "에서", "으로", "에게", "자료", "정보", "기능", "사용",
            "설명", "기준", "먼저", "살펴", "확인", "중요해요", "중요합니다",
            "있어요", "해야", "해요", "일을", "상황", "문장", "속에서", "생활",
            "관점", "활동", "문항이에요"
    );
    private static final String[] PROMPT_REPAIR_GOOD_ACTIONS = {
            "무엇을 원하는지 더 분명히 말해요.",
            "누가 읽을 글인지 알려 줘요.",
            "필요한 조건을 한두 가지 더해요.",
            "표나 목록처럼 원하는 형식을 말해요.",
            "답의 길이와 말투를 다시 알려 줘요.",
            "예시를 하나 넣어 다시 질문해요.",
            "주제에서 벗어난 부분은 빼 달라고 해요.",
            "어려운 말은 쉬운 말로 바꿔 달라고 해요."
    };
    private static final String[] PROMPT_REPAIR_WRONG_ACTIONS = {
            "아무 조건 없이 더 길게만 써 달라고 해요.",
            "개인정보를 더 넣으면 좋아질 거라고 생각해요.",
            "AI 답을 읽지 않고 그대로 제출해요.",
            "질문을 고치지 않고 같은 답만 기다려요.",
            "원하는 대상을 말하지 않고 다시 물어요.",
            "주제와 상관없는 조건을 많이 섞어요.",
            "이유를 확인하지 않고 말투만 바꿔요.",
            "친구 글을 허락 없이 넣어 고쳐 달라고 해요."
    };
    private static final String[] REASON_CHECK_GOOD_ACTIONS = {
            "왜 그렇게 답했는지 물어봐요.",
            "어떤 자료를 보고 답했는지 물어봐요.",
            "근거와 출처를 알려 달라고 해요.",
            "답의 이유를 설명해 달라고 해요.",
            "믿을 만한 자료와 비교해 봐요.",
            "근거가 부족하면 다시 확인해요."
    };
    private static final String[] REASON_CHECK_WRONG_ACTIONS = {
            "이유를 보지 않고 바로 제출해요.",
            "답이 길면 근거가 있다고 믿어요.",
            "출처를 묻지 않고 표현만 고쳐요.",
            "내가 마음에 드는 답만 골라요.",
            "자료를 확인하지 않고 말투만 바꿔요.",
            "근거가 없어도 자신 있게 발표해요."
    };
    private static final String[] FAIRNESS_GOOD_ACTIONS = {
            "불리한 사람이 없는지 먼저 살펴봐요.",
            "모두에게 억울하지 않은지 확인해요.",
            "여러 의견을 모아 함께 비교해요.",
            "AI 결정 이유를 설명할 수 있는지 확인해요.",
            "빠진 사람이나 상황이 없는지 살펴요.",
            "사람이 다시 확인할 기회를 두어요."
    };
    private static final String[] PRIVACY_GOOD_ACTIONS = {
            "이름, 얼굴, 위치처럼 나를 알 수 있는 정보는 빼요.",
            "친구 사진이나 목소리는 허락 없이 넣지 않아요.",
            "필요한 정보만 남기고 개인정보는 지워요.",
            "개인정보가 보이면 선생님이나 보호자에게 물어봐요.",
            "주소와 전화번호는 AI 질문에 넣지 않아요.",
            "나와 친구를 알아볼 수 있는 자료는 먼저 가려요."
    };
    private QuestionBank1056Rebuilder() {
    }

    public static void main(String[] args) throws Exception {
        boolean ultraDiverse = Arrays.asList(args).contains("--ultra-diverse");
        List<String> positionalArgs = Arrays.stream(args)
                .filter(arg -> !"--ultra-diverse".equals(arg))
                .toList();
        Path output = positionalArgs.size() > 0
                ? Path.of(positionalArgs.get(0))
                : Path.of(ultraDiverse
                ? "_generated/question-bank/question-bank-1056-starlevel-ultra-diverse.json"
                : "_generated/question-bank/question-bank-1056-starlevel-edits.json");
        Files.createDirectories(output.getParent());
        Path sqlOutput = positionalArgs.size() > 1
                ? Path.of(positionalArgs.get(1))
                : Path.of(ultraDiverse
                ? "_generated/question-bank/question-bank-1056-starlevel-ultra-diverse-seed.sql"
                : "_generated/question-bank/question-bank-1056-starlevel-seed.sql");
        Files.createDirectories(sqlOutput.getParent());

        List<Mission> missions = missions();
        List<Question> questions = generateQuestions(missions);
        if (ultraDiverse) {
            questions = ultraDiversifyQuestions(questions);
        }
        validate(missions, questions);

        Map<String, Object> bank = bankDocument(missions, questions, ultraDiverse);
        Files.writeString(output, Json.write(bank), StandardCharsets.UTF_8);
        Files.writeString(sqlOutput, Sql.write(missions, questions), StandardCharsets.UTF_8);
        System.out.println("Wrote " + questions.size() + " questions to " + output);
        System.out.println("Wrote SQL seed to " + sqlOutput);
    }

    private static Map<String, Object> bankDocument(List<Mission> missions, List<Question> questions, boolean ultraDiverse) {
        Map<String, Object> root = object();
        root.put("sourceTitle", ultraDiverse
                ? "AImong 초등 AI 리터러시 문제은행 1056 ultra-diverse 별도본"
                : "AImong 초등 AI 리터러시 문제은행 1056 재생성본");
        root.put("sourceReference", TOP_SOURCE_REFERENCE);
        root.put("generationVersion", ultraDiverse
                ? "AImong-AI-literacy-source-synthesis-1056-ultra-diverse-2026-05-26"
                : "AImong-AI-literacy-source-synthesis-1056-2026-05-25");
        root.put("normalizationNote", ultraDiverse
                ? "question 경로의 6개 PDF와 AI 리터러시 문제 구성 개편안의 방향을 반영한 새 문제은행을 기준으로 문장 품질만 보정했습니다. 유사도 수치를 맞추기 위한 장면 접두사는 붙이지 않았고, 개인정보, 검증, 출처, 저작권, 편향, 공정성, 책임 있는 활용 중심의 문항 구조를 유지했습니다."
                : "question 경로의 6개 PDF와 AI 리터러시 문제 구성 개편안의 방향을 반영해 새로 구성했습니다. 문항 내용은 개인정보, 검증, 출처, 저작권, 편향, 공정성, 책임 있는 활용 중심으로 재작성했습니다.");
        root.put("totalMissionCount", missions.size());
        root.put("totalQuestionCount", questions.size());
        root.put("stageSummary", List.of(
                object("stage", 1, "title", "AI 알아보기", "missionCount", 5, "questionCount", 330),
                object("stage", 2, "title", "AI 안전하게 쓰기", "missionCount", 6, "questionCount", 396),
                object("stage", 3, "title", "AI 판단하기", "missionCount", 5, "questionCount", 330)
        ));
        root.put("missions", missions.stream().map(Mission::toJson).toList());
        root.put("questions", questions.stream().map(Question::toJson).toList());
        root.put("expandedSeedQuestionCount", 1056);
        root.put("questionsPerMission", 66);
        root.put("packsPerMission", 6);
        root.put("difficultyQuotaPerMission", object("LOW", 30, "MEDIUM", 20, "HIGH", 16));
        root.put("packTypeQuota", object(
                "note", "P1-P3 are LOW concept packs, P4-P5 are MEDIUM application packs, and P6 is the HIGH capacity pack.",
                "legacyGenerationReference", object("OX", 2, "MULTIPLE", 3, "FILL", 2, "SITUATION", 3)
        ));
        root.put("qualityRefinement", object(
                "sourceBasis", "question 경로의 6개 PDF와 AI 리터러시 문제 구성 개편안",
                "changes", List.of(
                        "Rebuilt all questions from the AI literacy reform brief and the six question-source PDFs.",
                        "Reduced theory memorization and increased privacy, verification, copyright, bias, fairness, and responsible-use items.",
                        "Kept the 16 mission codes, 6 packs per mission, LOW/MEDIUM/HIGH quota, and question type shapes.",
                        "Balanced MULTIPLE and SITUATION answer positions across the 1056-question bank.",
                        "Used elementary grade 5-6 wording with short explanations."
                )
        ));
        root.put("starLevelSetRatio", object(
                "star1", object("LOW", 7, "MEDIUM", 2, "HIGH", 1),
                "star2", object("LOW", 3, "MEDIUM", 5, "HIGH", 2),
                "star3", object("LOW", 2, "MEDIUM", 3, "HIGH", 5),
                "note", "Each mission contains LOW 30, MEDIUM 20, HIGH 16 so runtime star-level ratios have enough capacity."
        ));
        root.put("highExpansion", object(
                "previousQuestionCount", 960,
                "addedHighQuestionCount", 96,
                "addedPerMission", 6,
                "externalIdRangePerMission", "P6-11..P6-16",
                "outputFile", ultraDiverse
                        ? "question-bank-1056-starlevel-ultra-diverse.json"
                        : "question-bank-1056-starlevel-edits.json",
                "actualPackTypeDistributionNote", "P6 contains 16 HIGH questions per mission."
        ));
        root.put("actualPackTypeDistribution", object(
                "P1", object("OX", 4, "MULTIPLE", 4, "FILL", 2, "SITUATION", 0),
                "P2", object("OX", 4, "MULTIPLE", 4, "FILL", 2, "SITUATION", 0),
                "P3", object("OX", 4, "MULTIPLE", 4, "FILL", 2, "SITUATION", 0),
                "P4", object("OX", 0, "MULTIPLE", 4, "FILL", 3, "SITUATION", 3),
                "P5", object("OX", 0, "MULTIPLE", 2, "FILL", 3, "SITUATION", 5),
                "P6", object("OX", 1, "MULTIPLE", 2, "FILL", 1, "SITUATION", 12)
        ));
        return root;
    }

    private static List<Question> generateQuestions(List<Mission> missions) {
        PROMPT_OCCURRENCES.clear();
        GLOBAL_PROMPT_OCCURRENCES.clear();
        List<Question> questions = new ArrayList<>();
        AnswerRotator multipleAnswers = new AnswerRotator(4);
        AnswerRotator situationAnswers = new AnswerRotator(4);
        AnswerRotator fillAnswers = new AnswerRotator(4);

        for (Mission mission : missions) {
            for (int pack = 1; pack <= 6; pack++) {
                int slot = 1;
                for (String type : packTypes(pack)) {
                    String difficulty = difficulty(pack);
                    int missionQuestionNo = questions.stream()
                            .filter(question -> question.missionCode.equals(mission.code))
                            .toList()
                            .size() + 1;
                    Question question = switch (type) {
                        case "OX" -> oxQuestion(mission, pack, slot, missionQuestionNo, difficulty);
                        case "MULTIPLE" -> multipleQuestion(mission, pack, slot, missionQuestionNo, difficulty, multipleAnswers);
                        case "FILL" -> fillQuestion(mission, pack, slot, missionQuestionNo, difficulty, fillAnswers);
                        case "SITUATION" -> situationQuestion(mission, pack, slot, missionQuestionNo, difficulty, situationAnswers);
                        default -> throw new IllegalStateException(type);
                    };
                    questions.add(question);
                    slot++;
                }
            }
        }
        return questions;
    }

    private static List<Question> ultraDiversifyQuestions(List<Question> questions) {
        List<Question> diversified = new ArrayList<>();
        for (int index = 0; index < questions.size(); index++) {
            Question question = questions.get(index);
            Question diversifiedQuestion = copyWithText(
                    question,
                    ultraDiversifyQuestion(question.question, index, question.externalId),
                    ultraPolishExplanation(question.explanation, index)
            );
            diversified.add(applyUltraFinalReviewPatch(diversifiedQuestion));
        }
        return List.copyOf(diversified);
    }

    private static Question applyUltraFinalReviewPatch(Question question) {
        return switch (question.externalId) {
            case "S0105-P3-07" -> copyWithOptionsAndExplanation(
                    question,
                    replaceOption(question.options, 2, "근거가 있는지 확인하고, 부족하면 다시 물어봐요."),
                    "맞아요. 근거가 있는지 확인하고 부족하면 다시 물어봐야 AI 답을 더 안전하게 쓸 수 있어요."
            );
            case "S0103-P2-03", "S0103-P6-01" -> copyWithOptionsAndExplanation(
                    question,
                    question.options,
                    "아니에요. 자료가 많아도 친구 사진이나 만든 자료는 허락을 확인한 뒤 사용해야 해요."
            );
            case "S0103-P4-03" -> copyWithOptionsAndExplanation(
                    question,
                    replaceOption(question.options, 2, "사진 이름표가 강아지와 고양이에 맞는지 확인해요."),
                    "맞아요. 이름표가 실제 사진과 맞아야 AI가 자료를 잘못 배우지 않아요."
            );
            case "S0103-P5-02" -> copyWithOptionsAndExplanation(
                    question,
                    replaceOption(question.options, 1, "잘못 붙은 이름표가 없는지 확인해요."),
                    "맞아요. 잘못된 이름표가 있으면 AI가 틀리게 배울 수 있으므로 먼저 확인해야 해요."
            );
            case "S0202-P4-02" -> copyWithOptionsAndExplanation(
                    question,
                    replaceOption(question.options, 1, "근거와 이유를 더 알려 달라고 해요."),
                    "맞아요. 근거가 빠진 답은 출처와 이유를 더 물어보고 다시 확인해야 해요."
            );
            case "S0203-P6-13" -> copyWithOptionsAndExplanation(
                    question,
                    replaceOption(question.options, 0, "이름과 반은 가리고 주의할 점을 발표해요."),
                    "맞아요. 이름과 반처럼 나를 알 수 있는 정보는 필요한 만큼만 쓰고, 허락 없이 공개하지 않아요."
            );
            case "S0204-P1-02" -> copyWithOptionsAndExplanation(
                    question,
                    question.options,
                    "아니에요. 이름표가 틀리면 AI도 잘못 배울 수 있으므로 사람이 다시 확인해야 해요."
            );
            case "S0204-P1-04" -> copyWithOptionsAndExplanation(
                    question,
                    question.options,
                    "아니에요. 흐린 자료는 AI 결과에 영향을 줄 수 있으므로 더 선명한 자료로 확인해야 해요."
            );
            case "S0204-P3-02" -> copyWithOptionsAndExplanation(
                    question,
                    question.options,
                    "아니에요. 좋은 데이터와 나쁜 데이터는 구분해서 AI가 잘못 배우지 않게 해야 해요."
            );
            case "S0204-P3-07" -> copyWithOptionsAndExplanation(
                    question,
                    replaceOption(question.options, 2, "친구 사진이나 자료는 허락을 확인하고 사용해요."),
                    "맞아요. 친구 사진이나 자료는 먼저 허락을 확인하고, 허락이 없으면 쓰지 않아야 해요."
            );
            case "S0204-P2-06" -> copyWithOptionsAndExplanation(
                    question,
                    replaceOption(question.options, 1, "그림 이름표가 실제 내용과 맞는지 확인해요."),
                    "맞아요. 이름표가 실제 그림과 맞아야 AI가 자료를 올바르게 배울 수 있어요."
            );
            case "S0204-P4-04" -> copyWithOptionsAndExplanation(
                    question,
                    replaceOption(question.options, 3, "친구 목소리나 사진은 허락을 받은 뒤 사용해요."),
                    "맞아요. 친구 목소리나 사진은 개인정보가 될 수 있으므로 먼저 허락을 받아야 해요."
            );
            case "S0204-P5-02" -> copyWithOptionsAndExplanation(
                    question,
                    replaceOption(question.options, 1, "그림의 출처와 사용 허락을 함께 확인해요."),
                    "맞아요. 그림이나 사진은 출처와 사용 허락, 만든 사람의 권리를 함께 확인해야 해요."
            );
            case "S0204-P5-10" -> copyWithOptionsAndExplanation(
                    question,
                    replaceOption(question.options, 3, "인터넷 그림의 출처와 사용 허락을 함께 확인해요."),
                    "맞아요. 인터넷 그림은 누가 만들었는지와 사용해도 되는지 함께 확인해야 해요."
            );
            case "S0301-P2-06" -> copyWithOptionsAndExplanation(
                    question,
                    replaceOption(question.options, 1, "공식 기관 자료인지 날짜와 출처를 확인해요."),
                    "맞아요. 공식 기관 자료인지, 날짜와 출처가 있는지 확인해야 해요."
            );
            case "S0302-P2-02", "S0302-P2-04" -> copyWithOptionsAndExplanation(
                    question,
                    question.options,
                    "아니에요. 출처가 없으면 믿기 어려우므로 작성자, 기관, 날짜를 확인해야 해요."
            );
            case "S0302-P2-03", "S0302-P6-01" -> copyWithOptionsAndExplanation(
                    question,
                    question.options,
                    "아니에요. 댓글 하나만으로는 부족하므로 공식 기관 자료와 날짜를 확인해야 해요."
            );
            default -> question;
        };
    }

    private static List<String> replaceOption(List<String> options, int index, String replacement) {
        List<String> updated = new ArrayList<>(options);
        updated.set(index, replacement);
        return List.copyOf(updated);
    }

    private static Question copyWithText(Question question, String newQuestion, String newExplanation) {
        return new Question(
                question.externalId,
                question.missionCode,
                question.stage,
                question.stageTitle,
                question.missionTitle,
                question.type,
                newQuestion,
                question.options,
                question.answer,
                newExplanation,
                question.contentTags,
                question.curriculumRef,
                question.sourceType,
                question.generationPhase,
                question.sourceReference,
                question.difficulty,
                question.packNo
        );
    }

    private static Question copyWithOptionsAndExplanation(
            Question question,
            List<String> newOptions,
            String newExplanation
    ) {
        return new Question(
                question.externalId,
                question.missionCode,
                question.stage,
                question.stageTitle,
                question.missionTitle,
                question.type,
                question.question,
                newOptions,
                question.answer,
                newExplanation,
                question.contentTags,
                question.curriculumRef,
                question.sourceType,
                question.generationPhase,
                question.sourceReference,
                question.difficulty,
                question.packNo
        );
    }

    private static String ultraDiversifyQuestion(String question, int index, String externalId) {
        String cleaned = compactUltraQuestion(polishText(question), externalId);
        if (cleaned.length() <= 90) {
            return cleaned;
        }
        String shorter = stripQuestionIntro(cleaned);
        return shorter.length() <= 90 ? shorter : cleaned;
    }

    private static String compactUltraQuestion(String question, String externalId) {
        String fixed = question
                .replace(" 예를 떠올려요. ", " 사례에서 ")
                .replace(" 기준으로도 함께 생각해 보세요.", "도 함께 생각해요.")
                .replace("발표 자료로 만들려고 해요.", "자료로 만들어요.")
                .replace("사용하기 전에 먼저 살펴볼 점", "사용 전 먼저 살펴볼 점")
                .replace("우리 주변의 AI 찾기 활동에서는", "생활 속 AI 활동에서")
                .replace("AI가 배우는 예시 자료를 ____라고 불러요.", "AI가 배우는 예시 묶음은 ____라고 불러요.")
                .replace("학급 게시판에서 AI가 배우는 예시 자료를 ____라고 불러요.", "학급 게시판의 예시 자료 묶음은 AI 학습용 ____예요.")
                .replace("믿을 만한 기관 자료처럼 믿을 만한 ____가 팩트체크에 좋아요.", "팩트체크에는 만든 곳이 분명한 ____가 좋아요.")
                .replace("통계 수치 사례에서 믿을 만한 기관 자료처럼 믿을 만한 ____가 팩트체크에 좋아요.", "통계 수치를 확인할 때는 출처가 분명한 ____를 찾아요.")
                .replace("얼굴 인식처럼 사람을 알아보는 기술은 ____ 보호를 생각해야 해요.", "얼굴 인식처럼 사람을 알아보는 기술은 ____가 드러나지 않게 써요.");
        return switch (externalId) {
            case "S0103-P3-03" -> fixed.replace(
                    "AI for Oceans 활동처럼 분류 예시를 보며 AI 원리를 체험할 수 있어요.",
                    "AI for Oceans 활동에서 분류 예시로 AI가 배우는 과정을 볼 수 있어요."
            );
            case "S0103-P5-05" -> fixed.replace(
                    "AI가 배우는 예시 묶음은 ____라고 불러요.",
                    "AI 학습에 쓰는 예시 모음은 ____예요."
            );
            case "S0104-P4-06" -> fixed.replace(
                    "AI는 글과 사진의 특징을 보려면 다양한 ____를 살펴야 해요.",
                    "글과 사진의 특징을 배우려면 AI는 여러 ____를 비교해요."
            );
            case "S0304-P2-09" -> fixed.replace(
                    "자동 채점 사례에서 얼굴 인식처럼 사람을 알아보는 기술은 ____가 드러나지 않게 써요.",
                    "자동 채점 사례에서는 사람을 알아볼 수 있는 ____ 노출을 조심해요."
            );
            case "S0304-P2-10" -> fixed.replace(
                    "얼굴 인식처럼 사람을 알아보는 기술은 ____가 드러나지 않게 써요.",
                    "얼굴 인식 기술을 쓸 때는 개인을 알아볼 ____ 보호가 필요해요."
            );
            default -> fixed;
        };
    }

    private static String stripQuestionIntro(String question) {
        if (question.startsWith("다음 중 ")) {
            return "보기에서 " + question.substring("다음 중 ".length());
        }
        return question;
    }

    private static String ultraPolishExplanation(String explanation, int index) {
        String fixed = polishText(explanation);
        if (fixed.contains("AI 결과는 바로 쓰기 전에 다시 확인해요.")) {
            fixed = fixed.replace("AI 결과는 바로 쓰기 전에 다시 확인해요.", ultraExplanationTail(index));
        }
        return fixed;
    }

    private static String ultraExplanationTail(int index) {
        return switch (Math.floorMod(index, 6)) {
            case 0 -> "출처와 목적에 맞는지도 함께 봐요.";
            case 1 -> "개인정보가 없는지도 살펴봐요.";
            case 2 -> "왜 그런 답이 나왔는지 확인해요.";
            case 3 -> "내가 쓸 내용과 맞는지 다시 읽어요.";
            case 4 -> "친구나 선생님과 한 번 더 비교해요.";
            default -> "필요한 부분만 골라 내 말로 고쳐요.";
        };
    }

    private static List<String> packTypes(int pack) {
        return switch (pack) {
            case 1, 2, 3 -> List.of("OX", "OX", "OX", "OX", "MULTIPLE", "MULTIPLE", "MULTIPLE", "MULTIPLE", "FILL", "FILL");
            case 4 -> List.of("MULTIPLE", "MULTIPLE", "MULTIPLE", "MULTIPLE", "FILL", "FILL", "FILL", "SITUATION", "SITUATION", "SITUATION");
            case 5 -> List.of("MULTIPLE", "MULTIPLE", "FILL", "FILL", "FILL", "SITUATION", "SITUATION", "SITUATION", "SITUATION", "SITUATION");
            case 6 -> List.of("OX", "MULTIPLE", "MULTIPLE", "FILL", "SITUATION", "SITUATION", "SITUATION", "SITUATION", "SITUATION", "SITUATION", "SITUATION", "SITUATION", "SITUATION", "SITUATION", "SITUATION", "SITUATION");
            default -> throw new IllegalArgumentException("pack: " + pack);
        };
    }

    private static String difficulty(int pack) {
        if (pack <= 3) {
            return "LOW";
        }
        if (pack <= 5) {
            return "MEDIUM";
        }
        return "HIGH";
    }

    private static Question oxQuestion(Mission mission, int pack, int slot, int no, String difficulty) {
        boolean answer = ((pack + slot + mission.stage + no) % 2 == 0);
        String claim = answer
                ? pick(mission.trueClaims, pack, slot, no)
                : pick(mission.falseClaims, pack, slot, no);
        String context = pick(mission.contexts, pack, slot, no);
        String example = pick(mission.examples, pack, slot, no);
        String prefix = switch ((pack + slot) % 4) {
            case 0 -> contextAt(context) + " ";
            case 1 -> mission.missionTitle + " 활동에서 ";
            case 2 -> objectPhrase(example) + " 살펴볼 때 ";
            default -> "";
        };
        String question = prefix + claim;
        if (pack == 6) {
            question = contextAt(context) + " " + objectPhrase(example) + " 살펴볼 때, " + claim;
        }
        return question(mission, pack, slot, no, "OX", question, null, answer,
                oxExplanation(mission, pack, slot, no, answer, context, claim),
                tags(mission, "FACT", no), difficulty);
    }

    private static Question multipleQuestion(
            Mission mission,
            int pack,
            int slot,
            int no,
            String difficulty,
            AnswerRotator answers
    ) {
        String context = pick(mission.contexts, pack, slot, no);
        String example = pick(mission.examples, pack + 1, slot, no);
        String concept = conceptFor(mission, pack, slot + 2, no);
        int questionPattern = Math.floorMod(no + pack + slot, 8);
        String question = switch (questionPattern) {
            case 0 -> contextAt(context) + " " + objectPhrase(example) + " 사용할 때 " + objectPhrase(concept) + " 살펴보는 행동은 무엇일까요?";
            case 1 -> "다음 중 " + contextAt(context) + " " + objectPhrase(mission.missionTitle) + " 실천한 모습은 무엇일까요?";
            case 2 -> contextAt(context) + " " + objectPhrase(concept) + " 살펴보려고 해요. 먼저 볼 것은 무엇일까요?";
            case 3 -> "친구가 " + example + " 때문에 헷갈려 해요. " + objectPhrase(mission.missionTitle) + " 배울 때 좋은 조언은 무엇일까요?";
            case 4 -> pack >= 6
                    ? reasonCheckQuestion(context, example)
                    : promptRepairQuestion(context, example);
            case 5 -> "다음 중 " + objectPhrase(concept) + " 안전하게 다루는 모습은 무엇일까요?";
            case 6 -> pack >= 4
                    ? contextAt(context) + " " + objectPhrase(example) + " 발표 자료로 만들려고 해요. "
                    + objectPhrase(concept) + " 살펴보는 태도는 무엇일까요?"
                    : "모둠 활동에서 " + objectPhrase(example) + " 정리하려고 해요. "
                    + mission.missionTitle + "에 맞는 태도는 무엇일까요?";
            default -> contextAt(context) + " " + objectPhrase(example) + " 보며 " + mission.missionTitle + "을 배운 뒤 피해야 할 행동은 무엇일까요?";
        };

        boolean avoidQuestion = question.endsWith("피해야 할 행동은 무엇일까요?");
        boolean promptRepairQuestion = questionPattern == 4 && pack < 6;
        boolean reasonCheckQuestion = questionPattern == 4 && pack >= 6;
        boolean fairnessQuestion = "S0305".equals(mission.code) && concept.contains("공정");
        boolean privacyQuestion = concept.contains("개인정보");
        String correct = avoidQuestion
                ? pick(mission.wrongActions, pack, slot, no)
                : promptRepairQuestion
                ? pick(PROMPT_REPAIR_GOOD_ACTIONS, pack, slot, no + mission.code.hashCode())
                : reasonCheckQuestion
                ? pick(REASON_CHECK_GOOD_ACTIONS, pack, slot, no + mission.code.hashCode())
                : fairnessQuestion
                ? pick(FAIRNESS_GOOD_ACTIONS, pack, slot, no + mission.code.hashCode())
                : privacyQuestion
                ? pick(PRIVACY_GOOD_ACTIONS, pack, slot, no + mission.code.hashCode())
                : pick(mission.goodActions, pack, slot, no);
        List<String> distractors = avoidQuestion
                ? distinctPicks(mission.goodActions, correct, pack + 1, slot, no, 3)
                : promptRepairQuestion
                ? distinctPicks(PROMPT_REPAIR_WRONG_ACTIONS, correct, pack + 1, slot, no + mission.code.hashCode(), 3)
                : reasonCheckQuestion
                ? distinctPicks(REASON_CHECK_WRONG_ACTIONS, correct, pack + 1, slot, no + mission.code.hashCode(), 3)
                : distinctPicks(mission.wrongActions, correct, pack + 1, slot, no, 3);
        int answerIndex = answers.next();
        List<String> options = varyChoiceOptions(place(correct, distractors, answerIndex), answerIndex, context, example, no);
        return question(mission, pack, slot, no, "MULTIPLE", question, options, answerIndex,
                multipleExplanation(mission, context, example, concept, correct, avoidQuestion, promptRepairQuestion, reasonCheckQuestion, no),
                tags(mission, "VERIFICATION", no), difficulty);
    }

    private static Question fillQuestion(
            Mission mission,
            int pack,
            int slot,
            int no,
            String difficulty,
            AnswerRotator answers
    ) {
        FillCard fill = pick(mission.fillCards, pack, slot, no);
        String context = pick(mission.contexts, pack, slot, no);
        String example = pick(mission.examples, pack, slot, no);
        String questionBody = fill.pattern
                .replace("{context}", context)
                .replace("{example}", example)
                .replace("{mission}", mission.missionTitle);
        String question = compactFillQuestion(mission, context, example, questionBody, pack, no);
        int answerIndex = answers.next();
        List<String> options = varyFillOptions(place(fill.answer, List.of(fill.distractors), answerIndex), answerIndex, context, example, no);
        return question(mission, pack, slot, no, "FILL", question, options, List.of(answerIndex),
                fillExplanation(fill, context, example),
                tags(mission, "FACT", no), difficulty);
    }

    private static Question situationQuestion(
            Mission mission,
            int pack,
            int slot,
            int no,
            String difficulty,
            AnswerRotator answers
    ) {
        String context = pick(mission.contexts, pack, slot, no);
        String example = pick(mission.examples, pack + 1, slot, no);
        String concern = pick(mission.concerns, pack, slot + 3, no);
        String concept = conceptFor(mission, pack + no, slot, no);
        int situationPattern = Math.floorMod(no + pack + slot, 10);
        String question = switch (situationPattern) {
            case 0 -> contextAt(context) + " " + objectPhrase(example) + " 쓰다가 " + concern + " 점이 보였어요. 어떻게 하는 것이 좋을까요?";
            case 1 -> contextAt(context) + " 친구가 " + example + " 결과를 바로 제출하려고 해요. 어떤 말이 가장 알맞을까요?";
            case 2 -> contextAt(context) + " 모둠이 " + mission.missionTitle + " 주제를 " + example + " 사례로 발표하려고 해요. 어떤 준비가 가장 좋을까요?";
            case 3 -> switch (mission.stage) {
                case 1 -> contextAt(context) + " 두 AI 답이 달라요. " + objectPhrase(concept) + " 살피며 무엇부터 비교할까요?";
                case 2 -> switch (mission.code) {
                    case "S0201" -> contextAt(context) + " 두 답이 다를 때 목적과 조건 중 무엇부터 볼까요?";
                    case "S0203" -> contextAt(context) + " 두 AI 답이 다르면 개인정보가 섞였는지 어떻게 확인할까요?";
                    case "S0202" -> contextAt(context) + " 두 AI 답이 달라요. 질문 조건과 목적을 어떻게 살펴볼까요?";
                    case "S0204" -> contextAt(context) + " AI 답이 엇갈렸어요. 저작권과 자료 출처 중 무엇을 확인할까요?";
                    case "S0205" -> contextAt(context) + " 결과가 서로 다를 때 바꾼 조건을 어떻게 기록할까요?";
                    case "S0206" -> contextAt(context) + " AI 답이 엇갈릴 때 내가 쓴 말과 출처를 어떻게 살펴볼까요?";
                    default -> contextAt(context) + " AI 답 두 개가 다르게 나왔어요. 어떤 정보부터 확인할까요?";
                };
                default -> switch (mission.code) {
                    case "S0302" -> contextAt(context) + " 두 AI 답의 근거가 달라요. 어떤 자료와 맞춰 볼까요?";
                    case "S0303" -> contextAt(context) + " 추천 결과가 엇갈렸어요. 어떤 자료와 함께 확인할까요?";
                    case "S0305" -> contextAt(context) + " AI 의견이 서로 다를 때 어떤 자료로 확인할까요?";
                    default -> contextAt(context) + " 서로 다른 AI 답을 보았어요. 어떤 자료와 맞춰 볼까요?";
                };
            };
            case 4 -> contextAt(context) + " AI가 " + example + "에 대해 자신 있게 말했지만 이유가 부족해 보여요. 어떻게 해야 할까요?";
            case 5 -> switch (mission.code) {
                case "S0204" -> contextAt(context) + " 친구들이 인터넷 그림의 허락과 출처를 두고 의견이 달라졌어요. 어떻게 해결할까요?";
                case "S0206" -> contextAt(context) + " 친구들이 AI 도움을 받은 글을 고치는 방법으로 의견이 달라졌어요. 어떻게 해결할까요?";
                default -> contextAt(context) + " 친구들이 " + objectPhrase(conceptFor(mission, pack, slot, no)) + " 두고 의견이 달라졌어요. 가장 좋은 해결 방법은 무엇일까요?";
            };
            case 6 -> contextAt(context) + " " + materialPhrase(example) + "를 다루다가 " + concern + " 점이 보였어요. 먼저 무엇을 해야 할까요?";
            case 7 -> contextAt(context) + " 숙제를 하다가 " + example + "에 대한 AI 도움을 받았어요. 마지막에는 어떻게 해야 할까요?";
            case 8 -> switch (mission.code) {
                case "S0204" -> contextAt(context) + " " + objectPhrase(example) + " 쓰기 전 저작권과 출처에서 먼저 볼 점은 무엇일까요?";
                case "S0103" -> contextAt(context) + " " + objectPhrase(example) + " 학습 자료로 쓰기 전 먼저 볼 점은 무엇일까요?";
                default -> contextAt(context) + " " + objectPhrase(example) + " 사용하기 전에 먼저 살펴볼 점은 무엇일까요?";
            };
            default -> contextAt(context) + " 발표 자료에 " + example + " 내용을 넣으려 해요. 어떤 행동이 가장 책임 있을까요?";
        };
        String focus = situationFocus(mission, situationPattern, example, concept);
        String correct = situationCorrectAction(mission, situationPattern, concern, pack, slot, no);
        List<String> distractors = distinctPicks(mission.wrongActions, correct, pack + 1, slot, no, 3);
        int answerIndex = answers.next();
        List<String> options = varyChoiceOptions(place(correct, distractors, answerIndex), answerIndex, context, example, no);
        return question(mission, pack, slot, no, "SITUATION", question, options, answerIndex,
                situationExplanation(mission, context, focus, concern, concept, correct, no, situationPattern),
                tags(mission, "SAFETY", no), difficulty);
    }

    private static Question question(
            Mission mission,
            int pack,
            int slot,
            int no,
            String type,
            String question,
            List<String> options,
            Object answer,
            String explanation,
            List<String> tags,
            String difficulty
    ) {
        question = polishText(fixJosa(question, mission));
        explanation = polishText(fixJosa(explanation, mission));
        if (options != null) {
            options = options.stream()
                    .map(option -> polishText(fixJosa(option, mission)))
                    .toList();
        }
        String promptKey = mission.code + "|" + question;
        int occurrence = PROMPT_OCCURRENCES.merge(promptKey, 1, Integer::sum);
        if (occurrence > 1) {
            question = duplicateQuestionVariant(mission, type, question, pack, slot, no, occurrence);
        }
        int globalOccurrence = GLOBAL_PROMPT_OCCURRENCES.merge(question, 1, Integer::sum);
        if (globalOccurrence > 1) {
            question = duplicateQuestionVariant(mission, type, question, pack, slot, no, occurrence + globalOccurrence);
        }
        return new Question(
                mission.code + "-P" + pack + "-" + "%02d".formatted(slot),
                mission.code,
                mission.stage,
                mission.stageTitle,
                mission.missionTitle,
                type,
                question,
                options,
                answer,
                explanation,
                tags,
                mission.curriculumRef,
                "STATIC",
                "PREGENERATED",
                mission.sourceReference,
                difficulty,
                pack
        );
    }

    private static String duplicateQuestionVariant(
            Mission mission,
            String type,
            String originalQuestion,
            int pack,
            int slot,
            int no,
            int occurrence
    ) {
        String context = pick(mission.contexts, pack + occurrence, slot + occurrence, no + occurrence);
        String example = pick(mission.examples, pack + occurrence, slot + occurrence, no + occurrence);
        String concept = conceptFor(mission, pack + occurrence, slot + occurrence, no + occurrence);
        return switch (type) {
            case "OX" -> contextAt(context) + " " + objectPhrase(example)
                    + " 볼 때 " + objectPhrase(concept) + " 판단으로 알맞은 말인지 확인해요.";
            case "FILL" -> duplicateFillQuestion(mission, context, example, concept, originalQuestion);
            case "MULTIPLE" -> duplicateMultipleQuestion(mission, context, example, concept, originalQuestion);
            case "SITUATION" -> duplicateSituationQuestion(mission, context, example, concept, originalQuestion);
            default -> originalQuestion;
        };
    }

    private static String duplicateFillQuestion(
            Mission mission,
            String context,
            String example,
            String concept,
            String originalQuestion
    ) {
        if (originalQuestion.contains("필요한 ____만")) {
            return contextAt(context) + " " + objectPhrase(example)
                    + " 쓸 때 꼭 필요한 ____만 고르는 습관이 좋아요.";
        }
        if (originalQuestion.contains("사용 전 먼저 살펴볼 점")) {
            return contextAt(context) + " " + objectPhrase(example)
                    + " 쓰기 전에는 " + objectPhrase(concept) + " 관련 ____을 확인해요.";
        }
        return contextAt(context) + " " + objectPhrase(concept)
                + " 떠올리며 빈칸에 알맞은 말을 넣어요.";
    }

    private static String duplicateMultipleQuestion(
            Mission mission,
            String context,
            String example,
            String concept,
            String originalQuestion
    ) {
        if (originalQuestion.contains("실천한 모습")) {
            return contextAt(context) + " " + example
                    + " 사례를 볼 때 " + objectPhrase(concept) + " 바르게 판단한 행동은 무엇일까요?";
        }
        if (originalQuestion.contains("사용 전 먼저 살펴볼 점")) {
            return contextAt(context) + " " + objectPhrase(example)
                    + " 활용 전에 먼저 확인할 점은 무엇일까요?";
        }
        if (originalQuestion.contains("피해야 할 행동")) {
            return contextAt(context) + " " + objectPhrase(concept)
                    + " 생각할 때 조심해야 할 행동은 무엇일까요?";
        }
        return contextAt(context) + " " + objectPhrase(concept)
                + " 기준으로 가장 책임 있는 행동은 무엇일까요?";
    }

    private static String duplicateSituationQuestion(
            Mission mission,
            String context,
            String example,
            String concept,
            String originalQuestion
    ) {
        if (originalQuestion.contains("사용하기 전에 먼저 살펴볼 점")) {
            return contextAt(context) + " " + objectPhrase(example)
                    + " 쓰기 전에 " + objectPhrase(concept) + " 관점에서 무엇을 확인할까요?";
        }
        if (originalQuestion.contains("의견이 달라")) {
            return contextAt(context) + " " + objectPhrase(concept)
                    + " 두고 의견이 갈렸어요. 어떤 순서로 해결할까요?";
        }
        return contextAt(context) + " " + example
                + " 상황에서 " + mission.missionTitle + " 내용을 적용하려면 무엇부터 할까요?";
    }

    private static String oxExplanation(Mission mission, int pack, int slot, int no, boolean answer, String context, String claim) {
        if (answer) {
            return "맞아요. " + trueClaimHint(mission, claim);
        }
        return "아니에요. " + falseClaimHint(mission, claim);
    }

    private static String trueClaimHint(Mission mission, String claim) {
        if (containsAny(claim, "근거가 없는 답", "근거가 부족", "출처나 이유", "출처와 이유")) {
            return "근거가 부족한 AI 답은 출처와 이유를 확인해야 해요.";
        }
        if (containsAny(claim, "개인정보가 보이는 자료")) {
            return "개인정보가 보이는 자료는 가리거나 필요한 부분만 사용해야 해요.";
        }
        if (containsAny(claim, "AI 질문에도 개인정보", "개인정보는 넣지 않아야")) {
            return "AI 질문에는 이름, 전화번호처럼 나를 알 수 있는 정보를 빼야 해요.";
        }
        if (containsAny(claim, "얼굴과 목소리", "얼굴", "목소리", "전화번호", "주소", "비밀번호", "나를 알 수")) {
            return "얼굴, 목소리, 이름처럼 개인을 알아볼 수 있는 정보는 조심해서 다뤄야 해요.";
        }
        if (containsAny(claim, "출처 확인", "출처", "저작권", "만든 사람")) {
            return "자료를 쓸 때는 출처와 만든 사람의 권리를 함께 확인해야 해요.";
        }
        if (containsAny(claim, "공정", "편향", "치우")) {
            return "자료와 결과가 한쪽으로 치우치지 않았는지 살펴야 해요.";
        }
        return missionPositiveHint(mission);
    }

    private static String falseClaimHint(Mission mission, String claim) {
        if (containsAny(claim, "친구 글", "친구의 글")) {
            return "친구 글은 허락 없이 AI에 넣지 말고 먼저 허락을 받아야 해요.";
        }
        if (containsAny(claim, "친구 목소리", "목소리")) {
            return "친구 목소리는 개인정보가 될 수 있으므로 허락 없이 녹음하거나 쓰면 안 돼요.";
        }
        if (containsAny(claim, "친구 얼굴", "얼굴 사진", "친구 사진", "허락 없이 써도")) {
            return "친구 얼굴 사진은 개인정보가 될 수 있으므로 허락 없이 사용하면 안 돼요.";
        }
        if (containsAny(claim, "전화번호")) {
            return "전화번호는 개인정보이므로 AI 질문에 넣지 않아야 해요.";
        }
        if (containsAny(claim, "개인정보", "전화번호", "주소", "비밀번호", "위치", "학교와 반", "나를 알 수")) {
            return "이름, 위치, 사진처럼 나를 알 수 있는 정보는 AI에 넣거나 공개하면 안 돼요.";
        }
        if (containsAny(claim, "출처 확인", "출처") && containsAny(claim, "필요 없어", "필요 없", "없어져", "안 해도")) {
            return "AI를 사용해도 자료의 출처와 근거는 직접 확인해야 해요.";
        }
        if (containsAny(claim, "한 종류", "항상 공정", "공정한 결과", "편향", "치우", "특정 지역", "남학생", "여학생")) {
            return "자료가 한쪽으로 치우치면 AI 결과도 공정하지 않을 수 있어요.";
        }
        if (containsAny(claim, "저작권", "인터넷 사진", "다른 사람이 만든", "만든 사람", "마음대로 써도")) {
            return "다른 사람이 만든 글과 그림은 출처와 허락, 만든 사람의 권리를 확인해야 해요.";
        }
        if (containsAny(claim, "표절", "베끼", "내 글처럼")) {
            return "AI 답이나 남의 글을 내 것처럼 쓰면 표절이 될 수 있어요.";
        }
        if (containsAny(claim, "근거", "이유", "자신 있게", "항상 맞", "틀릴 리", "답이 길", "자연스러우면")) {
            return "AI 답은 그럴듯해도 틀릴 수 있으므로 근거와 출처를 확인해야 해요.";
        }
        if (containsAny(claim, "자료를 추가", "자료가 달라도", "결과는 절대", "테스트 없이", "첫 결과", "조건이 달라도")) {
            return "자료나 조건을 바꾸면 AI 결과가 달라질 수 있으므로 테스트가 필요해요.";
        }
        if (containsAny(claim, "사람 얼굴", "얼굴 인식", "사람을 알아보는")) {
            return "사람을 알아보는 AI는 허락과 공정함, 개인정보 보호를 함께 살펴야 해요.";
        }
        return missionNegativeHint(mission);
    }

    private static String multipleExplanation(
            Mission mission,
            String context,
            String example,
            String concept,
            String correct,
            boolean avoidQuestion,
            boolean promptRepairQuestion,
            boolean reasonCheckQuestion,
        int no
    ) {
        if (avoidQuestion) {
            if ("S0203".equals(mission.code) || concept.contains("개인정보")) {
                return "이름, 학교, 반처럼 나를 알 수 있는 개인정보는 공개 질문에 넣지 않아야 해요.";
            }
            return "확인 없이 믿거나 책임을 넘기는 행동은 피해야 해요. "
                    + objectPhrase(concept) + " 먼저 살펴보면 좋아요.";
        }
        if (promptRepairQuestion) {
            return "맞아요. " + correct + " " + contextAt(context) + " AI 답을 원하는 방향으로 다시 물어요.";
        }
        if (reasonCheckQuestion) {
            return "맞아요. " + correct + " " + contextAt(context) + " 근거와 출처를 확인해요.";
        }
        String explanation = "맞아요. " + correct + " " + multipleTail(mission, concept, no);
        if (explanation.length() <= 90) {
            return explanation;
        }
        return "맞아요. " + objectPhrase(concept) + " 살펴보고 AI 결과를 다시 확인해요.";
    }

    private static String missionPositiveHint(Mission mission) {
        return switch (mission.code) {
            case "S0101" -> "AI인지 보려면 겉모습보다 어떤 자료와 일을 하는지 살펴요.";
            case "S0102" -> "규칙만 따르는 도구와 배워서 맞히는 AI는 달라요.";
            case "S0103" -> "AI는 여러 예시 자료에서 특징을 찾아 배울 수 있어요.";
            case "S0104" -> "사진, 소리, 글 인식 결과는 자료 상태에 따라 달라질 수 있어요.";
            case "S0105" -> "AI가 한 일도 사람이 확인하고 책임 있게 사용해야 해요.";
            case "S0201" -> "AI에게 물을 때는 목적, 대상, 형식을 분명히 말해요.";
            case "S0202" -> "AI 답은 목적에 맞게 다시 묻고 내 말로 고쳐 써요.";
            case "S0203" -> "이름, 위치, 얼굴처럼 나를 알 수 있는 정보는 빼야 해요.";
            case "S0204" -> "글과 그림을 쓸 때는 만든 사람의 권리와 출처를 확인해요.";
            case "S0205" -> "결과를 고칠 때는 바꾼 조건과 확인한 내용을 기록해요.";
            case "S0206" -> "AI 답을 그대로 베끼지 말고 출처와 내 생각을 함께 남겨요.";
            case "S0301" -> "사실 확인은 믿을 만한 자료와 날짜, 출처를 함께 봐야 해요.";
            case "S0302" -> "AI 답이 다른 이유를 비교하고 근거 있는 쪽을 찾아요.";
            case "S0303" -> "AI 추천은 한쪽으로 치우칠 수 있어 여러 의견을 살펴요.";
            case "S0304" -> "사람을 알아보는 AI는 공정함과 개인정보를 함께 생각해야 해요.";
            default -> "AI의 좋은 점과 걱정되는 점을 함께 보고 책임 있게 써요.";
        };
    }

    private static String missionNegativeHint(Mission mission) {
        return switch (mission.code) {
            case "S0101" -> "모든 전자기기가 AI는 아니므로 실제 기능을 비교해야 해요.";
            case "S0102" -> "정해진 규칙만 따른다면 스스로 배운 AI라고 보기 어려워요.";
            case "S0103" -> "AI도 충분하고 알맞은 예시가 없으면 잘못 배울 수 있어요.";
            case "S0104" -> "인식 AI도 흐린 자료나 소음 때문에 틀릴 수 있어요.";
            case "S0105" -> "AI가 한 결과라도 마지막 확인과 책임은 사람에게 있어요.";
            case "S0201" -> "목적과 조건을 숨기면 AI 답이 더 알맞아지기 어려워요.";
            case "S0202" -> "AI 답이 맞지 않으면 그대로 쓰지 말고 다시 요청해야 해요.";
            case "S0203" -> "개인정보는 AI가 알아서 지켜 준다고 믿으면 위험해요.";
            case "S0204" -> "AI가 만든 자료라도 출처와 만든 사람의 권리를 확인해야 해요.";
            case "S0205" -> "테스트 없이 첫 결과만 믿으면 고친 이유를 알기 어려워요.";
            case "S0206" -> "AI 답을 내 글처럼 베끼면 표절과 책임 문제가 생길 수 있어요.";
            case "S0301" -> "출처가 없거나 오래된 답은 그대로 믿지 말고 확인해야 해요.";
            case "S0302" -> "AI 답이 서로 다르면 조건과 자료를 비교해야 해요.";
            case "S0303" -> "추천 결과만 보면 다른 생각이나 자료를 놓칠 수 있어요.";
            case "S0304" -> "공정함과 개인정보를 빼고 사람 인식 AI를 쓰면 위험해요.";
            default -> "편리하다는 이유만으로 걱정되는 점을 무시하면 안 돼요.";
        };
    }

    private static String multipleTail(Mission mission, String concept, int no) {
        if ("S0203".equals(mission.code) || concept.contains("개인정보")) {
            return "이름, 사진, 위치가 드러나지 않는지도 확인해요.";
        }
        return switch (Math.floorMod(no + mission.stage, 6)) {
            case 0 -> "출처와 목적에 맞는지도 함께 봐요.";
            case 1 -> "왜 그런 결과가 나왔는지 한 번 더 살펴요.";
            case 2 -> "내가 쓸 내용과 맞는지 다시 읽어요.";
            case 3 -> "필요한 부분만 골라 내 말로 고쳐요.";
            case 4 -> "친구나 선생님과 비교하면 더 안전해요.";
            default -> "잘못된 정보나 빠진 점이 없는지 확인해요.";
        };
    }

    private static String fillExplanation(FillCard fill, String context, String example) {
        return fill.explanation;
    }

    private static String promptRepairQuestion(String context, String example) {
        if (example.endsWith("답")) {
            return contextAt(context) + " AI 답이 목적과 달라요. 질문에서 무엇을 더 분명히 할까요?";
        }
        return contextAt(context) + " AI가 " + objectPhrase(example)
                + " 도와줬지만 답이 목적과 달라요. 무엇을 더 분명히 할까요?";
    }

    private static String reasonCheckQuestion(String context, String example) {
        return contextAt(context) + " AI가 " + objectPhrase(example)
                + " 설명했지만 이유가 부족해 보여요. 무엇을 더 물어볼까요?";
    }

    private static String situationCorrectAction(
            Mission mission,
            int situationPattern,
            String concern,
            int pack,
            int slot,
            int no
    ) {
        int seed = no + mission.code.hashCode();
        return switch (situationPattern) {
            case 0, 6 -> concernCorrectAction(concern, pack, slot, seed);
            case 1 -> pick(new String[]{
                    "AI 결과를 바로 내지 말고 한 번 더 확인해요.",
                    "답의 출처와 내용을 확인한 뒤 제출해요.",
                    "AI 답을 읽고 맞는 부분만 내 말로 정리해요.",
                    "틀릴 수 있음을 알려 주고 함께 확인해요."
            }, pack, slot, seed);
            case 2 -> pick(new String[]{
                    "사례와 출처, 확인한 내용을 함께 준비해요.",
                    "발표할 자료가 믿을 만한지 먼저 확인해요.",
                    "어떤 자료로 확인했는지 발표에 함께 적어요.",
                    "좋은 점과 조심할 점을 같이 설명해요."
            }, pack, slot, seed);
            case 3 -> situationCompareAction(mission, pack, slot, seed);
            case 4 -> pick(new String[]{
                    "왜 그런 답인지 근거를 더 물어봐요.",
                    "이유와 출처를 확인하고 다른 자료와 비교해요.",
                    "근거가 부족하면 바로 쓰지 않고 다시 확인해요.",
                    "어떤 자료를 보고 말했는지 물어봐요."
            }, pack, slot, seed);
            case 5 -> pick(new String[]{
                    "각 의견의 이유를 듣고 함께 정해요.",
                    "서로의 근거를 비교하고 모두에게 알맞은지 봐요.",
                    "한쪽 말만 듣지 말고 여러 의견을 모아요.",
                    "살펴볼 점을 정하고 차례대로 비교해요."
            }, pack, slot, seed);
            case 7 -> pick(new String[]{
                    "AI 도움을 읽고 내 말로 고쳐요.",
                    "마지막에는 내가 이해한 말로 정리해요.",
                    "AI 답이 맞는지 확인하고 내 생각을 더해요.",
                    "도움을 받은 부분과 내 생각을 구분해요."
            }, pack, slot, seed);
            case 8 -> pick(new String[]{
                    "개인정보와 출처를 먼저 확인해요.",
                    "왜 쓰는지와 어떤 자료가 들어가는지 살펴요.",
                    "허락이 필요한 자료인지 먼저 확인해요.",
                    "안전하게 쓸 수 있는지 체크해요."
            }, pack, slot, seed);
            default -> pick(new String[]{
                    "출처와 허락을 확인하고 내 생각을 더해요.",
                    "AI가 만든 내용과 내가 확인한 내용을 구분해요.",
                    "그대로 베끼지 말고 책임 있게 정리해요.",
                    "발표에 넣기 전에 사실과 권리를 확인해요."
            }, pack, slot, seed);
        };
    }

    private static String situationCompareAction(Mission mission, int pack, int slot, int seed) {
        if ("S0203".equals(mission.code)) {
            return pick(new String[]{
                    "이름, 학교, 위치처럼 나를 알 수 있는 정보가 있는지 살펴봐요.",
                    "개인정보가 섞였는지 먼저 확인해요.",
                    "친구나 내 정보가 들어갔는지 찾아봐요.",
                    "나를 알아볼 정보는 지우고 다시 확인해요."
            }, pack, slot, seed);
        }
        if ("S0204".equals(mission.code)) {
            return pick(new String[]{
                    "자료 출처와 만든 사람의 권리를 확인해요.",
                    "저작권과 사용 허락을 먼저 살펴봐요.",
                    "출처가 있는 자료인지 비교해요.",
                    "허락받은 자료인지 확인하고 기록해요."
            }, pack, slot, seed);
        }
        if ("S0205".equals(mission.code)) {
            return pick(new String[]{
                    "바꾼 조건과 결과를 차례로 기록해요.",
                    "무엇을 바꾸었는지 적고 다시 비교해요.",
                    "조건 차이를 기록한 뒤 결과를 확인해요.",
                    "수정한 내용과 달라진 답을 함께 적어요."
            }, pack, slot, seed);
        }
        if ("S0206".equals(mission.code)) {
            return pick(new String[]{
                    "내가 쓴 말과 출처를 함께 살펴봐요.",
                    "AI 도움을 받은 부분과 내 생각을 구분해요.",
                    "출처가 필요한 내용인지 먼저 확인해요.",
                    "내 말로 정리했는지 다시 확인해요."
            }, pack, slot, seed);
        }
        if (mission.stage == 3) {
            return pick(new String[]{
                    "공식 자료나 믿을 만한 출처와 맞춰 봐요.",
                    "날짜와 출처가 있는 자료와 비교해요.",
                    "선생님이 알려 준 자료와 함께 확인해요.",
                    "근거가 있는 자료와 나란히 비교해요."
            }, pack, slot, seed);
        }
        return pick(new String[]{
                "두 답의 출처와 조건을 차례로 비교해요.",
                "서로 다른 부분을 표시하고 믿을 만한 자료로 확인해요.",
                "질문 조건이 달랐는지 먼저 살펴봐요.",
                "공통점과 다른 점을 나누어 다시 확인해요."
        }, pack, slot, seed);
    }

    private static String concernCorrectAction(String concern, int pack, int slot, int seed) {
        if (containsAny(concern, "개인정보", "실제 이름", "위치", "친구 얼굴", "전화번호", "목소리", "개인 얼굴")) {
            return pick(new String[]{
                    "개인정보가 들어간 부분을 지우고 다시 확인해요.",
                    "이름이나 사진처럼 나를 알 수 있는 정보는 빼요.",
                    "친구나 내 개인정보가 보이면 사용하지 않아요.",
                    "필요 없는 개인 정보는 넣지 않고 다시 질문해요."
            }, pack, slot, seed);
        }
        if (containsAny(concern, "추천 이유를 모르는")) {
            return pick(new String[]{
                    "추천 이유를 확인하고 바로 믿지 않아요.",
                    "왜 추천됐는지 물어보고 다른 자료도 봐요.",
                    "추천 기준을 알기 어려우면 다시 살펴요.",
                    "비슷한 추천만 보지 말고 다른 선택지도 찾아요."
            }, pack, slot, seed);
        }
        if (containsAny(concern, "질문이 너무 모호한", "질문 조건이 모호한", "목적이 빠진", "대상이 분명하지 않은", "형식이 없는", "질문 조건이 빠진")) {
            return pick(new String[]{
                    "질문의 목적과 조건을 더 분명히 써요.",
                    "필요한 조건을 한두 가지 더해 다시 물어요.",
                    "누가 볼 답인지와 원하는 형식을 알려 줘요.",
                    "모호한 말을 쉬운 조건으로 바꾸어 질문해요."
            }, pack, slot, seed);
        }
        if (containsAny(concern, "출처", "공식 출처", "작성자가 불분명한")) {
            return pick(new String[]{
                    "자료의 출처와 만든 사람을 확인해요.",
                    "출처가 없으면 믿을 만한 자료를 더 찾아요.",
                    "공식 자료인지 먼저 확인하고 사용해요.",
                    "출처를 적고 자료가 믿을 만한지 살펴요."
            }, pack, slot, seed);
        }
        if (containsAny(concern, "날짜", "오래된", "날짜가 맞지 않는")) {
            return pick(new String[]{
                    "자료의 날짜가 지금도 맞는지 확인해요.",
                    "오래된 정보인지 보고 최신 자료와 비교해요.",
                    "날짜가 중요한 정보는 다시 찾아봐요.",
                    "언제 만든 자료인지 먼저 확인해요."
            }, pack, slot, seed);
        }
        if (containsAny(concern, "근거")) {
            return pick(new String[]{
                    "왜 그런지 알려 주는 자료를 찾아요.",
                    "근거가 부족하면 바로 쓰지 않고 다시 확인해요.",
                    "답의 이유와 출처를 더 물어봐요.",
                    "믿을 만한 자료로 근거를 확인해요."
            }, pack, slot, seed);
        }
        if (containsAny(concern, "허락", "저작권")) {
            return pick(new String[]{
                    "허락받은 자료인지 먼저 확인해요.",
                    "만든 사람의 권리를 확인하고 사용해요.",
                    "직접 만든 자료나 사용 가능한 자료를 골라요.",
                    "출처와 허락을 적고 자료를 사용해요."
            }, pack, slot, seed);
        }
        if (containsAny(concern, "그대로 베낀", "AI 도움을 숨긴", "이해하지 못한")) {
            return pick(new String[]{
                    "AI 답을 읽고 내 말로 다시 정리해요.",
                    "AI 도움을 받은 부분을 필요하면 밝혀요.",
                    "이해한 내용만 내 생각과 함께 써요.",
                    "그대로 베끼지 말고 확인한 뒤 고쳐요."
            }, pack, slot, seed);
        }
        if (containsAny(concern, "학습 자료를 확인하지 않은", "자료가 한쪽", "자료가 한쪽으로", "자료가 한쪽으로 몰린", "한 집단만", "지역이 한쪽", "계절이 한 가지", "빠진 의견")) {
            return pick(new String[]{
                    "자료가 골고루 모였는지 확인해요.",
                    "빠진 사람이나 상황이 없는지 살펴요.",
                    "여러 종류의 예시를 더 모아 비교해요.",
                    "한쪽 자료만으로 결론을 내리지 않아요."
            }, pack, slot, seed);
        }
        if (containsAny(concern, "테스트", "새 자료", "틀린 예시", "AI가 잘못", "잘못 분류", "사진을 잘못", "이름표가 틀린")) {
            return pick(new String[]{
                    "틀린 예시를 모아 원인을 찾아요.",
                    "새 자료로 다시 테스트해요.",
                    "잘못된 이름표나 자료를 고쳐요.",
                    "무엇이 틀렸는지 기록하고 다시 확인해요."
            }, pack, slot, seed);
        }
        if (containsAny(concern, "기준", "이의 제기", "사람 확인", "누군가에게 불리한", "한쪽 의견")) {
            return pick(new String[]{
                    "결정 이유를 설명할 수 있는지 확인해요.",
                    "사람이 다시 확인할 기회를 두어요.",
                    "불리한 사람이 없는지 함께 살펴요.",
                    "여러 의견을 듣고 살펴볼 점을 정해요."
            }, pack, slot, seed);
        }
        if (containsAny(concern, "감시", "의존", "좋은 점만", "AI에 지나치게", "개인정보가 많이")) {
            return pick(new String[]{
                    "좋은 점과 걱정되는 점을 함께 적어요.",
                    "AI에만 맡기지 말고 내가 다시 생각해요.",
                    "누가 불편할 수 있는지 살펴봐요.",
                    "개인정보가 많이 모이지 않는지 확인해요."
            }, pack, slot, seed);
        }
        if (containsAny(concern, "자동화", "규칙과 학습", "계산 결과", "기능을 잘못")) {
            return pick(new String[]{
                    "정해진 규칙인지 AI가 배운 기능인지 나누어 봐요.",
                    "겉모습보다 실제로 하는 일을 살펴요.",
                    "계산 결과와 AI 판단을 구별해요.",
                    "어떤 자료를 보고 판단하는 기능인지 확인해요."
            }, pack, slot, seed);
        }
        if (containsAny(concern, "소음", "흐린", "글자가 잘린")) {
            return pick(new String[]{
                    "소리와 사진 상태를 바꾸어 다시 확인해요.",
                    "흐리거나 잘린 자료는 따로 표시해요.",
                    "결과에 영향을 준 조건을 살펴요.",
                    "더 또렷한 자료로 다시 비교해요."
            }, pack, slot, seed);
        }
        if (containsAny(concern, "광고", "홍보")) {
            return pick(new String[]{
                    "광고나 홍보 목적이 섞였는지 살펴요.",
                    "개인 의견과 객관적 자료를 구분해요.",
                    "홍보 글이면 다른 자료도 함께 확인해요.",
                    "누가 왜 만든 자료인지 먼저 봐요."
            }, pack, slot, seed);
        }
        return "문제가 된 점을 찾고 믿을 만한 자료로 다시 확인해요.";
    }

    private static String situationExplanation(
            Mission mission,
            String context,
            String focus,
            String concern,
            String concept,
            String correct,
            int no,
            int situationPattern
    ) {
        String advice = situationPattern == 0 || situationPattern == 6
                ? concernAdvice(concern, focus)
                : situationPatternAdvice(situationPattern, focus);
        String followUp = situationFollowUp(correct, concept, no);
        String explanation = "맞아요. " + advice + " " + followUp;
        if (explanation.length() <= 90) {
            return explanation;
        }
        return "맞아요. " + advice + " 바로 믿지 말고 다시 살펴요.";
    }

    private static String situationPatternAdvice(int situationPattern, String example) {
        String answer = answerLabel(example);
        return switch (situationPattern) {
            case 1 -> example + " 결과는 제출하기 전에 내용과 출처를 확인해요.";
            case 2 -> example + " 사례 발표에는 출처와 확인한 내용을 함께 준비해요.";
            case 3 -> compareAdvice(example);
            case 4 -> answer + "의 이유가 부족하면 근거와 출처를 더 확인해요.";
            case 5 -> "의견이 다를 때는 각자 이유를 듣고 살펴볼 점을 함께 정해요.";
            case 7 -> example + " 도움을 받은 뒤에는 내 말로 고치고 사실을 확인해요.";
            case 8 -> example + " 사용 전에는 개인정보, 출처, 목적을 먼저 살펴요.";
            default -> example + " 발표 자료에는 출처와 허락, 내 생각을 함께 확인해요.";
        };
    }

    private static String compareAdvice(String focus) {
        if ("개인정보".equals(focus)) {
            return "두 답에 개인정보가 섞였는지 먼저 확인해요.";
        }
        if (focus.contains("저작권")) {
            return "저작권과 자료 출처를 각각 확인해요.";
        }
        if (focus.contains("바꾼 조건")) {
            return "바꾼 조건과 결과를 함께 기록해요.";
        }
        if (focus.contains("내가 쓴 말")) {
            return "내가 쓴 말과 출처를 나누어 확인해요.";
        }
        if (focus.contains("질문 조건")) {
            return "두 답의 질문 조건과 목적을 나란히 비교해요.";
        }
        if ("자료".equals(focus)) {
            return "공식 자료나 믿을 만한 출처와 맞춰 봐요.";
        }
        return "서로 다른 " + answerLabel(focus) + "은 조건과 믿을 만한 자료를 비교해요.";
    }

    private static String situationFocus(Mission mission, int situationPattern, String example, String concept) {
        if (situationPattern == 3 && mission.stage == 1) {
            return concept;
        }
        if (situationPattern == 3 && "S0203".equals(mission.code)) {
            return "개인정보";
        }
        if (situationPattern == 3 && "S0204".equals(mission.code)) {
            return "저작권과 자료 출처";
        }
        if (situationPattern == 3 && "S0205".equals(mission.code)) {
            return "바꾼 조건";
        }
        if (situationPattern == 3 && "S0206".equals(mission.code)) {
            return "내가 쓴 말과 출처";
        }
        if (situationPattern == 3 && ("S0201".equals(mission.code) || "S0202".equals(mission.code))) {
            return "질문 조건과 목적";
        }
        if (situationPattern == 3 && mission.stage == 3) {
            return "자료";
        }
        if (situationPattern == 3 || situationPattern == 4) {
            return resultLabel(example);
        }
        return example;
    }

    private static String answerLabel(String focus) {
        if (focus.endsWith("답")) {
            return focus;
        }
        if (focus.endsWith("조건") || focus.endsWith("개인정보")) {
            return focus;
        }
        if (focus.endsWith("정보") || focus.endsWith("자료") || focus.endsWith("의견")
                || focus.endsWith("출처")) {
            return focus + " 답";
        }
        if (focus.endsWith("인식") || focus.endsWith("분석") || focus.endsWith("추천")
                || focus.endsWith("결과")) {
            return focus + " 답";
        }
        return resultLabel(focus);
    }

    private static String resultLabel(String example) {
        if (example.endsWith("답") || example.endsWith("결과") || example.endsWith("자료")
                || example.endsWith("정보") || example.endsWith("의견") || example.endsWith("출처")) {
            return example;
        }
        return example + " 답";
    }

    private static String concernAdvice(String concern, String example) {
        if (containsAny(concern, "개인정보", "실제 이름", "위치", "친구 얼굴", "전화번호", "목소리", "개인 얼굴")) {
            return example + "에 이름, 주소, 사진 같은 개인정보가 들어가지 않았는지 확인해요.";
        }
        if (containsAny(concern, "추천 이유를 모르는")) {
            return example + " 결과가 왜 나왔는지 알 수 있는지 확인해요.";
        }
        if (containsAny(concern, "질문이 너무 모호한", "질문 조건이 모호한", "목적이 빠진", "대상이 분명하지 않은", "형식이 없는", "질문 조건이 빠진")) {
            return example + " 질문의 목적과 조건이 분명한지 확인해요.";
        }
        if (containsAny(concern, "출처", "공식 출처", "작성자가 불분명한")) {
            return example + " 자료의 출처와 만든 사람이 빠진 점이 없는지 확인해요.";
        }
        if (containsAny(concern, "날짜", "오래된", "날짜가 맞지 않는")) {
            return example + " 자료의 날짜가 지금도 맞는지 확인해요.";
        }
        if (containsAny(concern, "근거")) {
            return example + " 답에 왜 그런지 알려 주는 자료가 있는지 확인해요.";
        }
        if (containsAny(concern, "허락", "저작권")) {
            return example + " 자료의 허락과 만든 사람의 권리를 먼저 확인해요.";
        }
        if (containsAny(concern, "그대로 베낀", "AI 도움을 숨긴", "이해하지 못한")) {
            return example + "에 AI 도움을 받았는지 밝히고 내 말로 정리해요.";
        }
        if (containsAny(concern, "학습 자료를 확인하지 않은", "자료가 한쪽", "자료가 한쪽으로", "자료가 한쪽으로 몰린", "한 집단만", "지역이 한쪽", "계절이 한 가지", "빠진 의견")) {
            return example + " 자료가 한쪽으로 치우치지 않았는지 살펴요.";
        }
        if (containsAny(concern, "테스트", "새 자료", "틀린 예시", "AI가 잘못", "잘못 분류", "사진을 잘못")) {
            return example + "의 틀린 결과가 왜 나왔는지 새 자료로 확인해요.";
        }
        if (containsAny(concern, "기준", "이의 제기", "사람 확인", "누군가에게 불리한", "한쪽 의견")) {
            return example + " 결정 이유와 사람이 다시 확인할 기회가 있는지 살펴요.";
        }
        if (containsAny(concern, "감시", "의존", "좋은 점만", "AI에 지나치게", "개인정보가 많이")) {
            return example + "의 편리함뿐 아니라 걱정되는 점도 함께 살펴요.";
        }
        if (containsAny(concern, "자동화", "규칙과 학습", "계산 결과", "기능을 잘못")) {
            return example + "가 정해진 규칙인지 AI가 배운 기능인지 나누어 봐요.";
        }
        if (containsAny(concern, "소음", "흐린", "글자가 잘린")) {
            return example + "에서 소리와 사진 상태가 결과에 영향을 주었는지 살펴요.";
        }
        if (containsAny(concern, "광고", "홍보")) {
            return example + "에 광고나 홍보 목적이 섞였는지 살펴요.";
        }
        return "문제가 된 점을 먼저 찾고 다시 확인해요.";
    }

    private static String situationFollowUp(String correct, String concept, int no) {
        if (containsAny(correct, "출처", "날짜", "근거", "공식")) {
            return "믿을 만한 자료와 비교하면 더 안전해요.";
        }
        if (containsAny(correct, "개인정보", "이름", "주소", "전화번호", "얼굴", "목소리", "위치")) {
            return "필요 없는 개인 정보는 빼고 사용해요.";
        }
        if (containsAny(correct, "질문", "조건", "목적", "형식", "대상", "누가 읽을")) {
            return "필요한 조건을 더해 다시 물어보면 좋아요.";
        }
        if (containsAny(correct, "테스트", "다시 확인", "비교", "틀린", "기록")) {
            return "바꾼 점을 적고 결과를 비교해요.";
        }
        if (containsAny(correct, "허락", "저작권", "권리")) {
            return "허락받은 자료인지 확인하고 사용해요.";
        }
        if (containsAny(correct, "공정", "불리", "의견", "대표")) {
            return "모두에게 억울하지 않은지도 생각해요.";
        }
        return switch (Math.floorMod(no, 4)) {
            case 0 -> "바로 믿지 말고 고른 행동을 실천해요.";
            case 1 -> "필요하면 선생님이나 친구와 함께 확인해요.";
            case 2 -> "AI 결과보다 확인 과정이 더 중요해요.";
            default -> "내가 다시 생각해 보고 책임 있게 사용해요.";
        };
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String compactFillQuestion(
            Mission mission,
            String context,
            String example,
            String questionBody,
            int pack,
            int no
    ) {
        questionBody = adaptFillBody(questionBody, pack, no);
        if (questionBody.contains(context) || questionBody.contains(example) || questionBody.contains(mission.missionTitle)) {
            return questionBody;
        }
        if (questionBody.length() >= 55) {
            return questionBody;
        }
        return switch (Math.floorMod(no, 3)) {
            case 0 -> contextAt(context) + " " + questionBody;
            case 1 -> example + " 예를 떠올려요. " + questionBody;
            default -> questionBody;
        };
    }

    private static String adaptFillBody(String questionBody, int pack, int no) {
        if (questionBody.contains("처럼 사용자의 자료를 보고 알맞은 답을 고르는 기능은 ____과 관련이 있어요.")
                && no % 2 == 0) {
            return "____는 사용자의 자료를 보고 알맞은 답을 고르는 데 쓰일 수 있어요.";
        }
        if (pack > 3 && questionBody.equals("AI 기능을 찾을 때는 겉모습보다 ____을 먼저 살펴요.")) {
            return no % 2 == 0
                    ? "AI인지 보려면 겉모습보다 하는 ____을 살펴요."
                    : "AI를 구별할 때는 모양보다 실제 ____을 보아요.";
        }
        if (pack > 3 && questionBody.equals("계산기는 보통 사람이 정한 ____을 그대로 따라요.")) {
            return no % 2 == 0
                    ? "계산기는 사람이 미리 정한 ____대로 움직여요."
                    : "계산기는 새로 배우기보다 정해진 ____을 따라요.";
        }
        if (pack > 3 && questionBody.equals("AI가 한쪽 자료만 보고 배우지 않으려면 자료의 ____이 필요해요.")) {
            return no % 2 == 0
                    ? "AI가 여러 경우를 보려면 자료의 ____을 살펴야 해요."
                    : "한쪽으로 치우치지 않으려면 자료가 ____해야 해요.";
        }
        if (pack > 3 && questionBody.equals("얼굴이나 목소리 자료는 ____와 관련될 수 있어 조심해야 해요.")) {
            return no % 2 == 0
                    ? "얼굴이나 목소리는 나를 알아볼 수 있어 ____처럼 다뤄요."
                    : "사람을 알아볼 수 있는 얼굴·목소리는 ____로 조심해요.";
        }
        if (questionBody.equals("AI가 글과 사진을 알아보려면 여러 ____가 필요해요.") && no % 2 == 0) {
            return "AI는 글과 사진의 특징을 보려면 다양한 ____를 살펴야 해요.";
        }
        if (pack > 3 && questionBody.equals("사진이나 소리를 보고 무엇인지 알아보는 기능은 ____과 관련이 있어요.")) {
            return no % 2 == 0
                    ? "사진이나 소리에서 특징을 찾아 이름을 맞히는 일은 ____이에요."
                    : "AI가 사진·소리의 특징을 보고 맞히는 기능은 ____과 이어져요.";
        }
        if (questionBody.equals("AI가 틀릴 수 있다는 점을 아는 것은 안전한 ____ 습관이에요.") && no % 2 == 0) {
            return "AI 답도 틀릴 수 있음을 떠올리며 쓰는 습관은 안전한 ____예요.";
        }
        if (pack > 3 && questionBody.equals("AI 답은 그럴듯해도 틀릴 수 있으므로 ____해야 해요.")) {
            return no % 2 == 0
                    ? "그럴듯한 AI 답도 다른 자료로 ____해요."
                    : "바로 제출하기 전에는 AI 답을 다시 ____해요.";
        }
        if (questionBody.equals("AI 질문에는 필요한 정보만 쓰는 ____ 원칙이 중요해요.")) {
            if (pack > 3) {
                return "질문에 넣는 정보는 ____로 줄여 개인정보를 지켜요.";
            }
            if (no % 2 == 0) {
                return "AI에게 물어볼 때는 꼭 필요한 정보만 넣는 ____ 원칙을 지켜요.";
            }
        }
        if (pack > 3 && questionBody.equals("이름, 주소, 전화번호처럼 나를 알아볼 수 있는 정보는 ____예요.")) {
            return no % 2 == 0
                    ? "나를 알아볼 수 있는 이름·주소·전화번호는 ____예요."
                    : "전화번호나 집 주소처럼 나를 드러내는 정보가 ____예요.";
        }
        if (questionBody.equals("추천 AI가 비슷한 것만 보여 줄 때는 다른 ____도 찾아봐야 해요.")) {
            if (pack > 3) {
                return "추천이 한쪽으로 몰리면 반대되는 ____도 살펴요.";
            }
            if (no % 2 == 0) {
                return "비슷한 추천만 이어질 때는 다른 ____을 함께 찾아요.";
            }
        }
        if (pack > 3 && questionBody.equals("AI가 자주 틀리는 예시는 원인을 찾기 위해 따로 ____해요.")) {
            return no % 2 == 0
                    ? "AI가 자주 헷갈리는 예시는 따로 ____ 원인을 찾아요."
                    : "틀린 예시를 ____ 보면 어떤 조건에서 약한지 알 수 있어요.";
        }
        if (pack > 3 && questionBody.equals("AI 결과를 고쳐 보려면 바꾼 내용을 ____해 두면 좋아요.")) {
            return no % 2 == 0
                    ? "결과가 달라졌는지 보려면 바꾼 점을 ____해요."
                    : "AI를 고칠 때는 무엇을 바꾸었는지 ____으로 남겨요.";
        }
        if (pack > 3 && questionBody.equals("팩트체크에는 공식 기관이나 믿을 만한 ____가 도움이 돼요.")) {
            return no % 2 == 0
                    ? "사실을 확인할 때는 믿을 만한 ____를 함께 봐요."
                    : "공식 기관 자료처럼 믿을 만한 ____가 팩트체크에 좋아요.";
        }
        if (pack > 3 && questionBody.equals("AI에게 질문할 때는 먼저 무엇을 하려는지 ____을 말해요.")) {
            return no % 2 == 0
                    ? "AI에게 물어보기 전에는 내가 원하는 ____을 또렷하게 적어요."
                    : "좋은 답을 얻으려면 질문 속에 ____을 분명히 넣어요.";
        }
        if (pack > 3 && questionBody.equals("AI 답이 목적에 맞지 않으면 조건을 더해 ____할 수 있어요.")) {
            return no % 2 == 0
                    ? "원한 답이 아니면 조건을 보태어 질문을 ____해요."
                    : "목적과 다른 답이 나오면 질문을 다시 ____할 수 있어요.";
        }
        if (pack > 3 && questionBody.equals("AI 학습 자료를 모을 때는 먼저 사용 ____을 확인해요.")) {
            return no % 2 == 0
                    ? "AI에 넣을 자료는 써도 되는지 ____을 먼저 살펴요."
                    : "학습 자료를 모으기 전에는 사용 ____부터 확인해요.";
        }
        if (pack > 3 && questionBody.equals("AI가 만든 글이나 그림도 사용할 때는 ____을 생각해야 해요.")) {
            return no % 2 == 0
                    ? "AI가 만든 결과를 쓸 때도 만든 사람의 ____을 생각해요."
                    : "AI 결과물을 내 작품에 넣기 전에는 ____ 문제를 살펴요.";
        }
        if (pack > 3 && questionBody.equals("AI 도움을 받은 뒤 마지막 답은 ____ 말로 정리해요.")) {
            return no % 2 == 0
                    ? "AI가 도와준 내용도 끝에는 ____ 말로 고쳐 써요."
                    : "제출하기 전에는 AI 답을 읽고 ____ 표현으로 정리해요.";
        }
        if (pack > 3 && questionBody.equals("AI 답에서 가장 먼저 확인할 중심 내용은 ____ 주장이에요.")) {
            return no % 2 == 0
                    ? "AI 답을 볼 때는 먼저 ____ 주장이 무엇인지 찾아요."
                    : "글의 중심이 되는 ____ 주장을 확인한 뒤 자료를 살펴요.";
        }
        if (pack > 3 && questionBody.equals("공식 자료와 개인 의견은 같은지 다른지 ____해야 해요.")) {
            return no % 2 == 0
                    ? "믿을 만한 자료와 개인 의견은 나누어 ____해요."
                    : "자료가 공식 안내인지 개인 생각인지 ____해 보아요.";
        }
        if (pack > 3 && questionBody.equals("자료를 만든 사람이나 기관을 알려 주는 정보는 ____예요.")) {
            return no % 2 == 0
                    ? "누가 만든 자료인지 알려 주는 표시는 ____예요."
                    : "자료의 만든 곳을 보여 주는 정보가 ____예요.";
        }
        if (questionBody.equals("추천 서비스가 비슷한 내용만 보여 주면 다른 ____도 찾아봐요.") && no % 2 == 0) {
            return "비슷한 추천만 계속 나오면 다른 ____을 찾아 넓게 보아요.";
        }
        if (pack > 3 && questionBody.equals("AI 추천은 최종 결정이 아니라 ____ 자료로 쓰는 것이 좋아요.")) {
            return no % 2 == 0
                    ? "AI 추천은 바로 결정하지 말고 ____ 자료로 참고해요."
                    : "마지막 선택 전에는 AI 추천을 ____ 자료로만 보아요.";
        }
        if (pack > 3 && questionBody.equals("AI 결정이 모두에게 알맞은지 살피는 기준은 ____이에요.")) {
            return no % 2 == 0
                    ? "AI의 결정이 누구에게 불리하지 않은지 보는 일은 ____이에요."
                    : "모두에게 억울하지 않은지 살피는 기준을 ____이라고 해요.";
        }
        if (pack <= 3) {
            return questionBody;
        }
        return questionBody
                .replace("AI가 추천한 내용은 바로 믿기보다 ____해 보는 태도가 필요해요.",
                        "추천 결과는 바로 쓰기 전에 ____해 보아야 해요.")
                .replace("모든 전자기기가 AI는 아니므로 ____을 구별해야 해요.",
                        "전자기기가 모두 AI는 아니에요. 하는 ____을 보고 구별해요.")
                .replace("AI가 배운 뒤에는 새 자료로 ____해 보아야 해요.",
                        "AI가 잘 배웠는지는 새 자료로 ____해 보며 알아봐요.")
                .replace("학습에 쓰지 않은 새 자료로 확인하는 과정은 ____예요.",
                        "처음 보는 자료로 다시 살펴보는 과정은 ____예요.")
                .replace("AI 답은 그럴듯해도 틀릴 수 있으므로 ____해야 해요.",
                        "AI 답이 그럴듯해도 바로 쓰기 전에는 ____해야 해요.")
                .replace("AI 질문에는 이름, 주소 같은 ____를 넣지 않아야 해요.",
                        "AI에게 물어볼 때 이름과 주소 같은 ____는 빼야 해요.")
                .replace("AI 답을 그대로 베끼면 ____ 문제가 생길 수 있어요.",
                        "AI 답을 내 생각처럼 베끼면 ____ 문제가 될 수 있어요.")
                .replace("AI 답과 다른 자료를 함께 보는 것은 자료 ____예요.",
                        "AI 답을 다른 자료와 나란히 보는 일은 자료 ____예요.")
                .replace("자료가 한쪽으로 치우친 상태를 ____이라고 해요.",
                        "자료가 한쪽에 몰린 상태를 ____이라고 해요.")
                .replace("추천 서비스가 비슷한 내용만 보여 주면 다른 ____도 찾아봐요.",
                        "추천 결과가 비슷하게만 나오면 다른 ____도 살펴봐요.")
                .replace("AI를 쓸 때 마지막 판단과 ____은 사람에게 있어요.",
                        "AI를 사용해도 마지막 선택과 ____은 사람에게 있어요.")
                .replace("AI 결과가 왜 나왔는지 알 수 있어야 ____하기 쉬워요.",
                        "AI 결과의 이유를 알면 더 잘 ____할 수 있어요.");
    }

    private static String objectPhrase(String word) {
        return word + (hasFinalConsonant(word) ? "을" : "를");
    }

    private static String contextAt(String context) {
        if (context.endsWith("때")) {
            return context + "는";
        }
        if (context.endsWith("시간") || context.endsWith("회의") || context.endsWith("토의")
                || context.endsWith("활동") || context.endsWith("조사") || context.endsWith("검색")
                || context.endsWith("분석") || context.endsWith("학습") || context.endsWith("수업")) {
            return context + "에";
        }
        return context + "에서";
    }

    private static String materialPhrase(String example) {
        if (example.endsWith("자료") || example.endsWith("정보") || example.endsWith("데이터")) {
            return example;
        }
        return example + " 자료";
    }

    private static String polishText(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String fixed = text;
        fixed = fixed.replace("AI에 질문", "AI에게 질문");
        fixed = fixed.replace("자료 자료", "자료");
        fixed = fixed.replace("정보 정보", "정보");
        fixed = fixed.replace("문장 문장", "문장");
        fixed = fixed.replace("데이터 데이터", "데이터");
        fixed = fixed.replace("때에서", "때는");
        fixed = fixed.replace("새 AI 기능인 ", "");
        fixed = fixed.replace("AI 답이 마음에 들지 않아요", "AI 답이 내가 원한 내용과 맞지 않아요");
        fixed = fixed.replace("확인을 확인", "다시 살펴볼 점을 확인");
        fixed = fixed.replace("답 AI 답", "AI 답");
        fixed = fixed.replace("답 답", "답");
        fixed = fixed.replace("개인 정보", "개인정보");
        fixed = fixed.replace("활용할", "사용할");
        fixed = fixed.replace("활용하면", "사용하면");
        fixed = fixed.replace("활용", "사용");
        fixed = fixed.replace(" 관점의", "을 배울 때");
        fixed = fixed.replace(" 관점으로", "을 볼 때");
        fixed = fixed.replace(" 관점에서", "을 볼 때");
        fixed = fixed.replace(" 관점에서도", "도");
        fixed = fixed.replace(" 기준으로도", "도");
        fixed = fixed.replace(" 기준으로", "을 살펴볼 때");
        fixed = fixed.replace("생체정보", "몸의 특징 정보");
        fixed = fixed.replace("설명 가능성을", "이유를 설명할 수 있는지");
        fixed = fixed.replace("설명 가능성은", "이유 설명은");
        fixed = fixed.replace("설명 가능성", "이유 설명");
        fixed = fixed.replace("이의 제기할", "다시 살펴 달라고 말할");
        fixed = fixed.replace("이의 제기 방법", "다시 살펴 달라고 말하는 방법");
        fixed = fixed.replace("이의 제기를", "다시 살펴 달라고 말하는 방법을");
        fixed = fixed.replace("이의 제기", "다시 살펴 달라고 말하기");
        fixed = fixed.replace("대표성을", "자료가 골고루 담겼는지");
        fixed = fixed.replace("대표성은", "자료가 골고루 담겼는지는");
        fixed = fixed.replace("대표성 기준", "자료 골고루 담김 기준");
        fixed = fixed.replace("대표성", "자료 골고루 담김");
        fixed = fixed.replace("최신성을", "지금도 맞는지");
        fixed = fixed.replace("최신성은", "최신 확인은");
        fixed = fixed.replace("최신성", "최신 확인");
        fixed = fixed.replace("추천 알고리즘", "추천 방법");
        fixed = fixed.replace("표본 기준", "자료 모음 기준");
        fixed = fixed.replace("공식 기관", "믿을 만한 기관");
        fixed = fixed.replace("필요 최소", "꼭 필요한 만큼");
        fixed = fixed.replace("꼭 필요한 만큼를", "꼭 필요한 만큼을");
        fixed = fixed.replace("비공개 기준", "다른 사람이 못 보게 하는 기준");
        fixed = fixed.replace("한이 없는지", "한 점이 없는지");
        fixed = fixed.replace("된이 없는지", "된 점이 없는지");
        fixed = fixed.replace("없는이 없는지", "없는 점이 없는지");
        fixed = fixed.replace("  ", " ");
        return fixed.trim();
    }

    private static String fixJosa(String text, Mission mission) {
        String fixed = text;
        fixed = fixJosaFor(fixed, mission.missionTitle);
        for (String value : mission.examples) {
            fixed = fixJosaFor(fixed, value);
        }
        for (String value : mission.concepts) {
            fixed = fixJosaFor(fixed, value);
        }
        for (String value : mission.contexts) {
            fixed = fixJosaFor(fixed, value);
        }
        for (String value : mission.concerns) {
            fixed = fixJosaFor(fixed, value);
        }
        return fixed;
    }

    private static String fixJosaFor(String text, String word) {
        if (word == null || word.isBlank()) {
            return text;
        }
        return text
                .replace(word + "을", word + (hasFinalConsonant(word) ? "을" : "를"))
                .replace(word + "를", word + (hasFinalConsonant(word) ? "을" : "를"))
                .replace(word + "이", word + (hasFinalConsonant(word) ? "이" : "가"))
                .replace(word + "가", word + (hasFinalConsonant(word) ? "이" : "가"));
    }

    private static boolean hasFinalConsonant(String word) {
        for (int i = word.length() - 1; i >= 0; i--) {
            char ch = word.charAt(i);
            if (Character.isWhitespace(ch)) {
                continue;
            }
            if (ch >= 0xAC00 && ch <= 0xD7A3) {
                return ((ch - 0xAC00) % 28) != 0;
            }
            if (Character.isLetterOrDigit(ch)) {
                return false;
            }
        }
        return false;
    }

    private static List<String> tags(Mission mission, String anchor, int no) {
        List<String> result = new ArrayList<>();
        result.add(anchor);
        for (String tag : mission.tags) {
            if (!result.contains(tag)) {
                result.add(tag);
            }
            if (result.size() == 2 + (no % 3 == 0 ? 1 : 0)) {
                break;
            }
        }
        return result;
    }

    private static String conceptFor(Mission mission, int pack, int slot, int no) {
        return pick(mission.concepts, pack, slot, no);
    }

    private static List<String> place(String correct, List<String> distractors, int answerIndex) {
        List<String> options = new ArrayList<>(List.of("", "", "", ""));
        options.set(answerIndex, correct);
        int distractorIndex = 0;
        for (int i = 0; i < options.size(); i++) {
            if (i == answerIndex) {
                continue;
            }
            String candidate = distractors.get(distractorIndex++ % distractors.size());
            int attempts = 0;
            while (options.contains(candidate) && attempts++ < distractors.size()) {
                candidate = distractors.get(distractorIndex++ % distractors.size());
            }
            if (options.contains(candidate)) {
                throw new IllegalStateException("duplicate option candidate: " + candidate);
            }
            options.set(i, candidate);
        }
        return List.copyOf(options);
    }

    private static List<String> varyChoiceOptions(
            List<String> options,
            int answerIndex,
            String context,
            String example,
            int no
    ) {
        List<String> varied = new ArrayList<>();
        for (int index = 0; index < options.size(); index++) {
            varied.add(choiceOptionVariant(options.get(index), context, example, no, index));
        }
        return ensureDistinct(varied, answerIndex, context, example);
    }

    private static List<String> varyFillOptions(
            List<String> options,
            int answerIndex,
            String context,
            String example,
            int no
    ) {
        List<String> varied = new ArrayList<>();
        for (int index = 0; index < options.size(); index++) {
            String option = options.get(index);
            varied.add(index == answerIndex ? option : fillOptionVariant(option, context, example, no, index));
        }
        return ensureDistinct(varied, answerIndex, context, example);
    }

    private static List<String> ensureDistinct(
            List<String> options,
            int answerIndex,
            String context,
            String example
    ) {
        List<String> result = new ArrayList<>();
        for (int index = 0; index < options.size(); index++) {
            String option = options.get(index);
            if (index != answerIndex) {
                int suffix = 0;
                while (result.contains(option)) {
                    option = fillOptionVariant(option, context, example, suffix, index + suffix + 1);
                    suffix++;
                    if (suffix > 8 && result.contains(option)) {
                        option = option + " " + (suffix + 1);
                    }
                }
            }
            result.add(option);
        }
        return List.copyOf(result);
    }

    private static String choiceOptionVariant(String option, String context, String example, int no, int index) {
        return switch (option) {
            case "친구 얼굴 사진을 허락 없이 올려요." -> pickVariant(no, index,
                    "친구 사진을 묻지 않고 올려요.",
                    "얼굴이 보이는 사진을 바로 공유해요.",
                    "허락 없이 친구 사진을 앱에 넣어요.",
                    "친구 얼굴이 보이는 자료를 그대로 올려요.");
            case "틀린 이름표를 그대로 둬요." -> pickVariant(no, index,
                    "맞지 않는 이름표를 고치지 않아요.",
                    "틀린 정답 표시를 그냥 둬요.",
                    "이름표가 이상해도 다시 보지 않아요.",
                    "잘못 붙은 이름표를 확인하지 않아요.");
            case "전기가 들어가면 모두 AI라고 정해요." -> pickVariant(no, index,
                    "전원을 켜는 도구는 모두 AI라고 해요.",
                    "전자기기라는 이유만으로 AI라고 말해요.",
                    "불이 켜지면 AI 기능이라고 정해요.",
                    "전기로 움직이면 모두 AI라고 생각해요.");
            case "계산을 빠르게 하면 모두 AI라고 말해요." -> pickVariant(no, index,
                    "계산이 빠르면 모두 AI라고 말해요.",
                    "빠른 계산만 보고 AI라고 정해요.",
                    "계산 결과가 나오면 AI 판단이라고 해요.",
                    "계산 속도만 보고 AI인지 결정해요.");
            case "비슷한 사진 한 종류만 모아 전체를 대표한다고 해요." -> pickVariant(no, index,
                    "비슷한 사진만 보고 전체를 알 수 있다고 해요.",
                    "한 종류의 예시만으로 충분하다고 말해요.",
                    "비슷한 자료만 모아 전체처럼 발표해요.",
                    "한쪽 예시만 보고 결론을 내요.");
            case "AI가 알아본 이름을 무조건 사실로 발표해요." -> pickVariant(no, index,
                    "AI가 붙인 이름을 바로 사실로 말해요.",
                    "인식 결과를 다시 보지 않고 발표해요.",
                    "AI가 맞혔다고 생각하고 그대로 써요.",
                    "잘못 알아본 가능성을 보지 않아요.");
            case "AI가 자신 있게 말하면 그대로 제출해요." -> pickVariant(no, index,
                    "AI 답이 자연스러우면 바로 제출해요.",
                    "AI가 길게 답하면 그대로 믿어요.",
                    "AI 답을 읽어 보지 않고 제출해요.",
                    "AI 말투가 자신 있어 보이면 바로 써요.");
            case "마음에 안 들어도 그대로 제출해요." -> pickVariant(no, index,
                    "답이 맞지 않아도 그대로 냅니다.",
                    "이상한 부분이 보여도 고치지 않아요.",
                    "내 목적과 달라도 그냥 제출해요.",
                    "고칠 점을 보지 않고 그대로 써요.");
            case "AI 답을 읽지 않고 그대로 제출해요." -> pickVariant(no, index,
                    "AI 답을 확인하지 않고 냅니다.",
                    "내용을 모른 채 그대로 제출해요.",
                    "읽어 보지 않은 답을 내 글처럼 냅니다.",
                    "AI 답을 내 말로 바꾸지 않아요.");
            case "AI가 정했으니 아무도 질문하면 안 된다고 해요." -> pickVariant(no, index,
                    "AI 결과라서 다시 물으면 안 된다고 해요.",
                    "AI가 정하면 친구 의견은 필요 없다고 해요.",
                    "AI 결정에는 질문할 수 없다고 말해요.",
                    "AI 결과를 모두가 그냥 따라야 한다고 해요.");
            case "아무 설명 없이 '해줘'라고만 써요." -> pickVariant(no, index,
                    "무엇을 원하는지 말하지 않고 요청해요.",
                    "조건 없이 짧게만 물어봐요.",
                    "목적을 쓰지 않고 AI에게 맡겨요.",
                    "필요한 내용을 설명하지 않아요.");
            case "인터넷 사진을 출처 없이 모두 가져와요." -> pickVariant(no, index,
                    "인터넷 사진의 출처를 적지 않아요.",
                    "누가 만든 사진인지 보지 않고 써요.",
                    "허락과 출처 없이 사진을 가져와요.",
                    "사진이 어디서 왔는지 지워요.");
            case "AI 답이 길면 바로 사실로 믿어요." -> pickVariant(no, index,
                    "답이 길다는 이유만으로 믿어요.",
                    "설명이 많으면 맞다고 생각해요.",
                    "긴 답을 다른 자료와 비교하지 않아요.",
                    "글이 길면 검토하지 않고 사용해요.");
            case "더 정확한 답을 위해 실제 주소를 넣어요." -> pickVariant(no, index,
                    "정확하게 하려고 집 주소를 넣어요.",
                    "실제 주소를 AI 질문에 그대로 써요.",
                    "주소를 넣으면 더 좋다고 생각해요.",
                    "개인 주소를 지우지 않고 질문해요.");
            case "제목이 자극적이면 바로 믿어요." -> pickVariant(no, index,
                    "제목만 보고 사실이라고 믿어요.",
                    "눈에 띄는 제목이면 바로 사용해요.",
                    "강한 제목을 믿을 만한 자료로 봐요.",
                    "제목이 재미있으면 확인하지 않아요.");
            case "첫 결과가 틀려도 이유를 찾지 않아요." -> pickVariant(no, index,
                    "틀린 결과가 나와도 원인을 보지 않아요.",
                    "처음 답이 이상해도 그냥 넘어가요.",
                    "오류가 보여도 다시 시험하지 않아요.",
                    "왜 틀렸는지 살펴보지 않아요.");
            case "한 반 의견을 학교 전체 의견이라고 말해요." -> pickVariant(no, index,
                    "한 반의 답만 보고 학교 전체라고 해요.",
                    "일부 의견을 모두의 의견처럼 말해요.",
                    "작은 설문으로 전체 결론을 냅니다.",
                    "빠진 친구 의견을 보지 않고 발표해요.");
            case "편리하면 걱정은 모두 무시해요." -> pickVariant(no, index,
                    "편하다는 이유로 조심할 점을 보지 않아요.",
                    "쓰기 쉬우면 위험한 점은 넘겨요.",
                    "편리하니 걱정할 필요가 없다고 해요.",
                    "좋아 보이면 안전 문제를 살피지 않아요.",
                    "도움이 되면 책임은 생각하지 않아요.",
                    "빠르게 끝나면 확인하지 않아도 된다고 해요.");
            case "한 종류의 사진만 많이 모아요." -> pickVariant(no, index,
                    "비슷한 사진만 잔뜩 모아요.",
                    "한쪽 예시만 계속 넣어요.",
                    "다른 경우는 빼고 같은 사진만 모아요.",
                    "한 종류 자료만 충분하다고 생각해요.",
                    "다양한 예시는 보지 않고 모아요.",
                    "비슷한 모습의 사진만 골라요.");
            case "개인정보를 더 넣어 정확하게 만들려고 해요." -> pickVariant(no, index,
                    "정확하게 하려고 개인정보를 더 적어요.",
                    "이름과 주소를 넣으면 더 좋다고 생각해요.",
                    "개인정보를 많이 넣어 답을 받으려 해요.",
                    "필요 없는 개인정보까지 질문에 넣어요.",
                    "친구 정보도 넣으면 정확하다고 말해요.",
                    "나를 알아볼 정보까지 AI에게 알려요.");
            case "출처가 없어도 답이 길면 믿어요." -> pickVariant(no, index,
                    "출처가 없어도 설명이 길면 믿어요.",
                    "긴 답이라면 확인하지 않아도 된다고 해요.",
                    "자료 출처 없이도 자세하면 맞다고 해요.",
                    "글이 길면 믿을 만하다고 생각해요.",
                    "출처보다 답의 길이를 더 믿어요.",
                    "길게 쓴 답은 바로 사용해요.");
            case "누가 썼는지 보지 않아요." -> pickVariant(no, index,
                    "쓴 사람을 확인하지 않아요.",
                    "만든 곳을 보지 않고 써요.",
                    "누가 만든 자료인지 넘겨요.",
                    "자료의 만든 사람을 살피지 않아요.",
                    "글쓴이를 확인하지 않고 믿어요.",
                    "자료가 어디서 왔는지 보지 않아요.");
            case "불리한 친구가 있어도 기준을 숨겨요." -> pickVariant(no, index,
                    "불리한 친구가 있어도 살펴볼 점을 숨겨요.",
                    "누가 불편한지 보지 않고 넘어가요.",
                    "억울한 친구가 있어도 이유를 말하지 않아요.",
                    "결정 이유를 친구들에게 알려 주지 않아요.",
                    "불리한 결과가 나와도 설명하지 않아요.",
                    "모두에게 괜찮은지 묻지 않아요.");
            case "한쪽 사진만 모아도 공정하다고 해요." -> pickVariant(no, index,
                    "한쪽 사진만 보고도 공정하다고 말해요.",
                    "비슷한 사진만 있어도 괜찮다고 해요.",
                    "빠진 예시가 있어도 문제없다고 해요.",
                    "한 종류 자료만으로 공정하다고 해요.",
                    "다른 모습의 사진은 필요 없다고 해요.",
                    "한쪽으로 모인 자료를 그대로 써요.");
            case "AI가 쓴 글을 전부 내 생각이라고 말해요." -> pickVariant(no, index,
                    "AI가 쓴 글을 모두 내가 썼다고 해요.",
                    "도움을 받았다는 사실을 숨겨요.",
                    "AI 문장을 그대로 내 생각처럼 말해요.",
                    "AI 도움을 표시하지 않고 제출해요.",
                    "AI 글을 읽지 않고 내 글로 냅니다.",
                    "AI가 만든 내용을 전부 베껴요.");
            case "AI가 해 주면 내 판단은 필요 없다고 해요." -> pickVariant(no, index,
                    "AI가 했으니 나는 생각하지 않아도 된다고 해요.",
                    "AI 결정이면 그대로 따르면 된다고 해요.",
                    "내가 다시 살필 필요가 없다고 말해요.",
                    "AI에게 맡기고 책임을 보지 않아요.",
                    "AI 답이면 내 선택은 필요 없다고 해요.",
                    "사람이 확인하지 않아도 된다고 해요.");
            case "친구의 이름과 연락처를 넣어 질문해요." -> pickVariant(no, index,
                    "친구 이름과 전화번호를 질문에 넣어요.",
                    "친구 연락처를 지우지 않고 AI에게 물어요.",
                    "친구를 알아볼 정보를 그대로 적어요.",
                    "다른 사람 개인정보를 질문에 넣어요.",
                    "친구 정보까지 AI에게 알려요.",
                    "친구의 실제 정보를 예시로 써요.");
            case "틀린 예시는 숨기고 맞은 것만 발표해요." -> pickVariant(no, index,
                    "틀린 결과는 빼고 맞은 것만 보여 줘요.",
                    "실패한 예시는 감추고 발표해요.",
                    "잘 안 된 경우를 기록하지 않아요.",
                    "맞힌 결과만 모아 좋은 것처럼 말해요.",
                    "오류 사례를 숨기고 넘어가요.",
                    "틀린 이유를 찾지 않고 빼 버려요.");
            case "날짜가 중요한데도 확인하지 않아요." -> pickVariant(no, index,
                    "날짜가 필요한 자료인데도 보지 않아요.",
                    "오래된 정보인지 확인하지 않아요.",
                    "언제 만든 자료인지 넘겨요.",
                    "최신 정보가 필요한데 날짜를 보지 않아요.",
                    "자료의 만든 때를 살피지 않아요.",
                    "시간이 중요한 답도 그냥 믿어요.");
            case "자동으로 움직이면 무조건 학습한 AI라고 정해요." -> pickVariant(no, index,
                    "자동으로 움직인다고 모두 학습한 AI라고 해요.",
                    "스스로 켜지면 AI가 배운 것이라고 말해요.",
                    "자동 기능만 보고 AI 학습이라고 정해요.",
                    "움직임만 보고 AI인지 판단해요.",
                    "정해진 자동 작동도 AI 학습이라고 해요.",
                    "자동이면 무조건 AI가 배운 결과라고 해요.");
            case "이름이 멋지면 AI라고 믿어요." -> pickVariant(no, index,
                    "이름만 멋지면 AI 기능이라고 믿어요.",
                    "광고 문구만 보고 AI라고 정해요.",
                    "멋진 이름이면 원리를 보지 않아요.",
                    "이름에 AI가 들어가면 바로 믿어요.",
                    "포장된 이름만 보고 판단해요.",
                    "기능 설명보다 이름을 더 믿어요.");
            case "친구 글을 허락 없이 AI에 넣어요." -> pickVariant(no, index,
                    "친구 글을 묻지 않고 AI에게 넣어요.",
                    "다른 사람 글을 허락 없이 질문에 붙여요.",
                    "친구의 글을 그대로 AI에 사용해요.",
                    "내 글이 아닌데 허락 없이 넣어요.",
                    "친구가 쓴 내용을 마음대로 올려요.",
                    "남의 글을 출처 없이 AI에 넣어요.");
            case "목적을 숨기고 긴 답만 요구해요." -> pickVariant(no, index,
                    "무엇을 원하는지 숨기고 길게만 써 달라고 해요.",
                    "목적 없이 긴 답을 달라고 해요.",
                    "필요한 조건은 빼고 분량만 요구해요.",
                    "왜 필요한지 말하지 않고 길게 써 달라고 해요.",
                    "질문의 목적을 적지 않고 많이 써 달라고 해요.",
                    "조건 없이 긴 답이면 된다고 해요.");
            case "아무 조건 없이 더 길게만 써 달라고 해요." -> pickVariant(no, index,
                    example + " 조건은 빼고 길게만 써 달라고 해요.",
                    contextAt(context) + " 무엇을 원하는지 말하지 않고 길이만 늘려요.",
                    example + "에 필요한 조건 없이 더 많이 써 달라고 해요.",
                    contextAt(context) + " 목적을 말하지 않고 긴 답만 요구해요.",
                    example + " 대상과 형식 없이 길게만 다시 물어요.",
                    contextAt(context) + " 원하는 점은 빼고 분량만 늘려 달라고 해요.",
                    example + " 조건 없이 길게만 써 달라고 해요.",
                    contextAt(context) + " 목적 없이 긴 답만 요구해요.",
                    example + " 답의 대상과 형식을 말하지 않아요.",
                    contextAt(context) + " 필요한 조건을 빼고 다시 물어요.");
            case "어떤 자료를 보고 말했는지 물어봐요." -> pickVariant(no, index,
                    "어떤 자료를 바탕으로 답했는지 물어봐요.",
                    "답의 근거가 된 자료를 확인해요.",
                    "무엇을 보고 말했는지 다시 물어요.",
                    "참고한 자료와 이유를 물어봐요.",
                    "답을 만든 자료가 무엇인지 확인해요.",
                    "근거로 쓴 자료를 알려 달라고 해요.");
            case "발표할 자료가 믿을 만한지 먼저 확인해요." -> pickVariant(no, index,
                    "발표 자료가 믿을 만한지 먼저 봐요.",
                    "발표에 쓸 자료의 출처를 확인해요.",
                    "사례 자료가 맞는지 발표 전에 살펴요.",
                    "발표 자료의 만든 곳을 확인해요.",
                    "발표할 내용이 사실인지 먼저 비교해요.",
                    "자료를 발표에 넣기 전에 확인해요.");
            case "좋은 점과 조심할 점을 같이 설명해요." -> pickVariant(no, index,
                    "좋은 점과 걱정되는 점을 함께 말해요.",
                    "도움 되는 점과 조심할 점을 같이 적어요.",
                    "편리한 점만 말하지 않고 걱정도 설명해요.",
                    "장점과 주의할 점을 나란히 발표해요.",
                    "좋은 점 뒤의 위험도 함께 살펴요.",
                    "도움과 걱정을 함께 정리해요.");
            case "왜 쓰는지와 어떤 자료가 들어가는지 살펴요." -> pickVariant(no, index,
                    "왜 쓰는지와 넣을 자료를 먼저 봐요.",
                    "사용 목적과 들어갈 자료를 확인해요.",
                    "무엇에 쓰는지와 자료 종류를 살펴요.",
                    "쓸 이유와 필요한 자료를 먼저 정해요.",
                    "사용 목적과 개인정보 여부를 확인해요.",
                    "어떤 자료가 필요한지 먼저 살펴요.");
            case "안전하게 쓸 수 있는지 체크해요." -> pickVariant(no, index,
                    "안전하게 쓸 수 있는지 먼저 확인해요.",
                    "사용 전에 위험한 점이 없는지 살펴요.",
                    "개인정보와 출처를 확인하고 사용해요.",
                    "조심할 점을 체크한 뒤 사용해요.",
                    "안전하게 쓸 조건을 먼저 봐요.",
                    "문제가 생길 만한 점을 살펴요.");
            case "질문 조건이 달랐는지 먼저 살펴봐요." -> pickVariant(no, index,
                    "두 질문의 조건이 달랐는지 확인해요.",
                    "처음 물은 조건을 나란히 비교해요.",
                    "서로 다른 조건 때문에 답이 달랐는지 봐요.",
                    "질문에 넣은 조건을 먼저 비교해요.",
                    "답보다 질문 조건부터 살펴봐요.",
                    "조건 차이를 찾고 다시 질문해요.");
            default -> option;
        };
    }

    private static String fillOptionVariant(String option, String context, String example, int no, int index) {
        return switch (option) {
            case "색깔" -> pickVariant(no, index, "색깔", "겉모습", "무늬", "모양");
            case "장식" -> pickVariant(no, index, "장식", "꾸미기", "겉치장", "포장", "꾸밈", "겉포장", "그림 장식", "보기 좋게 꾸미기");
            case "광고" -> pickVariant(no, index, "광고", "홍보 글", "선전", "상품 소개");
            case "속도" -> pickVariant(no, index, "속도", "빠르기", "걸린 시간", "움직임");
            case "소문" -> pickVariant(no, index, "소문", "들은 말", "뜬소문", "친구 말");
            case "비밀번호" -> pickVariant(no, index, "비밀번호", "암호", "잠금번호", "비밀 숫자");
            case "암호" -> pickVariant(no, index, "암호", "비밀번호", "잠금말", "비밀 표시");
            case "간식" -> pickVariant(no, index, "간식", "과자", "먹을 것", "음식");
            case "날씨" -> pickVariant(no, index, "날씨", "기온", "하늘 상태", "비 소식");
            case "가격" -> pickVariant(no, index, "가격", "값", "비용", "금액");
            case "삭제" -> pickVariant(no, index, "삭제", "지우기", "없애기", "버리기");
            case "복사" -> pickVariant(no, index, "복사", "따라 쓰기", "베껴 쓰기", "옮겨 쓰기");
            case "숨김" -> pickVariant(no, index, "숨김", "감추기", "덮기", "가리기");
            case "비밀" -> pickVariant(no, index, "비밀", "숨긴 말", "몰래 하기", "잠금");
            default -> option;
        };
    }

    private static String pickVariant(int no, int index, String... variants) {
        return variants[Math.floorMod(no * 31 + index * 17, variants.length)];
    }

    private static List<String> distinctPicks(
            String[] values,
            String excluded,
            int pack,
            int slot,
            int no,
            int count
    ) {
        List<String> result = new ArrayList<>();
        for (int offset = 0; result.size() < count && offset < values.length * 4; offset++) {
            String candidate = pick(values, pack + offset, slot + offset * 2, no + offset * 3);
            if (!candidate.equals(excluded) && !result.contains(candidate)) {
                result.add(candidate);
            }
        }
        for (String candidate : values) {
            if (result.size() >= count) {
                break;
            }
            if (!candidate.equals(excluded) && !result.contains(candidate)) {
                result.add(candidate);
            }
        }
        if (result.size() != count) {
            throw new IllegalStateException("not enough distinct options");
        }
        return List.copyOf(result);
    }

    private static <T> T pick(T[] values, int pack, int slot, int no) {
        return values[Math.floorMod(pack * 13 + slot * 17 + no * 19 + slot * no, values.length)];
    }

    private static void validate(List<Mission> missions, List<Question> questions) {
        if (missions.size() != 16) {
            throw new IllegalStateException("mission count mismatch: " + missions.size());
        }
        if (questions.size() != 1056) {
            throw new IllegalStateException("question count mismatch: " + questions.size());
        }
        Map<String, Integer> ids = new LinkedHashMap<>();
        Map<String, Integer> promptByMission = new LinkedHashMap<>();
        Map<String, Integer> globalPrompt = new LinkedHashMap<>();
        Map<String, Integer> difficulty = new LinkedHashMap<>();
        Map<String, Integer> type = new LinkedHashMap<>();
        Map<Integer, Integer> pack = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> missionDifficulty = new LinkedHashMap<>();

        for (Question question : questions) {
            ids.merge(question.externalId, 1, Integer::sum);
            promptByMission.merge(question.missionCode + "|" + question.question, 1, Integer::sum);
            globalPrompt.merge(normalizedPrompt(question.question), 1, Integer::sum);
            difficulty.merge(question.difficulty, 1, Integer::sum);
            type.merge(question.type, 1, Integer::sum);
            pack.merge(question.packNo, 1, Integer::sum);
            missionDifficulty.computeIfAbsent(question.missionCode, ignored -> new LinkedHashMap<>())
                    .merge(question.difficulty, 1, Integer::sum);

            if ("OX".equals(question.type)) {
                if (question.options != null || !(question.answer instanceof Boolean)) {
                    throw new IllegalStateException("bad OX: " + question.externalId);
                }
            } else if ("FILL".equals(question.type)) {
                if (question.options == null || question.options.size() != 4 || !(question.answer instanceof List<?>)) {
                    throw new IllegalStateException("bad FILL: " + question.externalId);
                }
                if (new LinkedHashSet<>(question.options).size() != question.options.size()) {
                    throw new IllegalStateException("duplicate options: " + question.externalId);
                }
            } else {
                if (question.options == null || question.options.size() != 4 || !(question.answer instanceof Integer)) {
                    throw new IllegalStateException("bad " + question.type + ": " + question.externalId);
                }
                if (new LinkedHashSet<>(question.options).size() != question.options.size()) {
                    throw new IllegalStateException("duplicate options: " + question.externalId);
                }
            }
            assertStudentFacingText(question);
        }

        ids.forEach((id, count) -> {
            if (count != 1) {
                throw new IllegalStateException("duplicate id: " + id);
            }
        });
        promptByMission.forEach((prompt, count) -> {
            if (count != 1) {
                throw new IllegalStateException("duplicate prompt: " + prompt);
            }
        });
        globalPrompt.forEach((prompt, count) -> {
            if (count != 1) {
                throw new IllegalStateException("global duplicate prompt: " + prompt);
            }
        });
        assertCount(difficulty, "LOW", 480);
        assertCount(difficulty, "MEDIUM", 320);
        assertCount(difficulty, "HIGH", 256);
        assertCount(type, "OX", 208);
        assertCount(type, "MULTIPLE", 320);
        assertCount(type, "FILL", 208);
        assertCount(type, "SITUATION", 320);
        assertCount(pack, 1, 160);
        assertCount(pack, 2, 160);
        assertCount(pack, 3, 160);
        assertCount(pack, 4, 160);
        assertCount(pack, 5, 160);
        assertCount(pack, 6, 256);

        missionDifficulty.forEach((mission, counts) -> {
            assertCount(counts, "LOW", 30);
            assertCount(counts, "MEDIUM", 20);
            assertCount(counts, "HIGH", 16);
        });
        assertSimilarityBelow(questions, 0.85d);
    }

    private static <T> void assertCount(Map<T, Integer> counts, T key, int expected) {
        int actual = counts.getOrDefault(key, 0);
        if (actual != expected) {
            throw new IllegalStateException("count mismatch for " + key + ": " + actual + " != " + expected);
        }
    }

    private static String normalizedPrompt(String text) {
        return text == null ? "" : text.toLowerCase()
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static void assertStudentFacingText(Question question) {
        if (question.question.length() > 90) {
            throw new IllegalStateException("question too long: " + question.externalId + " " + question.question.length());
        }
        if (question.explanation.length() > 90) {
            throw new IllegalStateException("explanation too long: " + question.externalId + " " + question.explanation.length());
        }
        String text = question.question + " " + question.explanation + " "
                + (question.options == null ? "" : String.join(" ", question.options));
        List<String> blocked = List.of(
                "때에서",
                "새 AI 기능인",
                "이 상황은",
                "문항이에요",
                "AI 답이 마음에 들지 않아요",
                "확인을 확인",
                "답 AI 답",
                "답 답",
                "개인 정보",
                "한이 없는지",
                "된이 없는지",
                "없는이 없는지",
                "을/를",
                "이/가",
                "은/는"
        );
        for (String pattern : blocked) {
            if (text.contains(pattern)) {
                throw new IllegalStateException("student-facing text gate failed: " + question.externalId + " " + pattern);
            }
        }
    }

    private static void assertSimilarityBelow(List<Question> questions, double threshold) {
        Map<String, List<Question>> byMission = new LinkedHashMap<>();
        for (Question question : questions) {
            byMission.computeIfAbsent(question.missionCode, ignored -> new ArrayList<>()).add(question);
        }
        for (Map.Entry<String, List<Question>> entry : byMission.entrySet()) {
            List<Question> missionQuestions = entry.getValue();
            for (int left = 0; left < missionQuestions.size(); left++) {
                for (int right = left + 1; right < missionQuestions.size(); right++) {
                    Question leftQuestion = missionQuestions.get(left);
                    Question rightQuestion = missionQuestions.get(right);
                    double score = Math.max(
                            tokenJaccard(leftQuestion.question, rightQuestion.question),
                            ngramDice(leftQuestion.question, rightQuestion.question)
                    );
                    if (score >= threshold) {
                        throw new IllegalStateException("similarity gate failed: "
                                + leftQuestion.externalId + " vs " + rightQuestion.externalId + " = " + score
                                + " | " + leftQuestion.question + " | " + rightQuestion.question);
                    }
                }
            }
        }
    }

    private static double tokenJaccard(String left, String right) {
        Set<String> leftTokens = tokenSet(left);
        Set<String> rightTokens = tokenSet(right);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0d;
        }
        Set<String> intersection = new LinkedHashSet<>(leftTokens);
        intersection.retainAll(rightTokens);
        Set<String> union = new LinkedHashSet<>(leftTokens);
        union.addAll(rightTokens);
        return union.isEmpty() ? 0d : intersection.size() / (double) union.size();
    }

    private static Set<String> tokenSet(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        for (String raw : normalizedPrompt(text).split("\\s+")) {
            String token = raw.replaceAll(
                    "(에게서|으로도|에서도|에서는|에게|처럼|보다|부터|까지|으로|에서|은|는|이|가|을|를|와|과|도|만|로|에|의)$",
                    ""
            );
            if (token.length() >= 2 && !SIMILARITY_STOP_WORDS.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private static double ngramDice(String left, String right) {
        Set<String> leftNgrams = ngrams(left);
        Set<String> rightNgrams = ngrams(right);
        if (leftNgrams.isEmpty() || rightNgrams.isEmpty()) {
            return 0d;
        }
        Set<String> intersection = new LinkedHashSet<>(leftNgrams);
        intersection.retainAll(rightNgrams);
        return (2d * intersection.size()) / (leftNgrams.size() + rightNgrams.size());
    }

    private static Set<String> ngrams(String text) {
        Set<String> ngrams = new LinkedHashSet<>();
        String compact = normalizedPrompt(text).replace(" ", "");
        if (compact.isBlank()) {
            return ngrams;
        }
        if (compact.length() <= 3) {
            ngrams.add(compact);
            return ngrams;
        }
        for (int index = 0; index <= compact.length() - 3; index++) {
            ngrams.add(compact.substring(index, index + 3));
        }
        return ngrams;
    }

    private static List<Mission> missions() {
        return List.of(
                mission(
                        "S0101", 1, "AI 알아보기", "우리 주변의 AI 찾기",
                        "생활 속 도구에서 AI가 하는 일을 기능 중심으로 찾아봅니다.",
                        "KERIS 초등 AI 교육 생활 속 인공지능; 디지털 리터러시 정보 이해",
                        "초등 교사를 위한 KERIS와 시작하는 인공지능 교육 1.pdf; 2021년 인공지능(AI)기본 역량 강화 연수 교재(초등).pdf",
                        tags("FACT", "VERIFICATION", "SAFETY"),
                        examples("번역 앱", "스마트 스피커", "카메라 꽃 이름 찾기", "스팸 메일 분류", "영상 추천", "음성 받아쓰기", "길 찾기 추천", "그림 인식 게임"),
                        concepts("기능 중심 판단", "생활 속 AI", "추천과 분류", "말과 글 인식", "사진 인식", "자동화와 AI 구분", "데이터 활용", "AI의 도움"),
                        contexts("교실", "집", "도서관", "온라인 숙제", "모둠 발표", "현장 체험", "방과 후 활동", "가족 대화"),
                        good("무엇을 보고 판단하는 기능인지 살펴봐요.", "사람의 말이나 사진을 바탕으로 추측하는지 확인해요.", "단순 전자기기와 AI 기능을 나누어 봐요.", "AI가 어떤 도움을 주는지 예로 설명해요.", "추천 결과가 왜 나왔는지 생각해 봐요.", "필요한 때에만 AI 기능을 사용해요.", "겉모습보다 하는 일을 먼저 봐요.", "AI가 아닌 기능도 함께 구별해요."),
                        wrong("전기가 들어가면 모두 AI라고 정해요.", "이름이 멋지면 AI라고 믿어요.", "기능을 보지 않고 모양만 보고 판단해요.", "추천 결과를 모두 정답처럼 받아들여요.", "친구의 말을 확인하지 않고 그대로 따라 해요.", "AI와 자동 타이머를 같은 것으로 봐요.", "광고 문구만 보고 AI 여부를 정해요.", "한 번 써 보고 모든 상황에 맞다고 말해요."),
                        concerns("기능을 잘못 이해한", "추천 이유를 모르는", "자동화와 AI를 섞어 생각한", "AI 도움을 과하게 믿는", "개인정보 입력을 놓친"),
                        trueClaims("AI는 사진, 말, 글 같은 자료를 바탕으로 판단할 수 있어요.", "번역 앱이나 음성 인식처럼 생활 속에도 AI 기능이 있어요.", "AI인지 보려면 겉모습보다 하는 일을 살펴야 해요.", "추천 기능은 사용 기록 같은 데이터를 활용할 수 있어요.", "모든 전자기기가 AI인 것은 아니라서 구별이 필요해요.", "AI 기능은 사람을 돕지만 항상 완벽하지는 않아요.", "카메라 앱도 대상을 알아보는 AI 기능을 쓸 수 있어요.", "AI가 한 일을 설명할 때는 어떤 자료를 보았는지 떠올리면 좋아요."),
                        falseClaims("계산기처럼 정해진 계산만 하는 도구는 언제나 AI예요.", "AI라는 글자가 붙으면 결과를 확인하지 않아도 돼요.", "스마트폰에 있는 모든 앱은 같은 방식의 AI예요.", "AI는 사람처럼 직접 경험하고 마음으로 판단해요.", "추천 영상은 나와 상관없이 항상 공정하게 골라져요.", "AI 기능은 인터넷에 연결되어 있으면 절대 틀리지 않아요.", "전원을 켜는 버튼도 스스로 배우는 AI 기능이에요.", "생활 속 AI를 찾을 때 개인정보 걱정은 전혀 필요 없어요."),
                        fill("AI 기능을 찾을 때는 겉모습보다 ____을 먼저 살펴요.", "기능", "가격", "색깔", "크기", "AI인지 판단할 때는 무엇을 해 주는지 살피는 것이 좋아요."),
                        fill("{example}처럼 사용자의 자료를 보고 알맞은 답을 고르는 기능은 ____과 관련이 있어요.", "AI", "종이", "전선", "자석", "AI는 자료를 바탕으로 분류하거나 추천하는 데 쓰일 수 있어요."),
                        fill("AI가 추천한 내용은 바로 믿기보다 ____해 보는 태도가 필요해요.", "확인", "복사", "삭제", "숨기기", "추천 결과도 틀리거나 한쪽으로 치우칠 수 있어요."),
                        fill("모든 전자기기가 AI는 아니므로 ____을 구별해야 해요.", "기능", "포장", "소리", "무게", "어떤 기능을 하는지 보면 AI 여부를 더 잘 판단할 수 있어요."),
                        fill("{context}에서 AI 도움을 쓸 때는 필요한 ____만 사용하는 것이 좋아요.", "기능", "비밀번호", "주소", "사진 전체", "필요한 기능만 쓰면 안전하고 목적에 맞게 사용할 수 있어요.")
                ),
                mission(
                        "S0102", 1, "AI 알아보기", "AI와 일반 프로그램 구분하기",
                        "정해진 규칙을 따르는 프로그램과 데이터를 보고 배우는 AI를 비교합니다.",
                        "KERIS 초등 AI 교육 규칙 기반과 학습 기반; 디지털 리터러시 판단",
                        "초등 교사를 위한 KERIS와 시작하는 인공지능 교육 1.pdf; 2021년 인공지능(AI)기본 역량 강화 연수 교재(초등).pdf",
                        tags("FACT", "VERIFICATION"),
                        examples("계산기", "알람 시계", "사진 분류 앱", "손글씨 인식", "음식 추천 앱", "온도 조절기", "규칙 카드 게임", "동물 사진 분류"),
                        concepts("규칙 기반", "학습 기반", "계산기와 AI 차이", "예시 데이터", "분류 기준", "자동화", "테스트", "판단 방식"),
                        contexts("수학 시간", "과학 시간", "방과 후 코딩 활동", "동아리 발표", "집에서 숙제할 때", "태블릿 사용 시간", "모둠 토의", "온라인 학습"),
                        good("정해진 계산인지 예시를 보고 배우는지 나누어 봐요.", "새 사진에도 판단을 시도하는지 살펴봐요.", "규칙을 사람이 모두 정했는지 확인해요.", "데이터로 배운 기능인지 설명해 봐요.", "틀린 결과가 나올 수 있음을 함께 생각해요.", "계산 결과와 분류 결과를 구별해요.", "테스트 자료로 다시 확인해요.", "도구의 판단 방식을 말로 정리해요."),
                        wrong("계산을 빠르게 하면 모두 AI라고 말해요.", "자동으로 움직이면 무조건 학습한 AI라고 정해요.", "새 자료에서 틀려도 확인하지 않아요.", "사람이 만든 규칙과 데이터 학습을 구별하지 않아요.", "도구 이름만 보고 AI인지 결정해요.", "계산기 결과와 추천 결과를 똑같이 믿어요.", "예시 없이도 AI가 저절로 배운다고 생각해요.", "한 번 맞히면 항상 맞는다고 말해요."),
                        concerns("규칙과 학습을 섞어 생각한", "계산 결과를 AI 판단처럼 본", "테스트 없이 결론 낸", "자동화만 보고 AI라고 한", "학습 자료를 확인하지 않은"),
                        trueClaims("계산기는 보통 사람이 정한 계산 규칙을 그대로 따라요.", "AI는 여러 예시 데이터를 보고 비슷한 새 자료를 판단할 수 있어요.", "자동으로 움직인다고 모두 AI인 것은 아니에요.", "학습 기반 AI도 처음 보는 자료에서는 틀릴 수 있어요.", "규칙 기반 프로그램은 정해진 조건에 따라 결과를 냅니다.", "AI와 계산기를 비교할 때 판단 방식이 중요한 기준이 돼요.", "사진 분류 앱은 예시 사진을 보고 배운 기능을 쓸 수 있어요.", "테스트는 배운 기능이 새 자료에도 맞는지 확인하는 과정이에요."),
                        falseClaims("계산기는 스스로 예시를 모아 새 규칙을 만들어요.", "정해진 알람 시간이 울리는 것은 데이터로 배운 AI 판단이에요.", "AI는 한 번 배우면 어떤 자료든 절대 틀리지 않아요.", "사람이 규칙을 정한 프로그램은 모두 학습 기반 AI예요.", "빠른 계산은 언제나 사람처럼 생각했다는 뜻이에요.", "새 자료로 시험하지 않아도 AI가 잘 배웠는지 알 수 있어요.", "자동문은 지나간 사람을 모두 공부해서 판단하므로 항상 AI예요.", "계산기의 정답과 AI의 추천은 확인 방법이 완전히 같아요."),
                        fill("계산기는 보통 사람이 정한 ____을 그대로 따라요.", "규칙", "기분", "소문", "취향", "계산기는 정해진 규칙을 빠르게 실행하는 도구에 가까워요."),
                        fill("AI는 여러 ____을 보고 비슷한 새 자료를 판단할 수 있어요.", "예시", "비밀번호", "장식", "소문", "AI는 예시 데이터를 통해 패턴을 배울 수 있어요."),
                        fill("AI와 일반 프로그램을 구분할 때는 결과보다 ____을 살펴요.", "방식", "색깔", "가격", "글씨체", "어떤 방식으로 판단하는지 보는 것이 중요해요."),
                        fill("AI가 배운 뒤에는 새 자료로 ____해 보아야 해요.", "테스트", "감추기", "꾸미기", "복사", "테스트는 배운 내용이 새 자료에도 맞는지 확인하는 과정이에요."),
                        fill("{example}이 정해진 조건만 따르는지, 자료로 ____했는지 비교해요.", "학습", "휴식", "포장", "충전", "자료로 배웠는지 보면 AI 기능인지 판단하는 데 도움이 돼요.")
                ),
                mission(
                        "S0103", 1, "AI 알아보기", "AI는 데이터를 보고 배워요",
                        "데이터, 이름표, 다양성, 테스트가 AI 결과에 미치는 영향을 이해합니다.",
                        "KERIS AI for Oceans; 디지털 리터러시 데이터 이해",
                        "초등 교사를 위한 KERIS와 시작하는 인공지능 교육 1.pdf; [별책본] 디지털 리터러시 구성 체계 및 교과별 성취기준 연계.pdf",
                        tags("FACT", "VERIFICATION", "SAFETY"),
                        examples("AI for Oceans 분류", "과일 사진 모음", "강아지와 고양이 사진", "손글씨 숫자", "재활용품 사진", "꽃 이름표", "바다 생물 자료", "소리 녹음 자료"),
                        concepts("데이터", "이름표", "학습", "테스트", "자료의 다양성", "잘못된 이름표", "대표성", "새 자료"),
                        contexts("과학 조사", "미술 자료 정리", "환경 수업", "모둠 분류 활동", "온라인 체험", "숙제 자료 모으기", "학급 게시판", "도서관 프로젝트"),
                        good("다양한 예시를 모아 한쪽으로 치우치지 않게 해요.", "이름표가 맞는지 확인해요.", "학습에 쓰지 않은 새 자료로 테스트해요.", "자료가 부족하면 더 모은 뒤 다시 살펴봐요.", "잘못 분류된 예시는 고쳐요.", "허락받은 자료만 사용해요.", "어떤 자료로 배웠는지 기록해요.", "자료의 종류가 충분한지 비교해요."),
                        wrong("비슷한 사진 한 종류만 모아 전체를 대표한다고 해요.", "틀린 이름표를 그대로 둬요.", "테스트 없이 잘 배웠다고 정해요.", "친구 사진을 허락 없이 학습 자료로 써요.", "마음에 드는 결과만 골라 발표해요.", "자료가 적어도 결과를 무조건 믿어요.", "AI가 틀린 이유를 살피지 않아요.", "한 지역 자료만 모아 전국 자료라고 말해요."),
                        concerns("자료가 한쪽으로 치우친", "이름표가 틀린", "테스트가 빠진", "허락 없는 자료를 쓴", "새 자료에서 틀린"),
                        trueClaims("AI가 배우는 자료가 다양할수록 새 상황을 더 잘 판단하는 데 도움이 돼요.", "잘못된 이름표가 많으면 AI도 잘못 배울 수 있어요.", "학습에 쓰지 않은 새 자료로 테스트하는 과정이 필요해요.", "데이터는 AI가 배우는 예시 자료가 될 수 있어요.", "AI for Oceans 활동처럼 분류 예시를 보며 AI 원리를 체험할 수 있어요.", "자료가 부족하면 AI 결과가 한쪽으로 치우칠 수 있어요.", "사진뿐 아니라 글, 소리도 AI 학습 자료가 될 수 있어요.", "AI가 틀렸을 때는 데이터와 이름표를 함께 살펴볼 수 있어요."),
                        falseClaims("AI는 데이터가 없어도 저절로 정확하게 배워요.", "틀린 이름표가 있어도 AI 결과에는 영향이 없어요.", "학습에 쓴 자료만 다시 맞히면 새 자료도 반드시 잘 맞혀요.", "많이 모은 자료라면 허락은 생각하지 않아도 돼요.", "한 종류의 사진만 모아도 모든 상황을 대표할 수 있어요.", "테스트는 AI가 틀렸을 때만 해도 충분해요.", "데이터가 치우쳐도 AI는 항상 공정하게 판단해요.", "AI가 배운 자료는 아무도 확인할 필요가 없어요."),
                        fill("AI가 배우는 예시 자료를 ____라고 불러요.", "데이터", "소문", "장식", "암호", "데이터는 AI가 패턴을 배우는 바탕이 되는 자료예요."),
                        fill("사진에 '고양이'처럼 붙인 정답 이름은 ____예요.", "이름표", "비밀번호", "광고", "점수", "이름표가 맞아야 AI도 올바른 관계를 배울 수 있어요."),
                        fill("학습에 쓰지 않은 새 자료로 확인하는 과정은 ____예요.", "테스트", "복사", "숨기기", "삭제", "테스트는 배운 내용이 새 자료에도 통하는지 보는 과정이에요."),
                        fill("AI가 한쪽 자료만 보고 배우지 않으려면 자료의 ____이 필요해요.", "다양성", "비밀성", "화려함", "속도", "다양한 자료는 치우친 결과를 줄이는 데 도움을 줘요."),
                        fill("{example} 자료를 모을 때는 이름표가 ____지 확인해야 해요.", "맞는", "빠른", "비싼", "짧은", "틀린 이름표는 AI가 잘못 배우는 원인이 될 수 있어요.")
                ),
                mission(
                        "S0104", 1, "AI 알아보기", "사진·소리·글을 알아보는 AI",
                        "사진, 소리, 글을 인식하는 AI의 쓰임과 한계를 생활 사례로 살펴봅니다.",
                        "KERIS 초등 AI 교육 인식 활동; 디지털 리터러시 자료 판단",
                        "초등 교사를 위한 KERIS와 시작하는 인공지능 교육 1.pdf; 2021년 인공지능(AI)기본 역량 강화 연수 교재(초등).pdf",
                        tags("FACT", "VERIFICATION", "SAFETY"),
                        examples("사진 속 꽃 찾기", "음성 받아쓰기", "문장 번역", "손글씨 숫자 인식", "얼굴 대신 물체 인식", "새소리 구별", "QR 코드 읽기", "책 표지 검색"),
                        concepts("인식", "패턴", "사진 데이터", "소리 데이터", "글 데이터", "오인식", "학습 예시", "개인정보 주의"),
                        contexts("음악 시간", "국어 시간", "과학 관찰", "학교 태블릿", "발표 준비", "가정 학습", "체험 부스", "도서관 검색"),
                        good("사진, 소리, 글 중 어떤 자료를 쓰는지 살펴봐요.", "인식 결과가 이상하면 다시 확인해요.", "얼굴이나 목소리처럼 민감한 자료는 조심해요.", "밝기와 소음처럼 결과에 영향을 주는 조건을 생각해요.", "여러 예시로 비교해요.", "AI가 알아본 결과를 사람의 판단과 함께 봐요.", "틀린 인식 사례를 기록해요.", "필요하지 않은 개인 자료는 넣지 않아요."),
                        wrong("AI가 알아본 이름을 무조건 사실로 발표해요.", "친구 얼굴 사진을 허락 없이 올려요.", "소음이 심해도 결과가 항상 같다고 말해요.", "사진이 흐려도 AI는 절대 틀리지 않는다고 믿어요.", "인식 결과를 확인하지 않고 복사해요.", "개인 목소리 자료를 장난으로 공유해요.", "한 장의 사진만 보고 모든 대상을 판단해요.", "AI가 사람의 마음까지 읽는다고 설명해요."),
                        concerns("사진이 흐린", "소음이 큰", "글자가 잘린", "개인 얼굴이 들어간", "AI가 잘못 알아본"),
                        trueClaims("AI는 사진, 소리, 글에서 특징을 찾아 인식할 수 있어요.", "흐린 사진이나 소음은 인식 결과에 영향을 줄 수 있어요.", "인식 AI도 잘못 알아볼 수 있으므로 확인이 필요해요.", "얼굴과 목소리 자료는 개인정보와 관련될 수 있어요.", "번역 AI는 문장 뜻을 추측하지만 항상 자연스럽지는 않을 수 있어요.", "손글씨 인식은 여러 글씨 예시를 보고 배울 수 있어요.", "AI가 알아본 결과는 조건에 따라 달라질 수 있어요.", "사진 인식 결과를 쓸 때는 무엇을 근거로 했는지 살펴야 해요."),
                        falseClaims("AI가 사진을 알아보면 사람의 생각까지 읽은 거예요.", "소음이 커도 음성 인식 결과는 절대 달라지지 않아요.", "번역 AI의 문장은 항상 원래 뜻과 완전히 같아요.", "친구 얼굴 사진은 재미있으면 허락 없이 써도 돼요.", "흐린 사진도 AI는 언제나 정확히 알아봐요.", "한 번 인식한 결과는 다시 확인할 필요가 없어요.", "AI가 틀리면 자료 조건과는 아무 상관이 없어요.", "사진 인식은 데이터 없이도 스스로 완성돼요."),
                        fill("사진이나 소리를 보고 무엇인지 알아보는 기능은 ____과 관련이 있어요.", "인식", "충전", "포장", "삭제", "인식은 자료의 특징을 보고 대상을 알아보는 기능이에요."),
                        fill("음성 인식 결과는 주변 ____의 영향을 받을 수 있어요.", "소음", "색연필", "책상", "가방", "소음이 크면 AI가 말을 잘못 알아들을 수 있어요."),
                        fill("{example} 결과가 이상하면 바로 믿지 말고 ____해야 해요.", "확인", "복사", "장식", "숨김", "인식 AI도 틀릴 수 있으므로 확인이 필요해요."),
                        fill("얼굴이나 목소리 자료는 ____와 관련될 수 있어 조심해야 해요.", "개인정보", "간식", "날씨", "색깔", "얼굴과 목소리는 개인을 알아볼 수 있는 정보가 될 수 있어요."),
                        fill("AI가 글과 사진을 알아보려면 여러 ____가 필요해요.", "예시", "광고", "소문", "상자", "AI는 여러 예시에서 특징을 배우는 경우가 많아요.")
                ),
                mission(
                        "S0105", 1, "AI 알아보기", "AI도 틀릴 수 있어요",
                        "AI 답을 그대로 믿지 않고 한계와 오류 가능성을 이해합니다.",
                        "KERIS 초등 AI 교육 한계와 편향; 생성형 AI 활용 가이드 검증 원칙",
                        "초등 교사를 위한 KERIS와 시작하는 인공지능 교육 1.pdf; [GM 2024-05] 생성형 AI를 활용한 교수학습 운영 가이드_f.pdf",
                        tags("VERIFICATION", "FACT", "SAFETY"),
                        examples("역사 인물 설명", "날씨 정보", "숙제 풀이", "번역 결과", "사진 분류", "추천 이유", "과학 상식", "학교 행사 날짜"),
                        concepts("오류 가능성", "그럴듯한 답", "확인 습관", "최신 정보", "근거", "자료 부족", "잘못된 추측", "질문 조건"),
                        contexts("숙제 시간", "발표 준비", "교실 토의", "검색 활동", "가정 학습", "도서관 조사", "체험 보고서", "친구와 협력"),
                        good("AI 답의 핵심 내용을 다른 자료와 비교해요.", "출처와 날짜를 확인해요.", "이상한 부분은 선생님이나 공식 자료로 확인해요.", "질문 조건을 더 분명히 해서 다시 물어봐요.", "틀릴 수 있다는 점을 발표에 함께 말해요.", "근거가 없는 답은 바로 쓰지 않아요.", "여러 자료를 보고 공통점을 찾습니다.", "최신 정보인지 확인해요."),
                        wrong("AI가 자신 있게 말하면 그대로 제출해요.", "출처가 없어도 답이 길면 믿어요.", "틀린 부분을 찾아도 모른 척해요.", "날짜가 중요한 정보도 확인하지 않아요.", "친구에게 확인 없이 바로 공유해요.", "한 답만 보고 결론을 내요.", "AI 답을 내 생각처럼 발표해요.", "근거보다 표현이 멋진지만 봐요."),
                        concerns("날짜가 오래된", "근거가 없는", "서로 다른 답이 나온", "질문 조건이 빠진", "사진을 잘못 분류한"),
                        trueClaims("AI 답은 그럴듯해 보여도 틀릴 수 있어요.", "날짜가 중요한 정보는 최신 자료인지 확인해야 해요.", "AI가 모르는 내용을 추측해 말할 수 있으므로 근거가 필요해요.", "AI 답이 이상하면 질문을 고치거나 다른 자료와 비교할 수 있어요.", "숙제에 AI 도움을 받더라도 마지막 판단은 내가 해야 해요.", "출처가 없는 답은 바로 믿기 어렵습니다.", "AI가 틀린 이유에는 부족하거나 잘못된 데이터가 있을 수 있어요.", "확인 습관은 AI를 안전하게 쓰는 기본 태도예요."),
                        falseClaims("AI 답은 항상 맞으므로 출처가 필요 없어요.", "길고 자연스러운 답이면 반드시 사실이에요.", "AI가 틀렸을 때는 질문을 바꾸어도 소용없어요.", "최신 정보도 날짜 확인 없이 믿어도 돼요.", "친구 숙제에 AI 답을 그대로 보내도 문제가 없어요.", "사진 분류 AI는 처음 보는 사진도 절대 틀리지 않아요.", "근거가 없는 답도 마음에 들면 발표해도 돼요.", "AI가 틀릴 수 있다는 생각은 AI 사용과 관련이 없어요."),
                        fill("AI 답은 그럴듯해도 틀릴 수 있으므로 ____해야 해요.", "확인", "복사", "감추기", "장식", "AI 답은 항상 맞지 않기 때문에 확인 습관이 중요해요."),
                        fill("날짜가 중요한 정보는 ____ 자료인지 살펴야 해요.", "최신", "가장 긴", "가장 예쁜", "가장 빠른", "날짜 확인은 정보가 지금도 맞는지 판단하는 데 필요해요."),
                        fill("근거가 없는 AI 답은 바로 쓰지 말고 ____를 찾아야 해요.", "출처", "색깔", "이름표", "비밀번호", "출처와 근거를 보면 답의 믿을 만함을 판단할 수 있어요."),
                        fill("{example}에 대한 AI 답이 이상하면 질문을 ____ 다시 물어볼 수 있어요.", "고쳐", "숨겨", "삭제해", "외워", "조건을 더 분명히 하면 더 알맞은 답을 받을 수 있어요."),
                        fill("AI가 틀릴 수 있다는 점을 아는 것은 안전한 ____ 습관이에요.", "사용", "꾸미기", "잠금", "인쇄", "한계를 알고 쓰면 AI 답을 더 책임 있게 활용할 수 있어요.")
                ),
                mission(
                        "S0201", 2, "AI 안전하게 쓰기", "AI에게 도움 요청하기",
                        "목적, 대상, 형식을 분명히 말해 AI 도움을 알맞게 요청합니다.",
                        "생성형 AI 교수학습 운영 가이드; 디지털 리터러시 활용과 소통",
                        "[GM 2024-05] 생성형 AI를 활용한 교수학습 운영 가이드_f.pdf; [별책본] 디지털 리터러시 구성 체계 및 교과별 성취기준 연계.pdf",
                        tags("PROMPT", "SAFETY", "VERIFICATION"),
                        examples("발표 개요", "독서 감상문 힌트", "과학 실험 질문", "친구에게 설명할 문장", "퀴즈 연습", "학급 안내문 초안", "어려운 낱말 풀이", "토의 주제 정리"),
                        concepts("목적", "대상", "형식", "조건", "쉬운 말 요청", "예시 요청", "질문 고치기", "개인정보 제외"),
                        contexts("발표 준비", "숙제 계획", "모둠 토의", "온라인 학습", "도서관 조사", "가정 학습", "교실 활동", "복습 시간"),
                        good("무엇을 하려는지 먼저 말해요.", "누가 읽을 답인지 알려 줘요.", "표나 목록처럼 원하는 형식을 말해요.", "개인정보는 넣지 않고 질문해요.", "어려우면 쉬운 말로 다시 설명해 달라고 해요.", "필요한 조건을 짧게 덧붙여요.", "AI 답을 받은 뒤 내 상황에 맞게 고쳐요.", "예시와 주의할 점을 함께 요청해요."),
                        wrong("아무 설명 없이 '해줘'라고만 써요.", "친구의 이름과 연락처를 넣어 질문해요.", "목적을 숨기고 긴 답만 요구해요.", "AI 답을 확인하지 않고 그대로 제출해요.", "조건을 너무 많이 섞어 헷갈리게 물어요.", "누가 볼 글인지 말하지 않아요.", "틀린 답이 나와도 질문을 고치지 않아요.", "비밀 정보를 넣어 더 자세히 답해 달라고 해요."),
                        concerns("목적이 빠진", "대상이 분명하지 않은", "형식이 없는", "개인정보가 들어간", "질문이 너무 모호한"),
                        trueClaims("AI에게 도움을 요청할 때 목적을 말하면 더 알맞은 답을 받을 수 있어요.", "초등학생이 읽을 답인지, 발표용인지처럼 대상을 알려 주면 도움이 돼요.", "표, 목록, 짧은 문단처럼 원하는 형식을 말할 수 있어요.", "AI 질문에도 개인정보는 넣지 않아야 해요.", "AI 답이 어렵다면 쉬운 말로 다시 설명해 달라고 할 수 있어요.", "좋은 질문은 길기보다 목적과 조건이 분명한 질문이에요.", "AI는 내 생각을 완성해 주는 대신 도움을 주는 도구로 쓰는 것이 좋아요.", "답을 받은 뒤에는 사실과 표현을 다시 확인해야 해요."),
                        falseClaims("AI에게 물을 때는 목적을 숨길수록 답이 좋아져요.", "친구의 전화번호를 넣으면 더 좋은 질문이 되므로 괜찮아요.", "원하는 형식을 말하면 AI 사용 규칙을 어기는 거예요.", "AI 답은 받는 순간 바로 내 최종 답이 돼요.", "어려운 답이 나오면 무조건 그대로 외워야 해요.", "좋은 질문은 반드시 아주 길고 복잡해야 해요.", "개인정보는 질문에 넣어도 AI가 알아서 지워 줘요.", "대상을 말하지 않아도 모든 답은 같은 사람에게 알맞아요."),
                        fill("AI에게 질문할 때는 먼저 무엇을 하려는지 ____을 말해요.", "목적", "비밀", "장난", "소문", "목적을 말하면 AI가 필요한 방향을 더 잘 맞출 수 있어요."),
                        fill("친구에게 설명할 글이라면 읽을 ____을 알려 주면 좋아요.", "대상", "가격", "주소", "암호", "대상을 알려 주면 답의 말투와 수준을 맞추기 쉬워요."),
                        fill("표나 목록처럼 원하는 답의 모양은 ____이라고 할 수 있어요.", "형식", "소음", "무게", "속도", "형식을 말하면 원하는 모습의 답을 받는 데 도움이 돼요."),
                        fill("AI 질문에는 이름, 주소 같은 ____를 넣지 않아야 해요.", "개인정보", "준비물", "주제", "예시", "개인정보를 빼고 질문하는 것은 안전한 사용 습관이에요."),
                        fill("{example}을 요청할 때 어려우면 ____ 말로 설명해 달라고 할 수 있어요.", "쉬운", "비밀", "빠른", "비싼", "쉬운 말 요청은 AI 답을 이해하는 데 도움이 돼요.")
                ),
                mission(
                        "S0202", 2, "AI 안전하게 쓰기", "AI 답을 더 알맞게 고치기",
                        "AI 답이 목적에 맞지 않을 때 조건을 더하거나 다시 요청하는 방법을 연습합니다.",
                        "생성형 AI 교수학습 운영 가이드; 디지털 리터러시 정보 활용",
                        "[GM 2024-05] 생성형 AI를 활용한 교수학습 운영 가이드_f.pdf; 초등 교사를 위한 KERIS와 시작하는 인공지능 교육 2.pdf",
                        tags("PROMPT", "VERIFICATION", "SAFETY"),
                        examples("너무 긴 설명", "어려운 낱말이 많은 답", "주제에서 벗어난 답", "근거가 빠진 답", "나이에 맞지 않는 표현", "표가 필요한 자료", "예시가 부족한 답", "틀린 날짜가 있는 답"),
                        concepts("재질문", "조건 추가", "쉬운 말", "길이 조절", "형식 바꾸기", "근거 요청", "목적 확인", "수정"),
                        contexts("발표 연습", "숙제 정리", "모둠 보고서", "독서 활동", "과학 탐구", "학급 신문", "온라인 복습", "가정 학습"),
                        good("부족한 조건을 한두 가지 더해 다시 요청해요.", "어려운 표현은 쉬운 말로 바꿔 달라고 해요.", "목적에 맞는 길이와 형식을 다시 알려 줘요.", "근거가 필요하면 출처와 이유를 요청해요.", "AI 답을 읽고 내 말로 고쳐요.", "틀린 내용은 다른 자료로 확인해요.", "주제에서 벗어난 부분은 빼 달라고 해요.", "좋은 부분과 고칠 부분을 나누어 봐요."),
                        wrong("마음에 안 들어도 그대로 제출해요.", "개인정보를 더 넣어 정확하게 만들려고 해요.", "틀린 부분을 보아도 확인하지 않아요.", "AI에게 화내는 말만 입력해요.", "조건을 끝없이 많이 넣어 더 헷갈리게 해요.", "근거 없는 답을 멋진 표현이라서 믿어요.", "친구 글을 AI에 그대로 넣어 고쳐 달라고 해요.", "내가 이해하지 못한 답을 발표해요."),
                        concerns("답이 너무 긴", "표현이 어려운", "근거가 빠진", "주제에서 벗어난", "개인정보가 들어갈 위험이 있는"),
                        trueClaims("AI 답이 맞지 않으면 조건을 더해 다시 요청할 수 있어요.", "어려운 답은 쉬운 말로 설명해 달라고 요청할 수 있어요.", "AI 답을 고칠 때도 개인정보를 추가하지 않아야 해요.", "근거가 없는 답은 출처나 이유를 확인해야 해요.", "AI 답은 내 목적에 맞게 읽고 고쳐 쓰는 과정이 필요해요.", "답의 길이나 형식을 다시 말하면 더 알맞은 결과를 받을 수 있어요.", "주제에서 벗어난 답은 필요한 부분만 남기도록 다시 요청할 수 있어요.", "고친 답도 사실인지 다시 확인하는 태도가 중요해요."),
                        falseClaims("AI 답이 이상해도 처음 답만 사용해야 해요.", "더 정확한 답을 위해 친구 개인정보를 넣어도 돼요.", "재질문은 AI를 잘못 쓰는 행동이에요.", "답이 길면 근거가 없어도 믿어도 돼요.", "어려운 말을 쉬운 말로 바꿔 달라는 것은 나쁜 질문이에요.", "AI 답을 고치면 출처 확인은 필요 없어져요.", "목적에 안 맞는 답도 표현이 좋으면 그대로 제출해요.", "조건은 많을수록 언제나 더 좋은 답을 만들어요."),
                        fill("AI 답이 목적에 맞지 않으면 조건을 더해 ____할 수 있어요.", "재질문", "숨김", "복사", "포장", "재질문은 더 알맞은 답을 얻기 위한 자연스러운 방법이에요."),
                        fill("어려운 답은 ____ 말로 설명해 달라고 요청할 수 있어요.", "쉬운", "비밀", "빠른", "화려한", "쉬운 말 요청은 내용을 이해하는 데 도움이 돼요."),
                        fill("답의 길이, 표, 목록 같은 요구는 ____을 정하는 일이에요.", "형식", "주소", "비밀번호", "소문", "형식을 정하면 원하는 모양으로 답을 받을 수 있어요."),
                        fill("근거가 빠진 답은 ____와 이유를 다시 확인해요.", "출처", "색깔", "무게", "속도", "출처와 이유는 답을 믿을 수 있는지 판단하는 기준이에요."),
                        fill("{example}이 보이면 AI 답을 그대로 쓰지 말고 ____해야 해요.", "수정", "외면", "삭제만", "자랑", "AI 답은 읽고 고쳐 내 목적에 맞게 만드는 과정이 필요해요.")
                ),
                mission(
                        "S0203", 2, "AI 안전하게 쓰기", "개인정보는 넣지 않기",
                        "이름, 주소, 학교, 얼굴, 목소리, 위치 등 개인정보를 AI에 넣지 않는 습관을 익힙니다.",
                        "생성형 AI 교수학습 운영 가이드 개인정보 보호; 디지털 리터러시 안전",
                        "[GM 2024-05] 생성형 AI를 활용한 교수학습 운영 가이드_f.pdf; [KR 2026-01] 2025년 학생 디지털 리터러시 수준측정 연구_FF.pdf",
                        tags("PRIVACY", "SAFETY", "VERIFICATION"),
                        examples("이름과 반", "집 주소", "전화번호", "학교 이름", "얼굴 사진", "목소리 녹음", "현재 위치", "가족 정보"),
                        concepts("개인정보", "생체정보", "위치 정보", "허락", "가명", "비공개", "필요 최소", "안전한 질문"),
                        contexts("AI 채팅", "사진 편집 앱", "음성 변환 앱", "숙제 질문", "친구와 공유", "온라인 가입", "발표 자료 만들기", "태블릿 사용"),
                        good("실제 이름 대신 가상의 이름을 써요.", "주소와 전화번호는 입력하지 않아요.", "얼굴이나 목소리 자료는 허락 없이 올리지 않아요.", "필요한 정보만 최소한으로 사용해요.", "위치 정보가 들어가지 않았는지 확인해요.", "개인정보가 보이면 지우고 질문해요.", "선생님이나 보호자에게 먼저 물어봐요.", "친구 정보는 내 정보처럼 조심해요."),
                        wrong("더 정확한 답을 위해 실제 주소를 넣어요.", "친구 얼굴 사진을 허락 없이 올려요.", "전화번호를 예시로 그대로 입력해요.", "학교와 반을 모두 적어 공개 질문해요.", "위치 정보가 보이는 사진을 그대로 올려요.", "목소리 녹음을 장난으로 공유해요.", "가족 정보를 넣어 소개 글을 만들어요.", "개인정보가 있어도 AI가 알아서 보호한다고 믿어요."),
                        concerns("실제 이름이 들어간", "위치가 보이는", "친구 얼굴이 포함된", "전화번호가 적힌", "목소리로 알아볼 수 있는"),
                        trueClaims("AI에 질문할 때 실제 이름과 주소는 넣지 않는 것이 좋아요.", "얼굴과 목소리도 개인을 알아볼 수 있는 정보가 될 수 있어요.", "친구의 개인정보도 내 정보처럼 조심해야 해요.", "사진을 올리기 전에는 위치나 얼굴이 보이는지 확인해야 해요.", "필요 없는 개인정보는 지우고 질문하는 것이 안전해요.", "가상의 이름을 쓰면 개인정보 노출을 줄일 수 있어요.", "AI 앱이 편리해도 개인정보 보호 규칙은 지켜야 해요.", "개인정보가 필요한지 헷갈리면 어른에게 먼저 물어볼 수 있어요."),
                        falseClaims("AI가 물어보면 실제 주소를 자세히 넣어도 돼요.", "친구 얼굴 사진은 재미있으면 허락 없이 올려도 돼요.", "목소리는 개인정보와 전혀 관련이 없어요.", "위치가 보이는 사진은 언제나 안전해요.", "비밀번호는 AI가 기억하면 편하므로 입력해도 좋아요.", "개인정보는 한 번만 입력하면 문제가 생기지 않아요.", "친구 정보는 내 정보가 아니므로 마음대로 써도 돼요.", "AI 앱에서는 개인정보를 지우는 습관이 필요 없어요."),
                        fill("이름, 주소, 전화번호처럼 나를 알아볼 수 있는 정보는 ____예요.", "개인정보", "준비물", "주제어", "일반 설명", "개인정보는 나와 친구를 알아볼 수 있게 하는 정보예요."),
                        fill("얼굴, 지문, 목소리처럼 몸의 특징과 관련된 정보는 ____예요.", "몸 정보", "색깔 정보", "시간표", "간식", "몸의 특징 정보도 개인을 알아볼 수 있어 조심해야 해요."),
                        fill("실제 이름 대신 사용할 수 있는 안전한 이름은 ____이에요.", "가명", "주소", "비밀번호", "위치", "가명은 실제 개인정보 노출을 줄이는 데 도움이 돼요."),
                        fill("{example}이 들어간 자료를 올리기 전에는 ____을 받아야 해요.", "허락", "점수", "광고", "장식", "다른 사람 정보가 들어가면 허락을 먼저 생각해야 해요."),
                        fill("AI 질문에는 필요한 정보만 쓰는 ____ 원칙이 중요해요.", "최소", "최대", "비밀", "장난", "필요 최소 원칙은 불필요한 개인정보 노출을 줄여 줘요.")
                ),
                mission(
                        "S0204", 2, "AI 안전하게 쓰기", "좋은 데이터와 나쁜 데이터 구분하기",
                        "AI 자료를 모을 때 다양성, 정확한 이름표, 허락, 저작권을 함께 살핍니다.",
                        "KERIS AI for Oceans, Teachable Machine; 디지털 리터러시 데이터 윤리",
                        "초등 교사를 위한 KERIS와 시작하는 인공지능 교육 1.pdf; [별책본] 디지털 리터러시 구성 체계 및 교과별 성취기준 연계.pdf",
                        tags("FACT", "SAFETY", "PRIVACY", "VERIFICATION"),
                        examples("동물 사진 모음", "재활용품 사진", "목소리 녹음", "손글씨 자료", "학교 주변 사진", "그림 분류 자료", "바다 쓰레기 사진", "운동 자세 영상"),
                        concepts("좋은 데이터", "나쁜 데이터", "정확한 이름표", "자료 다양성", "허락", "저작권", "대표성", "품질"),
                        contexts("AI 분류 실험", "모둠 프로젝트", "환경 캠페인", "미술 시간", "체육 기록", "과학 관찰", "학급 신문", "온라인 체험"),
                        good("허락받은 자료만 모아요.", "여러 종류의 예시를 골고루 넣어요.", "이름표가 맞는지 확인해요.", "출처와 만든 사람을 기록해요.", "흐리거나 잘못된 자료는 따로 표시해요.", "개인정보가 보이는 자료는 제외해요.", "한쪽으로 치우친 자료인지 살펴봐요.", "저작권이 걱정되면 직접 만든 자료를 써요."),
                        wrong("인터넷 사진을 출처 없이 모두 가져와요.", "한 종류의 사진만 많이 모아요.", "틀린 이름표를 그대로 둬요.", "친구 목소리를 허락 없이 녹음해요.", "흐린 사진도 문제없다고 섞어요.", "마음에 드는 자료만 골라 전체처럼 발표해요.", "저작권과 허락을 확인하지 않아요.", "개인정보가 보이는 사진을 그대로 사용해요."),
                        concerns("허락이 없는", "출처가 빠진", "이름표가 틀린", "자료가 한쪽으로 몰린", "개인정보가 보이는"),
                        trueClaims("AI 학습 자료를 모을 때는 허락과 저작권을 함께 생각해야 해요.", "이름표가 틀리면 AI가 잘못 배울 수 있어요.", "다양한 자료는 치우친 결과를 줄이는 데 도움이 돼요.", "개인정보가 보이는 자료는 조심해서 다루어야 해요.", "출처를 적어 두면 자료를 책임 있게 사용할 수 있어요.", "좋은 데이터는 정확하고 목적에 맞으며 너무 치우치지 않은 자료예요.", "흐리거나 잘린 자료는 AI 결과를 나쁘게 만들 수 있어요.", "직접 만든 자료라도 다른 사람 모습이 있으면 허락을 확인해야 해요."),
                        falseClaims("AI 학습용이면 인터넷 사진을 마음대로 써도 돼요.", "이름표가 조금 틀려도 AI는 알아서 고쳐 배워요.", "한 종류의 자료만 많으면 항상 공정한 결과가 나와요.", "친구 목소리는 허락 없이 녹음해도 개인정보가 아니에요.", "출처는 발표할 때만 필요하고 자료 모을 때는 필요 없어요.", "자료가 흐려도 AI 결과에는 아무 영향이 없어요.", "개인정보가 보이는 사진은 학습용이면 공개해도 괜찮아요.", "좋은 데이터와 나쁜 데이터는 구분할 필요가 없어요."),
                        fill("AI 학습 자료를 모을 때는 먼저 사용 ____을 확인해요.", "허락", "속도", "소리", "장식", "허락은 다른 사람의 자료를 책임 있게 쓰기 위한 기본 조건이에요."),
                        fill("자료가 한쪽으로 치우치지 않으려면 ____이 필요해요.", "다양성", "비밀", "가격", "크기", "다양성은 AI 결과가 한쪽으로 치우치는 일을 줄여 줘요."),
                        fill("사진에 붙인 정답 이름이 맞는지 보는 것은 ____ 확인이에요.", "이름표", "비밀번호", "색깔", "날씨", "이름표가 정확해야 AI가 올바르게 배울 수 있어요."),
                        fill("다른 사람이 만든 그림이나 사진은 ____을 생각해야 해요.", "저작권", "간식", "운동", "속도", "저작권은 만든 사람의 권리를 존중하는 규칙이에요."),
                        fill("{example}에 개인정보가 보이면 학습 자료에서 ____해야 해요.", "제외", "확대", "자랑", "반복", "개인정보가 보이는 자료는 안전을 위해 빼거나 가려야 해요.")
                ),
                mission(
                        "S0205", 2, "AI 안전하게 쓰기", "AI 결과를 실험하고 고쳐 보기",
                        "AI 도구의 결과를 관찰하고 오류 원인을 찾아 자료와 질문을 고쳐 봅니다.",
                        "KERIS AI for Oceans, Teachable Machine; 생성형 AI 가이드 검토와 수정",
                        "초등 교사를 위한 KERIS와 시작하는 인공지능 교육 1.pdf; [GM 2024-05] 생성형 AI를 활용한 교수학습 운영 가이드_f.pdf",
                        tags("VERIFICATION", "FACT", "PROMPT", "SAFETY"),
                        examples("Teachable Machine 모델", "AI for Oceans 결과", "사진 분류 결과", "번역 문장", "요약 답변", "음성 인식 결과", "추천 목록", "퀴즈 생성 결과"),
                        concepts("실험", "오류 찾기", "자료 추가", "재시도", "비교", "테스트", "수정", "관찰 기록"),
                        contexts("AI 체험 수업", "모둠 실험", "발표 준비", "과학 탐구", "온라인 활동", "복습 시간", "동아리 프로젝트", "가정 학습"),
                        good("틀린 결과가 나온 예시를 따로 모아요.", "자료나 질문을 고친 뒤 다시 테스트해요.", "무엇이 달라졌는지 기록해요.", "여러 조건에서 결과를 비교해요.", "부족한 예시를 더 추가해요.", "AI 결과를 친구와 함께 확인해요.", "잘 맞은 경우와 틀린 경우를 모두 살펴요.", "수정한 뒤에도 개인정보가 없는지 확인해요."),
                        wrong("첫 결과가 틀려도 이유를 찾지 않아요.", "틀린 예시는 숨기고 맞은 것만 발표해요.", "자료를 바꾸지 않고 계속 같은 결과만 기대해요.", "개인정보가 담긴 자료를 더 넣어요.", "비교 없이 마음에 드는 결과만 골라요.", "기록하지 않아 무엇을 고쳤는지 모르게 해요.", "AI가 틀리면 바로 포기해요.", "테스트 없이 완성됐다고 말해요."),
                        concerns("틀린 예시가 반복된", "자료가 부족한", "질문 조건이 모호한", "테스트 기록이 없는", "개인정보가 추가될 위험이 있는"),
                        trueClaims("AI 결과가 틀리면 자료나 질문을 고쳐 다시 테스트할 수 있어요.", "잘 맞은 예시와 틀린 예시를 모두 보는 것이 좋아요.", "무엇을 바꾸었는지 기록하면 개선 과정을 알 수 있어요.", "자료가 부족하면 AI 결과가 불안정할 수 있어요.", "Teachable Machine 같은 도구는 예시를 추가하며 결과 변화를 살펴볼 수 있어요.", "AI 결과를 실험할 때도 개인정보는 넣지 않아야 해요.", "첫 결과만 보고 결론을 내리기보다 여러 조건에서 비교해야 해요.", "AI를 고쳐 쓰는 과정은 확인과 수정의 반복이에요."),
                        falseClaims("AI가 한 번 틀리면 절대 고칠 방법이 없어요.", "맞은 결과만 모아 발표하면 가장 정직한 실험이에요.", "자료를 추가해도 AI 결과는 절대 달라지지 않아요.", "기록은 필요 없고 마지막 결과만 보면 돼요.", "개인정보를 많이 넣을수록 안전한 실험이 돼요.", "테스트 없이 첫 결과가 마음에 들면 완성이라고 해도 돼요.", "틀린 예시는 숨겨야 좋은 발표가 돼요.", "AI 결과를 비교하는 일은 실험과 관련이 없어요."),
                        fill("AI 결과를 고쳐 보려면 바꾼 내용을 ____해 두면 좋아요.", "기록", "삭제", "장식", "숨김", "기록은 무엇을 고쳤을 때 결과가 달라졌는지 알게 해 줘요."),
                        fill("자료나 질문을 고친 뒤에는 다시 ____해야 해요.", "테스트", "복사", "잠금", "자랑", "테스트는 수정한 내용이 실제로 도움이 되었는지 확인하는 과정이에요."),
                        fill("AI가 자주 틀리는 예시는 원인을 찾기 위해 따로 ____해요.", "모아", "버려", "숨겨", "외워", "틀린 예시를 모으면 어떤 조건에서 약한지 알 수 있어요."),
                        fill("부족한 예시는 더 ____하면 결과를 살펴볼 수 있어요.", "추가", "삭제", "감추기", "인쇄", "자료를 추가하면 AI 결과가 어떻게 달라지는지 비교할 수 있어요."),
                        fill("{example}을 실험할 때 개인정보는 넣지 않는 ____이 필요해요.", "주의", "속도", "광고", "장식", "실험 과정에서도 개인정보 보호는 꼭 지켜야 해요.")
                ),
                mission(
                        "S0206", 2, "AI 안전하게 쓰기", "AI 도움을 내 말로 정리하기",
                        "AI가 준 힌트와 예시를 이해한 뒤 내 표현으로 정리하고 책임 있게 사용합니다.",
                        "생성형 AI 교수학습 운영 가이드 책임 있는 활용; 디지털 리터러시 저작권",
                        "[GM 2024-05] 생성형 AI를 활용한 교수학습 운영 가이드_f.pdf; [별책본] 디지털 리터러시 구성 체계 및 교과별 성취기준 연계.pdf",
                        tags("SAFETY", "VERIFICATION", "PROMPT"),
                        examples("숙제 답안", "발표 원고", "독서 감상문", "그림 설명", "요약문", "토의 의견", "퀴즈 해설", "학급 신문 기사"),
                        concepts("내 말로 정리", "표절 예방", "AI 도움 표시", "이해 확인", "책임", "저작권", "힌트 활용", "출처 생각"),
                        contexts("숙제 제출", "발표 준비", "글쓰기 시간", "모둠 과제", "독서 활동", "온라인 게시", "학급 신문", "복습 노트"),
                        good("AI 답을 읽고 이해한 뒤 내 말로 바꿔요.", "AI 도움을 받았다는 점을 필요하면 밝혀요.", "모르는 내용은 다시 확인해요.", "그림이나 글의 사용 권리를 생각해요.", "AI 답을 그대로 베끼지 않아요.", "내 생각과 예시를 더해 정리해요.", "출처가 필요한 자료는 따로 확인해요.", "친구의 글을 AI에 넣기 전 허락을 구해요."),
                        wrong("AI 답을 읽지 않고 그대로 제출해요.", "AI가 쓴 글을 전부 내 생각이라고 말해요.", "친구 글을 허락 없이 AI에 넣어요.", "출처가 필요한 자료를 확인하지 않아요.", "이해하지 못한 표현을 그대로 발표해요.", "AI 그림을 아무 곳에나 마음대로 올려요.", "틀린 내용이 있어도 표현이 좋아서 그대로 둬요.", "내 생각을 하나도 넣지 않아요."),
                        concerns("그대로 베낀", "출처가 빠진", "이해하지 못한", "AI 도움을 숨긴", "저작권을 확인하지 않은"),
                        trueClaims("AI 도움을 받았더라도 마지막 답은 내 말로 정리해야 해요.", "AI 답을 그대로 베끼면 표절 문제가 생길 수 있어요.", "AI가 만든 글이나 그림을 사용할 때도 책임이 있어요.", "AI 답을 이해하지 못했다면 다시 설명을 요청하거나 확인해야 해요.", "출처가 필요한 정보는 따로 확인해 적는 것이 좋아요.", "친구의 글이나 사진은 허락 없이 AI에 넣지 않아야 해요.", "AI는 생각을 돕는 도구이지 내 판단을 대신하는 사람은 아니에요.", "내 경험과 생각을 더하면 더 책임 있는 결과물이 돼요."),
                        falseClaims("AI가 쓴 글은 무조건 내 글처럼 제출해도 돼요.", "AI 도움을 받았다는 사실은 언제나 숨겨야 해요.", "이해하지 못한 문장도 멋지면 발표해도 괜찮아요.", "친구 글은 허락 없이 AI에 넣어도 문제가 없어요.", "AI 그림에는 사용 책임이나 저작권 고민이 전혀 없어요.", "출처 확인은 AI를 쓰면 필요 없어져요.", "내 생각을 넣으면 AI 답이 나빠지므로 빼야 해요.", "AI가 만든 결과는 틀릴 수 없으니 고칠 필요가 없어요."),
                        fill("AI 도움을 받은 뒤 마지막 답은 ____ 말로 정리해요.", "내", "비밀", "친구", "광고", "내 말로 정리해야 내용을 이해하고 책임 있게 사용할 수 있어요."),
                        fill("AI 답을 그대로 베끼면 ____ 문제가 생길 수 있어요.", "표절", "날씨", "속도", "색깔", "표절은 다른 사람이나 도구의 결과를 내 것처럼 쓰는 문제예요."),
                        fill("AI 도움을 받았는지 필요할 때 ____하는 태도가 책임 있어요.", "밝히기", "숨기기", "삭제하기", "장난치기", "도움을 받은 사실을 알맞게 밝히면 더 정직한 활용이 돼요."),
                        fill("AI가 만든 글이나 그림도 사용할 때는 ____을 생각해야 해요.", "책임", "간식", "높이", "날씨", "AI 결과를 쓰는 사람에게도 확인하고 고칠 책임이 있어요."),
                        fill("{example}을 제출하기 전에는 내가 내용을 ____했는지 확인해요.", "이해", "암호화", "복사", "포장", "이해하지 못한 답은 내 학습 결과라고 보기 어려워요.")
                ),
                mission(
                        "S0301", 3, "AI 판단하기", "AI 답 다시 확인하기",
                        "AI 답의 핵심 주장을 찾고 다른 자료, 공식 출처, 날짜로 다시 확인합니다.",
                        "디지털 리터러시 수준측정 정보 판단; 생성형 AI 가이드 검증",
                        "[KR 2026-01] 2025년 학생 디지털 리터러시 수준측정 연구_FF.pdf; [GM 2024-05] 생성형 AI를 활용한 교수학습 운영 가이드_f.pdf",
                        tags("VERIFICATION", "FACT", "SAFETY"),
                        examples("역사 사건 설명", "과학 상식", "학교 행사 날짜", "건강 정보", "뉴스 요약", "인물 소개", "통계 수치", "기후 자료"),
                        concepts("핵심 주장", "팩트체크", "공식 출처", "날짜 확인", "여러 자료 비교", "근거", "오류 찾기", "최신성"),
                        contexts("발표 준비", "사회 조사", "과학 탐구", "뉴스 읽기", "숙제 검토", "도서관 조사", "온라인 검색", "모둠 토의"),
                        good("핵심 주장을 먼저 표시해요.", "공식 기관이나 믿을 만한 자료와 비교해요.", "자료의 날짜를 확인해요.", "서로 다른 자료의 공통점을 찾아요.", "근거가 부족하면 더 찾아봐요.", "확인한 내용만 발표에 사용해요.", "틀린 가능성을 메모해요.", "AI 답과 내 판단을 구분해요."),
                        wrong("AI 답이 길면 바로 사실로 믿어요.", "날짜가 중요한데도 확인하지 않아요.", "출처 없는 답을 그대로 발표해요.", "내가 원하는 답만 골라요.", "서로 다른 자료를 비교하지 않아요.", "오류 가능성을 친구에게 말하지 않아요.", "공식 자료보다 댓글을 먼저 믿어요.", "근거가 없어도 표현이 자연스러우면 통과시켜요."),
                        concerns("날짜가 맞지 않는", "공식 출처가 없는", "핵심 주장이 흐린", "자료끼리 서로 다른", "근거가 부족한"),
                        trueClaims("AI 답을 확인할 때는 핵심 주장을 먼저 찾는 것이 좋아요.", "날짜가 중요한 정보는 최신 자료인지 확인해야 해요.", "공식 기관 자료는 팩트체크에 도움이 될 수 있어요.", "AI 답이 길고 자연스러워도 틀릴 수 있어요.", "여러 자료를 비교하면 오류를 찾기 쉬워요.", "근거가 없는 답은 바로 발표하기 어렵습니다.", "AI 답과 내가 확인한 내용을 구분해 적어야 해요.", "팩트체크는 AI를 비판적으로 쓰는 기본 습관이에요."),
                        falseClaims("AI 답이 길면 팩트체크가 필요 없어요.", "날짜는 모든 정보에서 전혀 중요하지 않아요.", "출처가 없는 답도 표현이 좋으면 공식 자료와 같아요.", "내가 듣고 싶은 답만 고르면 확인이 끝나요.", "서로 다른 자료가 나오면 아무거나 골라도 돼요.", "AI 답을 확인하는 일은 비판적 사고와 관련이 없어요.", "공식 자료보다 익명 댓글이 항상 더 믿을 만해요.", "근거가 빠진 답은 더 멋진 말로 바꾸면 사실이 돼요."),
                        fill("AI 답에서 가장 먼저 확인할 중심 내용은 ____ 주장이에요.", "핵심", "비밀", "장식", "빠른", "핵심 주장을 찾아야 무엇을 확인할지 분명해져요."),
                        fill("학교 행사 날짜처럼 시간이 중요한 정보는 ____을 확인해요.", "날짜", "색깔", "목소리", "크기", "날짜 확인은 정보가 지금도 맞는지 판단하는 데 필요해요."),
                        fill("팩트체크에는 공식 기관이나 믿을 만한 ____가 도움이 돼요.", "출처", "소문", "광고", "느낌", "믿을 만한 출처는 정보 판단의 근거가 돼요."),
                        fill("AI 답과 다른 자료를 함께 보는 것은 자료 ____예요.", "비교", "숨김", "복사", "장식", "비교하면 서로 다른 점과 오류 가능성을 찾기 쉬워요."),
                        fill("{example}이 맞는지 보려면 답의 ____를 찾아야 해요.", "근거", "색연필", "음악", "점수", "근거는 답이 왜 맞는지 판단하는 기준이에요.")
                ),
                mission(
                        "S0302", 3, "AI 판단하기", "출처와 근거 비교하기",
                        "누가, 언제, 어디에서, 어떤 근거로 말했는지 비교해 자료의 믿을 만함을 판단합니다.",
                        "디지털 리터러시 구성 체계 정보 평가; 학생 디지털 리터러시 수준측정",
                        "[별책본] 디지털 리터러시 구성 체계 및 교과별 성취기준 연계.pdf; [KR 2026-01] 2025년 학생 디지털 리터러시 수준측정 연구_FF.pdf",
                        tags("VERIFICATION", "FACT"),
                        examples("블로그 글", "공식 누리집", "뉴스 기사", "영상 댓글", "통계 표", "백과 자료", "홍보 글", "전문가 인터뷰"),
                        concepts("출처", "근거", "작성자", "날짜", "기관", "목적", "자료 비교", "믿을 만함"),
                        contexts("사회 발표", "과학 조사", "독서 토론", "뉴스 분석", "온라인 검색", "모둠 보고서", "가정 과제", "학급 토의"),
                        good("작성자와 기관을 확인해요.", "언제 만든 자료인지 날짜를 봐요.", "주장을 뒷받침하는 근거가 있는지 살펴요.", "광고나 홍보 목적이 있는지 생각해요.", "공식 자료와 개인 의견을 구분해요.", "여러 자료의 공통점과 차이점을 표시해요.", "출처를 적어 책임 있게 사용해요.", "근거가 약한 자료는 보조 자료로만 봐요."),
                        wrong("제목이 자극적이면 바로 믿어요.", "누가 썼는지 보지 않아요.", "날짜가 오래됐는지 확인하지 않아요.", "광고 글을 객관적 자료처럼 발표해요.", "근거 없는 댓글을 공식 자료처럼 써요.", "한 자료만 보고 결론을 내요.", "출처를 지우고 내 자료처럼 사용해요.", "내 생각과 맞는 자료만 골라요."),
                        concerns("작성자가 불분명한", "광고 목적이 있는", "날짜가 오래된", "근거가 약한", "출처가 지워진"),
                        trueClaims("자료를 비교할 때는 누가 썼는지 확인해야 해요.", "날짜는 자료의 믿을 만함을 판단하는 기준이 될 수 있어요.", "근거가 있는 주장은 근거 없는 주장보다 확인하기 쉬워요.", "공식 누리집과 개인 댓글은 같은 무게로 보기 어렵습니다.", "광고 목적이 있는 자료는 목적을 함께 살펴야 해요.", "출처를 적는 것은 책임 있는 정보 활용 습관이에요.", "여러 자료를 비교하면 한 자료의 약점을 찾기 쉬워요.", "작성자, 날짜, 기관, 근거를 함께 보면 판단이 더 정확해져요."),
                        falseClaims("출처가 없어도 제목이 멋지면 믿을 만해요.", "언제 만든 자료인지는 절대 볼 필요가 없어요.", "광고 글은 항상 객관적인 연구 결과와 같아요.", "댓글 하나만으로도 공식 자료 확인이 끝나요.", "출처를 지우면 자료가 더 책임 있게 보여요.", "내 생각과 맞는 자료만 고르는 것이 가장 공정해요.", "작성자가 누구인지는 정보 판단과 관계가 없어요.", "근거가 없어도 글이 길면 사실이라고 볼 수 있어요."),
                        fill("자료를 만든 사람이나 기관을 알려 주는 정보는 ____예요.", "출처", "색깔", "비밀번호", "배경음", "출처는 자료가 어디에서 왔는지 알려 주는 중요한 단서예요."),
                        fill("주장을 뒷받침하는 이유나 자료는 ____예요.", "근거", "장식", "소문", "감정", "근거는 주장을 믿을 수 있는지 판단하게 해 줘요."),
                        fill("자료가 언제 만들어졌는지 보려면 ____를 확인해요.", "날짜", "글씨체", "소리", "가격", "날짜는 정보가 오래되었는지 판단하는 데 필요해요."),
                        fill("공식 자료와 개인 의견은 같은지 다른지 ____해야 해요.", "비교", "숨김", "복사", "삭제", "자료를 비교하면 믿을 만함을 더 잘 판단할 수 있어요."),
                        fill("{example}을 사용할 때는 만든 목적이 ____인지도 살펴요.", "무엇", "얼마", "몇 색", "몇 장", "자료의 목적을 보면 광고나 주장인지 판단하는 데 도움이 돼요.")
                ),
                mission(
                        "S0303", 3, "AI 판단하기", "데이터 편향 찾아보기",
                        "대표성이 부족한 자료가 AI 결과를 한쪽으로 치우치게 만들 수 있음을 이해합니다.",
                        "KERIS 인공지능 편향성; 디지털 리터러시 공정성과 정보 평가",
                        "초등 교사를 위한 KERIS와 시작하는 인공지능 교육 1.pdf; [KR 2026-01] 2025년 학생 디지털 리터러시 수준측정 연구_FF.pdf",
                        tags("VERIFICATION", "FACT", "SAFETY"),
                        examples("한 반 설문", "특정 지역 사진", "비슷한 얼굴 사진", "남학생 의견만 모은 자료", "도시 자료만 있는 표", "한 계절 사진", "추천 영상 기록", "일부 친구의 댓글"),
                        concepts("편향", "대표성", "치우친 데이터", "공정성", "다양한 관점", "표본", "추천 알고리즘", "결과 영향"),
                        contexts("설문 조사", "사진 분류 활동", "추천 영상 분석", "학교 규칙 토의", "모둠 발표", "지역 조사", "AI 모델 만들기", "온라인 자료 수집"),
                        good("자료가 누구를 대표하는지 살펴봐요.", "빠진 집단이나 상황이 없는지 확인해요.", "다양한 의견과 예시를 더 모아요.", "한쪽 자료만으로 전체 결론을 내리지 않아요.", "추천 결과가 비슷한 내용만 보여 주는지 생각해요.", "치우친 결과가 누구에게 불리한지 따져 봐요.", "자료 모은 방법을 발표에 함께 말해요.", "공정한 판단을 위해 다른 자료와 비교해요."),
                        wrong("한 반 의견을 학교 전체 의견이라고 말해요.", "한쪽 사진만 모아도 공정하다고 해요.", "빠진 친구들의 의견은 중요하지 않다고 봐요.", "추천 영상이 보여 준 내용만 세상의 전부라고 믿어요.", "치우친 자료를 고치지 않고 발표해요.", "누가 불편할지 생각하지 않아요.", "자료를 어떻게 모았는지 숨겨요.", "내가 좋아하는 의견만 남겨요."),
                        concerns("한 집단만 들어간", "지역이 한쪽인", "계절이 한 가지인", "추천이 비슷한 것만 보이는", "빠진 의견이 많은"),
                        trueClaims("데이터가 한쪽으로 치우치면 AI 결과도 치우칠 수 있어요.", "한 반의 의견만으로 학교 전체 의견을 대표하기는 어려워요.", "대표성이 부족한 자료는 공정한 판단을 방해할 수 있어요.", "추천 AI는 내가 본 것과 비슷한 내용만 더 보여 줄 수 있어요.", "편향을 줄이려면 다양한 자료와 관점이 필요해요.", "빠진 집단이 있는지 살피는 것은 공정성과 관련이 있어요.", "자료를 어떻게 모았는지 설명하면 결과를 더 잘 판단할 수 있어요.", "치우친 결과는 누군가에게 불리한 영향을 줄 수 있어요."),
                        falseClaims("데이터가 한쪽으로 치우쳐도 AI는 항상 공정해요.", "한 명의 의견만 들어도 전체 의견을 정확히 알 수 있어요.", "추천 영상은 세상의 모든 관점을 골고루 보여 줘요.", "빠진 사람이나 상황은 결과에 아무 영향이 없어요.", "편향은 어른들만 생각하면 되고 학생에게는 필요 없어요.", "자료 모은 방법은 숨길수록 더 믿을 만해요.", "내가 좋아하는 자료만 남기면 가장 공정해요.", "AI 결과가 불리하게 작용할 사람은 생각하지 않아도 돼요."),
                        fill("자료가 한쪽으로 치우친 상태를 ____이라고 해요.", "편향", "충전", "장식", "잠금", "편향은 결과가 한쪽으로 기울 수 있는 원인이 돼요."),
                        fill("자료가 전체를 잘 나타내려면 여러 경우가 ____ 담겨야 해요.", "골고루", "비밀로", "빠르게", "화려하게", "자료가 한쪽에 치우치면 전체 결론을 내리기 어려워요."),
                        fill("공정한 AI 결과를 위해서는 ____ 자료가 필요해요.", "다양한", "비싼", "빠른", "짧은", "다양한 자료는 한쪽으로 치우친 결과를 줄이는 데 도움이 돼요."),
                        fill("{example}만 모으면 결과가 ____ 수 있어요.", "치우칠", "반짝일", "잠길", "커질", "한쪽 자료만 있으면 결과가 한쪽으로 기울 수 있어요."),
                        fill("추천 AI가 비슷한 것만 보여 줄 때는 다른 ____도 찾아봐야 해요.", "관점", "색깔", "소리", "비밀번호", "다른 관점을 찾아보면 좁은 정보만 보는 일을 줄일 수 있어요.")
                ),
                mission(
                        "S0304", 3, "AI 판단하기", "AI의 좋은 점과 걱정되는 점",
                        "AI 기술의 편리함과 개인정보, 감시, 차별, 의존 같은 걱정을 균형 있게 봅니다.",
                        "KERIS 인공지능의 양면성; 디지털 리터러시 윤리와 영향",
                        "초등 교사를 위한 KERIS와 시작하는 인공지능 교육 1.pdf; [별책본] 디지털 리터러시 구성 체계 및 교과별 성취기준 연계.pdf",
                        tags("SAFETY", "FACT", "VERIFICATION", "PRIVACY"),
                        examples("번역 AI", "추천 서비스", "얼굴 인식 출입", "학습 도우미", "채팅 AI", "음성 비서", "AI 카메라", "자동 채점"),
                        concepts("양면성", "편리함", "걱정", "개인정보", "감시", "차별", "의존", "책임"),
                        contexts("학교 생활", "가정 학습", "온라인 서비스", "도서관 이용", "발표 준비", "친구와 대화", "체험 학습", "방과 후 활동"),
                        good("좋은 점과 걱정되는 점을 함께 적어요.", "편리함 뒤에 개인정보 문제가 없는지 살펴요.", "누가 도움을 받고 누가 불편할지 생각해요.", "AI에 지나치게 의존하지 않도록 내 판단을 남겨요.", "문제가 보이면 사용 방법을 바꾸거나 알릴 수 있어요.", "기술의 목적과 영향을 함께 봐요.", "차별이나 감시 우려를 쉬운 말로 설명해요.", "책임 있게 쓸 규칙을 정해요."),
                        wrong("편리하면 걱정은 모두 무시해요.", "AI가 해 주면 내 판단은 필요 없다고 해요.", "개인정보가 모이는지 보지 않아요.", "불편한 사람이 있어도 상관없다고 말해요.", "AI의 좋은 점만 골라 발표해요.", "감시처럼 느껴지는 상황을 장난으로 넘겨요.", "차별 가능성을 확인하지 않아요.", "문제가 생겨도 책임질 사람이 없다고 말해요."),
                        concerns("개인정보가 많이 모이는", "감시처럼 느껴지는", "누군가에게 불리한", "AI에 지나치게 의존하는", "좋은 점만 강조한"),
                        trueClaims("AI 기술은 좋은 점과 걱정되는 점을 함께 가질 수 있어요.", "번역 AI는 편리하지만 틀린 번역을 확인해야 해요.", "얼굴 인식은 편리할 수 있지만 개인정보와 감시 걱정도 있어요.", "AI를 쓸 때도 마지막 판단과 책임은 사람에게 있어요.", "추천 서비스는 편리하지만 비슷한 내용만 보게 만들 수 있어요.", "기술의 영향을 생각할 때는 도움받는 사람과 불편한 사람을 함께 봐야 해요.", "AI에 너무 의존하면 내가 생각하는 힘이 줄 수 있어요.", "좋은 점만 보지 않고 걱정되는 점도 말하는 태도가 필요해요."),
                        falseClaims("AI가 편리하면 걱정되는 점은 절대 없어요.", "AI가 한 일에는 사람이 책임질 필요가 없어요.", "얼굴 인식은 개인정보와 전혀 관계가 없어요.", "추천 서비스는 언제나 모든 관점을 골고루 보여 줘요.", "AI 답을 쓰면 내 생각은 필요 없어져요.", "불편한 사람이 있어도 기술이 멋지면 무시해도 돼요.", "좋은 점만 발표하면 가장 균형 잡힌 판단이에요.", "감시처럼 느껴지는 상황은 학생이 생각할 문제가 아니에요."),
                        fill("AI의 좋은 점과 걱정되는 점을 함께 보는 태도는 ____을 이해하는 거예요.", "양면성", "비밀번호", "속도", "모양", "양면성은 한 기술에 좋은 영향과 걱정되는 영향이 함께 있음을 뜻해요."),
                        fill("얼굴 인식처럼 사람을 알아보는 기술은 ____ 보호를 생각해야 해요.", "개인정보", "색연필", "간식", "운동장", "사람을 알아볼 수 있는 정보는 개인정보와 관련될 수 있어요."),
                        fill("AI를 쓸 때 마지막 판단과 ____은 사람에게 있어요.", "책임", "소음", "장식", "온도", "AI는 도구이고 결과를 쓰는 사람에게 확인과 책임이 남아요."),
                        fill("추천 서비스가 비슷한 내용만 보여 주면 다른 ____도 찾아봐요.", "관점", "음식", "암호", "상자", "다른 관점을 찾으면 좁은 정보만 보는 일을 줄일 수 있어요."),
                        fill("{example}의 좋은 점만 보지 말고 걱정되는 ____도 함께 살펴요.", "영향", "색깔", "속도", "높이", "영향을 함께 살펴야 기술을 균형 있게 판단할 수 있어요.")
                ),
                mission(
                        "S0305", 3, "AI 판단하기", "생활 속 공정한 AI 사용",
                        "학교와 생활 속 AI 결정이 모두에게 공정한지 살피고 더 나은 선택을 고민합니다.",
                        "디지털 리터러시 윤리와 공정성; KERIS AI 편향과 딜레마의 초등 완화",
                        "[별책본] 디지털 리터러시 구성 체계 및 교과별 성취기준 연계.pdf; 초등 교사를 위한 KERIS와 시작하는 인공지능 교육 1.pdf",
                        tags("SAFETY", "VERIFICATION", "FACT"),
                        examples("모둠 추천", "자동 채점", "도서 추천", "급식 의견 분석", "학급 규칙 설문", "동아리 배정", "발표 순서 추천", "학교 행사 안내"),
                        concepts("공정성", "설명 가능성", "이의 제기", "다양한 의견", "불리한 사람", "기준 공개", "사람의 확인", "책임 있는 선택"),
                        contexts("학급 회의", "학교 행사", "모둠 활동", "온라인 설문", "도서관 추천 활동", "수행평가", "동아리 활동", "생활 규칙 만들기"),
                        good("AI 결정 기준을 알 수 있는지 확인해요.", "불리한 친구가 없는지 살펴봐요.", "사람이 다시 확인할 기회를 둬요.", "이의 제기할 방법을 마련해요.", "다양한 의견을 모은 뒤 판단해요.", "결과가 공정한지 이유를 설명해요.", "AI 추천은 참고 자료로만 사용해요.", "규칙을 모두에게 미리 알려요."),
                        wrong("AI가 정했으니 아무도 질문하면 안 된다고 해요.", "불리한 친구가 있어도 기준을 숨겨요.", "한쪽 의견만 모아 전체 규칙을 정해요.", "사람이 다시 볼 기회를 없애요.", "이유를 설명하지 않고 결과만 발표해요.", "AI 추천을 무조건 최종 결정으로 써요.", "틀릴 가능성을 말하지 않아요.", "소수 의견을 일부러 빼요."),
                        concerns("기준이 보이지 않는", "이의 제기 방법이 없는", "한쪽 의견만 반영된", "누군가에게 불리한", "사람 확인이 빠진"),
                        trueClaims("AI가 내린 결정도 공정한지 사람이 확인해야 해요.", "결정 기준을 알 수 있으면 결과를 더 잘 판단할 수 있어요.", "불리한 사람이 생기지 않는지 살피는 것은 공정성과 관련이 있어요.", "AI 추천은 참고가 될 수 있지만 무조건 최종 결정은 아니에요.", "이의 제기할 방법이 있으면 더 책임 있는 사용에 가까워져요.", "한쪽 의견만 모으면 공정한 규칙을 만들기 어려워요.", "사람이 다시 확인할 기회를 두면 오류 피해를 줄일 수 있어요.", "생활 속 AI 사용은 편리함뿐 아니라 공정한 절차도 중요해요."),
                        falseClaims("AI가 정한 결과는 언제나 공정하므로 확인할 필요가 없어요.", "결정 기준은 숨길수록 더 믿을 만해요.", "불리한 친구가 있어도 AI가 정했으면 그냥 따라야 해요.", "이의 제기 방법은 공정성과 관계가 없어요.", "한쪽 의견만 모아도 모두를 위한 규칙을 만들 수 있어요.", "AI 추천은 언제나 최종 결정으로 써야 해요.", "사람이 다시 확인하면 AI 사용이 불공정해져요.", "공정성은 학교생활과 관련이 없는 어려운 말이에요."),
                        fill("AI 결정이 모두에게 알맞은지 살피는 기준은 ____이에요.", "공정성", "속도", "색깔", "소음", "공정성은 누군가에게 부당하게 불리하지 않은지 살피는 기준이에요."),
                        fill("AI 결과가 왜 나왔는지 알 수 있어야 ____하기 쉬워요.", "판단", "숨기기", "꾸미기", "충전", "이유와 기준을 알면 결과를 더 책임 있게 판단할 수 있어요."),
                        fill("AI 결정에 문제가 있을 때는 ____ 달라고 말할 수 있어요.", "다시 봐", "광고해", "꾸며", "숨겨", "문제가 있는 결과는 다시 살펴볼 기회를 두어야 해요."),
                        fill("AI 추천은 최종 결정이 아니라 ____ 자료로 쓰는 것이 좋아요.", "참고", "비밀", "정답", "벌칙", "AI 추천은 사람이 판단할 때 참고할 수 있는 자료예요."),
                        fill("{example}을 정할 때는 빠진 ____이 없는지 살펴야 해요.", "의견", "색깔", "암호", "간식", "다양한 의견을 살피면 더 공정한 결정을 하는 데 도움이 돼요.")
                )
        );
    }

    private static Mission mission(
            String code,
            int stage,
            String stageTitle,
            String missionTitle,
            String summary,
            String curriculumRef,
            String sourceReference,
            String[] tags,
            String[] examples,
            String[] concepts,
            String[] contexts,
            String[] goodActions,
            String[] wrongActions,
            String[] concerns,
            String[] trueClaims,
            String[] falseClaims,
            FillCard... fillCards
    ) {
        return new Mission(
                code,
                stage,
                stageTitle,
                missionTitle,
                summary,
                curriculumRef,
                sourceReference,
                tags,
                examples,
                concepts,
                contexts,
                goodActions,
                wrongActions,
                concerns,
                trueClaims,
                falseClaims,
                fillCards,
                missionTitle + "에서는 확인하고 비교하는 태도가 중요해요. 생활 속 AI를 쓸 때도 같은 기준을 떠올려 보세요.",
                "이 문장은 AI를 지나치게 믿거나 안전 규칙을 놓친 생각이에요. AI를 쓸 때는 자료와 상황을 함께 확인해야 해요.",
                "가장 알맞은 선택은 안전하게 확인하고 목적에 맞게 고치는 행동이에요. AI는 도움이 되는 도구지만 마지막 판단은 사람이 해야 해요.",
                "피해야 할 행동은 확인 없이 믿거나 개인정보와 책임을 가볍게 여기는 행동이에요. 좋은 AI 사용은 안전, 확인, 공정성을 함께 봅니다.",
                "상황형 문제에서는 누가 영향을 받는지, 무엇을 확인해야 하는지 함께 살펴야 해요. AI 결과를 그대로 따르기보다 사람의 판단을 더해야 합니다."
        );
    }

    private static String[] examples(String... values) {
        return values;
    }

    private static String[] concepts(String... values) {
        return values;
    }

    private static String[] contexts(String... values) {
        return values;
    }

    private static String[] good(String... values) {
        return values;
    }

    private static String[] wrong(String... values) {
        return values;
    }

    private static String[] concerns(String... values) {
        return values;
    }

    private static String[] trueClaims(String... values) {
        return values;
    }

    private static String[] falseClaims(String... values) {
        return values;
    }

    private static String[] tags(String... values) {
        return values;
    }

    private static FillCard fill(String pattern, String answer, String distractorA, String distractorB, String distractorC, String explanation) {
        return new FillCard(pattern, answer, new String[]{distractorA, distractorB, distractorC}, explanation);
    }

    private static Map<String, Object> object(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    private record FillCard(String pattern, String answer, String[] distractors, String explanation) {
    }

    private record Mission(
            String code,
            int stage,
            String stageTitle,
            String missionTitle,
            String summary,
            String curriculumRef,
            String sourceReference,
            String[] tags,
            String[] examples,
            String[] concepts,
            String[] contexts,
            String[] goodActions,
            String[] wrongActions,
            String[] concerns,
            String[] trueClaims,
            String[] falseClaims,
            FillCard[] fillCards,
            String positiveExplanation,
            String correctionExplanation,
            String actionExplanation,
            String avoidExplanation,
            String situationExplanation
    ) {
        Map<String, Object> toJson() {
            return object(
                    "missionCode", code,
                    "stage", stage,
                    "stageTitle", stageTitle,
                    "missionTitle", missionTitle,
                    "questionCount", 66,
                    "missionSummary", summary,
                    "curriculumRef", curriculumRef,
                    "sourceReference", sourceReference,
                    "baseQuestionCount", 10,
                    "packsPerMission", 6
            );
        }
    }

    private record Question(
            String externalId,
            String missionCode,
            int stage,
            String stageTitle,
            String missionTitle,
            String type,
            String question,
            List<String> options,
            Object answer,
            String explanation,
            List<String> contentTags,
            String curriculumRef,
            String sourceType,
            String generationPhase,
            String sourceReference,
            String difficulty,
            int packNo
    ) {
        Map<String, Object> toJson() {
            return object(
                    "externalId", externalId,
                    "missionCode", missionCode,
                    "stage", stage,
                    "stageTitle", stageTitle,
                    "missionTitle", missionTitle,
                    "type", type,
                    "question", question,
                    "options", options,
                    "answer", answer,
                    "explanation", explanation,
                    "contentTags", contentTags,
                    "curriculumRef", curriculumRef,
                    "sourceType", sourceType,
                    "generationPhase", generationPhase,
                    "sourceReference", sourceReference,
                    "difficulty", difficulty,
                    "packNo", packNo
            );
        }
    }

    private static final class AnswerRotator {
        private final int modulo;
        private int next;

        private AnswerRotator(int modulo) {
            this.modulo = modulo;
        }

        int next() {
            int value = next;
            next = (next + 1) % modulo;
            return value;
        }
    }

    private static final class Json {
        private Json() {
        }

        static String write(Object value) throws IOException {
            StringBuilder builder = new StringBuilder();
            writeValue(builder, value, 0);
            builder.append('\n');
            return builder.toString();
        }

        static String writeCompact(Object value) {
            StringBuilder builder = new StringBuilder();
            try {
                writeCompactValue(builder, value);
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
            return builder.toString();
        }

        @SuppressWarnings("unchecked")
        private static void writeValue(StringBuilder builder, Object value, int indent) throws IOException {
            if (value == null) {
                builder.append("null");
            } else if (value instanceof String string) {
                writeString(builder, string);
            } else if (value instanceof Number || value instanceof Boolean) {
                builder.append(value);
            } else if (value instanceof Map<?, ?> map) {
                builder.append("{");
                if (!map.isEmpty()) {
                    builder.append('\n');
                    int index = 0;
                    for (Map.Entry<String, Object> entry : ((Map<String, Object>) map).entrySet()) {
                        spaces(builder, indent + 2);
                        writeString(builder, entry.getKey());
                        builder.append(": ");
                        writeValue(builder, entry.getValue(), indent + 2);
                        if (++index < map.size()) {
                            builder.append(',');
                        }
                        builder.append('\n');
                    }
                    spaces(builder, indent);
                }
                builder.append("}");
            } else if (value instanceof Iterable<?> iterable) {
                builder.append("[");
                java.util.Iterator<?> iterator = iterable.iterator();
                if (iterator.hasNext()) {
                    builder.append('\n');
                    int index = 0;
                    List<Object> list = new ArrayList<>();
                    iterable.forEach(list::add);
                    for (Object item : list) {
                        spaces(builder, indent + 2);
                        writeValue(builder, item, indent + 2);
                        if (++index < list.size()) {
                            builder.append(',');
                        }
                        builder.append('\n');
                    }
                    spaces(builder, indent);
                }
                builder.append("]");
            } else {
                throw new IOException("unsupported json value: " + value.getClass());
            }
        }

        private static void writeString(StringBuilder builder, String value) {
            builder.append('"');
            for (int i = 0; i < value.length(); i++) {
                char ch = value.charAt(i);
                switch (ch) {
                    case '"' -> builder.append("\\\"");
                    case '\\' -> builder.append("\\\\");
                    case '\b' -> builder.append("\\b");
                    case '\f' -> builder.append("\\f");
                    case '\n' -> builder.append("\\n");
                    case '\r' -> builder.append("\\r");
                    case '\t' -> builder.append("\\t");
                    default -> {
                        if (ch < 0x20) {
                            builder.append("\\u%04x".formatted((int) ch));
                        } else {
                            builder.append(ch);
                        }
                    }
                }
            }
            builder.append('"');
        }

        @SuppressWarnings("unchecked")
        private static void writeCompactValue(StringBuilder builder, Object value) throws IOException {
            if (value == null) {
                builder.append("null");
            } else if (value instanceof String string) {
                writeString(builder, string);
            } else if (value instanceof Number || value instanceof Boolean) {
                builder.append(value);
            } else if (value instanceof Map<?, ?> map) {
                builder.append("{");
                int index = 0;
                for (Map.Entry<String, Object> entry : ((Map<String, Object>) map).entrySet()) {
                    writeString(builder, entry.getKey());
                    builder.append(":");
                    writeCompactValue(builder, entry.getValue());
                    if (++index < map.size()) {
                        builder.append(',');
                    }
                }
                builder.append("}");
            } else if (value instanceof Iterable<?> iterable) {
                List<Object> list = new ArrayList<>();
                iterable.forEach(list::add);
                builder.append("[");
                for (int index = 0; index < list.size(); index++) {
                    writeCompactValue(builder, list.get(index));
                    if (index < list.size() - 1) {
                        builder.append(',');
                    }
                }
                builder.append("]");
            } else {
                throw new IOException("unsupported json value: " + value.getClass());
            }
        }

        private static void spaces(StringBuilder builder, int count) {
            builder.append(" ".repeat(count));
        }
    }

    private static final class Sql {
        private Sql() {
        }

        static String write(List<Mission> missions, List<Question> questions) {
            Map<String, UUID> missionIds = new LinkedHashMap<>();
            for (Mission mission : missions) {
                missionIds.put(mission.code, uuid("mission:" + mission.code));
            }
            StringBuilder sql = new StringBuilder();
            sql.append("-- Generated from question source PDFs and AI literacy reform brief; adapted for current backend schema\n");
            sql.append("-- Question count: 1056; questions are selected at runtime by missionId + starLevel ratio.\n");
            sql.append("-- Star-level ratios: starLevel 1 = 7 LOW / 2 MEDIUM / 1 HIGH, starLevel 2 = 3 / 5 / 2, starLevel 3 = 2 / 3 / 5.\n\n");
            sql.append("BEGIN;\n\n");
            appendMissions(sql, missions, missionIds);
            appendMissionSets(sql, missions, missionIds);
            appendQuestions(sql, questions, missionIds);
            appendAnswerKeys(sql, questions);
            sql.append("\nCOMMIT;\n");
            return sql.toString();
        }

        private static void appendMissions(StringBuilder sql, List<Mission> missions, Map<String, UUID> missionIds) {
            sql.append("INSERT INTO public.missions (id, stage, title, mission_code, description, unlock_condition, is_active) VALUES\n");
            for (int index = 0; index < missions.size(); index++) {
                Mission mission = missions.get(index);
                sql.append("    ('").append(missionIds.get(mission.code)).append("', ")
                        .append(mission.stage).append(", ")
                        .append(sqlString(mission.missionTitle)).append(", ")
                        .append(sqlString(mission.code)).append(", ")
                        .append(sqlString(mission.summary)).append(", NULL, TRUE)");
                sql.append(index < missions.size() - 1 ? ",\n" : "\n");
            }
            sql.append("ON CONFLICT (id) DO UPDATE SET\n")
                    .append("    stage = EXCLUDED.stage,\n")
                    .append("    title = EXCLUDED.title,\n")
                    .append("    mission_code = EXCLUDED.mission_code,\n")
                    .append("    description = EXCLUDED.description,\n")
                    .append("    unlock_condition = EXCLUDED.unlock_condition,\n")
                    .append("    is_active = TRUE;\n\n");
            sql.append("UPDATE public.missions\n")
                    .append("SET is_active = FALSE\n")
                    .append("WHERE mission_code IS NULL OR mission_code NOT IN (");
            appendSqlStrings(sql, missions.stream().map(mission -> mission.code).toList());
            sql.append(");\n\n");
        }

        private static void appendMissionSets(StringBuilder sql, List<Mission> missions, Map<String, UUID> missionIds) {
            List<String> setIds = new ArrayList<>();
            sql.append("INSERT INTO public.mission_sets (set_id, mission_id, mission_code, star_level, variant_no, stage, title, description, display_order, is_active) VALUES\n");
            int row = 0;
            for (Mission mission : missions) {
                int missionNo = Integer.parseInt(mission.code.substring(3));
                for (int setNo = 1; setNo <= 6; setNo++) {
                    String setId = mission.code + "-L" + setNo;
                    setIds.add(setId);
                    int starLevel = (setNo + 1) / 2;
                    int variantNo = setNo % 2 == 1 ? 1 : 2;
                    int displayOrder = mission.stage * 1000 + missionNo * 10 + setNo;
                    sql.append("    (").append(sqlString(setId)).append(", '")
                            .append(missionIds.get(mission.code)).append("', ")
                            .append(sqlString(mission.code)).append(", ")
                            .append(starLevel).append(", ")
                            .append(variantNo).append(", ")
                            .append(mission.stage).append(", ")
                            .append(sqlString(mission.missionTitle)).append(", ")
                            .append(sqlString(mission.summary)).append(", ")
                            .append(displayOrder).append(", TRUE)");
                    sql.append(++row < missions.size() * 6 ? ",\n" : "\n");
                }
            }
            sql.append("ON CONFLICT (set_id) DO UPDATE SET\n")
                    .append("    mission_id = EXCLUDED.mission_id,\n")
                    .append("    mission_code = EXCLUDED.mission_code,\n")
                    .append("    star_level = EXCLUDED.star_level,\n")
                    .append("    variant_no = EXCLUDED.variant_no,\n")
                    .append("    stage = EXCLUDED.stage,\n")
                    .append("    title = EXCLUDED.title,\n")
                    .append("    description = EXCLUDED.description,\n")
                    .append("    display_order = EXCLUDED.display_order,\n")
                    .append("    is_active = TRUE;\n\n");
            sql.append("UPDATE public.mission_sets\n")
                    .append("SET is_active = FALSE\n")
                    .append("WHERE set_id NOT IN (");
            appendSqlStrings(sql, setIds);
            sql.append(");\n\n");
        }

        private static void appendQuestions(StringBuilder sql, List<Question> questions, Map<String, UUID> missionIds) {
            sql.append("INSERT INTO public.question_bank (id, mission_id, set_id, question_type, prompt, options, content_tags, curriculum_ref, difficulty, source_type, generation_phase, pack_no, question_pool_status, is_active) VALUES\n");
            for (int index = 0; index < questions.size(); index++) {
                Question question = questions.get(index);
                sql.append("    ('").append(uuid("question:" + question.externalId)).append("', '")
                        .append(missionIds.get(question.missionCode)).append("', NULL, '")
                        .append(question.type).append("', ")
                        .append(sqlString(question.question)).append(", ")
                        .append(question.options == null ? "NULL" : sqlString(Json.writeCompact(question.options)) + "::jsonb")
                        .append(", ")
                        .append(sqlString(Json.writeCompact(question.contentTags))).append("::jsonb, ")
                        .append(sqlString(question.curriculumRef)).append(", '")
                        .append(question.difficulty).append("', '")
                        .append(question.sourceType).append("', '")
                        .append(question.generationPhase).append("', ")
                        .append(question.packNo).append(", 'ACTIVE', TRUE)");
                sql.append(index < questions.size() - 1 ? ",\n" : "\n");
            }
            sql.append("ON CONFLICT (id) DO UPDATE SET\n")
                    .append("    mission_id = EXCLUDED.mission_id, set_id = EXCLUDED.set_id, question_type = EXCLUDED.question_type, prompt = EXCLUDED.prompt, options = EXCLUDED.options, content_tags = EXCLUDED.content_tags, curriculum_ref = EXCLUDED.curriculum_ref, difficulty = EXCLUDED.difficulty, source_type = EXCLUDED.source_type, generation_phase = EXCLUDED.generation_phase, pack_no = EXCLUDED.pack_no, question_pool_status = EXCLUDED.question_pool_status, is_active = TRUE;\n\n");
            sql.append("UPDATE public.question_bank\n")
                    .append("SET is_active = FALSE,\n")
                    .append("    question_pool_status = 'RETIRED'\n")
                    .append("WHERE mission_id IN (");
            appendQuotedUuids(sql, missionIds.values().stream().toList());
            sql.append(")\n  AND id NOT IN (");
            appendQuotedUuids(sql, questions.stream().map(question -> uuid("question:" + question.externalId)).toList());
            sql.append(");\n\n");
        }

        private static void appendAnswerKeys(StringBuilder sql, List<Question> questions) {
            sql.append("INSERT INTO private.question_answer_keys (question_id, answer_payload, explanation) VALUES\n");
            for (int index = 0; index < questions.size(); index++) {
                Question question = questions.get(index);
                sql.append("    ('").append(uuid("question:" + question.externalId)).append("', ")
                        .append(sqlString(Json.writeCompact(answerPayload(question)))).append("::jsonb, ")
                        .append(sqlString(question.explanation)).append(")");
                sql.append(index < questions.size() - 1 ? ",\n" : "\n");
            }
            sql.append("ON CONFLICT (question_id) DO UPDATE SET\n")
                    .append("    answer_payload = EXCLUDED.answer_payload,\n")
                    .append("    explanation = EXCLUDED.explanation;\n");
        }

        private static Object answerPayload(Question question) {
            if (("MULTIPLE".equals(question.type) || "SITUATION".equals(question.type)) && question.answer instanceof Integer index) {
                return index + 1;
            }
            if ("FILL".equals(question.type) && question.answer instanceof List<?> indexes) {
                return indexes.stream()
                        .map(index -> index instanceof Integer value ? value + 1 : index)
                        .toList();
            }
            return question.answer;
        }

        private static void appendSqlStrings(StringBuilder sql, List<String> values) {
            for (int index = 0; index < values.size(); index++) {
                if (index > 0) {
                    sql.append(", ");
                }
                sql.append(sqlString(values.get(index)));
            }
        }

        private static void appendQuotedUuids(StringBuilder sql, List<UUID> values) {
            for (int index = 0; index < values.size(); index++) {
                if (index > 0) {
                    sql.append(", ");
                }
                sql.append("'").append(values.get(index)).append("'");
            }
        }

        private static String sqlString(String value) {
            if (value == null) {
                return "NULL";
            }
            String tag = value.contains("$aimong$") ? "$aimong_sql$" : "$aimong$";
            return tag + value + tag;
        }

        private static UUID uuid(String seed) {
            return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
        }
    }
}
