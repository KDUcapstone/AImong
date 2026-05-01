package com.aimong.backend.domain.mission.service.generation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SafetyValidator {

    private static final List<String> IDENTITY_KEYWORDS = List.of(
            "실명", "본명", "전체 이름", "주소", "학교명", "학교 이름", "생년월일", "전화번호", "연락처", "비밀번호", "계정", "로그인 아이디", "주민등록번호",
            "real name", "full name", "address", "school name", "birthday", "phone number", "contact", "password", "account"
    );
    private static final List<String> BIOMETRIC_KEYWORDS = List.of(
            "얼굴 사진", "얼굴", "지문", "목소리", "음성 파일", "음성 녹음", "생체정보",
            "face photo", "fingerprint", "voice recording", "biometric"
    );
    private static final List<String> INPUT_ACTION_KEYWORDS = List.of(
            "입력", "적어", "쓰기", "써", "보내", "공유", "올려", "업로드", "제출",
            "enter", "write", "send", "share", "upload", "submit"
    );
    private static final List<String> RECORD_ACTION_KEYWORDS = List.of(
            "촬영", "녹음", "캡처", "기록", "보내", "공유", "올려", "업로드",
            "record", "capture", "take a photo", "take photo", "send", "share", "upload"
    );
    private static final List<String> UNAUTHORIZED_CONTEXT_KEYWORDS = List.of(
            "허락 없이", "동의 없이", "몰래", "무단으로",
            "without permission", "without consent", "secretly"
    );
    private static final List<String> CAUTION_CONTEXT_KEYWORDS = List.of(
            "하지 마", "하면 안", "올리면 안", "입력하면 안", "공유하면 안", "보내면 안", "업로드하면 안",
            "멈추자", "조심", "보호", "숨겨", "공개하지", "입력하지", "보내지", "공유하지", "업로드하지",
            "안전", "지켜", "허락 없는", "do not", "don't", "should not", "stop", "protect", "be careful", "not safe"
    );
    private static final List<String> SCENARIO_REVIEW_KEYWORDS = List.of(
            "하려 해요", "하려고 해요", "올리려 해요", "보내려 해요", "어떻게 말", "어떻게 설명", "어떻게 이해",
            "무엇일까요", "어떤 생각", "가장 알맞", "가장 적절", "고르면", "고르세요",
            "판단해 보세요", "생각해 보세요", "빈칸을 채워 보세요", "다음 중", "예로"
    );
    private static final List<String> HARMFUL_KEYWORDS = List.of(
            "죽여", "때려", "폭력", "성적", "혐오", "차별", "멍청", "바보",
            "kill", "hit", "violence", "sexual", "hate", "discriminate", "stupid"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b01[0-9]-?[0-9]{3,4}-?[0-9]{4}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);

    public ValidationSubResult validate(StructuredQuestionSchema candidate) {
        String fullText = ValidationTextUtils.joinCandidateText(candidate);
        String questionText = candidate.question() == null ? "" : candidate.question();
        String promptText = String.join("\n",
                questionText,
                candidate.options() == null ? "" : String.join("\n", candidate.options()));
        String questionLower = questionText.toLowerCase(Locale.ROOT);
        String rawLower = fullText.toLowerCase(Locale.ROOT);
        String promptLower = promptText.toLowerCase(Locale.ROOT);
        String questionNormalized = ValidationTextUtils.normalize(questionText);
        String normalized = ValidationTextUtils.normalize(fullText);
        String promptNormalized = ValidationTextUtils.normalize(promptText);
        String questionCompact = compact(questionLower);
        String compact = compact(rawLower);
        String promptCompact = compact(promptLower);

        List<String> hardFails = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> repairHints = new ArrayList<>();

        if (PHONE_PATTERN.matcher(fullText).find()) {
            hardFails.add("safety.phone_number_detected");
        }
        if (EMAIL_PATTERN.matcher(fullText).find()) {
            hardFails.add("safety.email_detected");
        }
        if (containsSensitiveInputPrompt(questionLower, questionNormalized, questionCompact)) {
            hardFails.add("safety.personal_data_request");
            repairHints.add("Replace real identifiers with fictional or anonymous placeholders.");
        }
        if (containsBiometricUpload(questionLower, questionNormalized, questionCompact)) {
            hardFails.add("safety.biometric_upload_request");
            repairHints.add("Do not ask children to upload face, voice, or fingerprint data.");
        }
        if (containsUnauthorizedRecording(questionLower, questionNormalized, questionCompact)) {
            hardFails.add("safety.unauthorized_recording");
            repairHints.add("Use consent-first classroom scenarios.");
        }
        if (containsAny(rawLower, normalized, compact, HARMFUL_KEYWORDS)) {
            hardFails.add("safety.harmful_or_abusive_content");
        }
        if (normalized.contains("무조건") || normalized.contains("절대") || normalized.contains("always") || normalized.contains("never")) {
            warnings.add("safety.absolute_language");
        }

        int score = hardFails.isEmpty() ? Math.max(0, 100 - warnings.size() * 10) : 0;
        return new ValidationSubResult(score, List.copyOf(hardFails), List.copyOf(warnings), List.copyOf(repairHints));
    }

    private boolean containsSensitiveInputPrompt(
            String questionLower,
            String questionNormalized,
            String questionCompact
    ) {
        boolean cautionContext = containsAny(questionLower, questionNormalized, questionCompact, CAUTION_CONTEXT_KEYWORDS);
        boolean scenarioReview = containsAny(questionLower, questionNormalized, questionCompact, SCENARIO_REVIEW_KEYWORDS);
        return containsAny(questionLower, questionNormalized, questionCompact, IDENTITY_KEYWORDS)
                && containsAny(questionLower, questionNormalized, questionCompact, INPUT_ACTION_KEYWORDS)
                && !cautionContext
                && !scenarioReview;
    }

    private boolean containsBiometricUpload(
            String questionLower,
            String questionNormalized,
            String questionCompact
    ) {
        boolean cautionContext = containsAny(questionLower, questionNormalized, questionCompact, CAUTION_CONTEXT_KEYWORDS);
        boolean scenarioReview = containsAny(questionLower, questionNormalized, questionCompact, SCENARIO_REVIEW_KEYWORDS);
        return containsAny(questionLower, questionNormalized, questionCompact, BIOMETRIC_KEYWORDS)
                && containsAny(questionLower, questionNormalized, questionCompact, INPUT_ACTION_KEYWORDS)
                && !cautionContext
                && !scenarioReview;
    }

    private boolean containsUnauthorizedRecording(
            String questionLower,
            String questionNormalized,
            String questionCompact
    ) {
        boolean cautionContext = containsAny(questionLower, questionNormalized, questionCompact, CAUTION_CONTEXT_KEYWORDS);
        boolean scenarioReview = containsAny(questionLower, questionNormalized, questionCompact, SCENARIO_REVIEW_KEYWORDS);
        return containsAny(questionLower, questionNormalized, questionCompact, UNAUTHORIZED_CONTEXT_KEYWORDS)
                && containsAny(questionLower, questionNormalized, questionCompact, RECORD_ACTION_KEYWORDS)
                && !cautionContext
                && !scenarioReview;
    }

    private boolean containsAny(String rawLower, String normalized, String compact, List<String> keywords) {
        return keywords.stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .anyMatch(keyword -> rawLower.contains(keyword) || normalized.contains(keyword) || compact.contains(compact(keyword)));
    }

    private String compact(String value) {
        return value == null ? "" : value.replace(" ", "");
    }
}
