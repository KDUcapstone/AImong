package com.aimong.backend.domain.mission.service.generation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SafetyValidator {

    private static final List<String> SENSITIVE_INPUT_KEYWORDS = List.of(
            "실제 이름", "이름", "주소", "전화번호", "연락처", "비밀번호", "계정", "아이디",
            "주민등록번호", "얼굴 사진", "얼굴", "지문", "목소리", "음성", "생체정보",
            "친구의 개인정보", "가족의 개인정보",
            "real name", "address", "phone number", "contact", "password", "account", "id",
            "face photo", "face", "fingerprint", "voice", "biometric"
    );
    private static final List<String> UNSAFE_ACTION_KEYWORDS = List.of(
            "업로드", "입력", "써", "적어", "보내", "공유", "올려", "녹음", "촬영", "찍어",
            "upload", "enter", "write", "send", "share", "record", "take a photo", "take photo"
    );
    private static final List<String> UNSAFE_CONTEXT_KEYWORDS = List.of(
            "허락 없이", "몰래", "비밀로", "무단으로",
            "without permission", "secretly", "without consent"
    );
    private static final List<String> HARMFUL_KEYWORDS = List.of(
            "죽", "때리", "폭력", "학대", "성적", "혐오", "차별", "멍청", "바보",
            "kill", "hit", "violence", "abuse", "sexual", "hate", "discriminate", "stupid"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b01[0-9]-?[0-9]{3,4}-?[0-9]{4}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);

    public ValidationSubResult validate(StructuredQuestionSchema candidate) {
        String text = ValidationTextUtils.joinCandidateText(candidate);
        String promptText = String.join("\n",
                candidate.question() == null ? "" : candidate.question(),
                candidate.options() == null ? "" : String.join("\n", candidate.options()));
        String promptLower = promptText.toLowerCase(Locale.ROOT);
        String rawLower = text.toLowerCase(Locale.ROOT);
        String normalized = ValidationTextUtils.normalize(text);
        String promptNormalized = ValidationTextUtils.normalize(promptText);
        String compact = compact(rawLower);
        String promptCompact = compact(promptLower);

        List<String> hardFails = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> repairHints = new ArrayList<>();

        if (PHONE_PATTERN.matcher(text).find()) {
            hardFails.add("safety.phone_number_detected");
        }
        if (EMAIL_PATTERN.matcher(text).find()) {
            hardFails.add("safety.email_detected");
        }
        if (containsSensitiveInputPrompt(promptLower, promptNormalized, promptCompact)) {
            hardFails.add("safety.personal_data_request");
            repairHints.add("Replace real identifiers with fictional or anonymous placeholders.");
        }
        if (containsBiometricUpload(rawLower, normalized, compact)) {
            hardFails.add("safety.biometric_upload_request");
            repairHints.add("Do not ask children to upload face, voice, or fingerprint data.");
        }
        if (containsUnauthorizedRecording(rawLower, normalized, compact)) {
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

    private boolean containsSensitiveInputPrompt(String rawLower, String normalized, String compact) {
        boolean cautionContext = containsAny(rawLower, normalized, compact, List.of(
                "넣지 말", "넣으면 안", "쓰면 안", "적지 말", "조심", "안전", "보호",
                "do not", "don't", "should not", "not safe", "be careful", "protect"
        ));
        return containsAny(rawLower, normalized, compact, SENSITIVE_INPUT_KEYWORDS)
                && containsAny(rawLower, normalized, compact, UNSAFE_ACTION_KEYWORDS)
                && !cautionContext;
    }

    private boolean containsBiometricUpload(String rawLower, String normalized, String compact) {
        return containsAny(rawLower, normalized, compact, List.of("얼굴", "얼굴 사진", "지문", "목소리", "음성", "생체정보", "face", "face photo", "fingerprint", "voice", "biometric"))
                && containsAny(rawLower, normalized, compact, List.of("업로드", "올려", "보내", "녹음", "촬영", "찍어", "upload", "send", "record", "take photo"));
    }

    private boolean containsUnauthorizedRecording(String rawLower, String normalized, String compact) {
        return containsAny(rawLower, normalized, compact, UNSAFE_CONTEXT_KEYWORDS)
                && containsAny(rawLower, normalized, compact, List.of("촬영", "녹음", "공유", "보내", "올려", "찍어", "record", "share", "send", "take photo"));
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
