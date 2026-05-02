package com.aimong.backend.domain.mission.service.generation;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ChildSafetyFilter {

    private static final List<String> BANNED_PATTERNS = List.of(
            "실제 이름",
            "실제 주소",
            "실제 연락처",
            "전화번호를 입력",
            "집 주소",
            "주민등록번호",
            "얼굴 사진을 올려",
            "지문을 올려",
            "목소리 파일을 올려",
            "비밀번호를 입력"
    );

    public List<String> validate(StructuredQuestionSchema candidate) {
        String text = normalize(candidate.question()) + "\n"
                + normalize(candidate.explanation()) + "\n"
                + String.join("\n", candidate.options() == null ? List.of() : candidate.options());

        return BANNED_PATTERNS.stream()
                .filter(pattern -> text.contains(pattern.toLowerCase(Locale.ROOT)))
                .map(pattern -> "child-safety banned pattern: " + pattern)
                .toList();
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
