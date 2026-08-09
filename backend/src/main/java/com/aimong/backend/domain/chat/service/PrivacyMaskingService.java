package com.aimong.backend.domain.chat.service;

import com.aimong.backend.domain.privacy.entity.PrivacyDetectedType;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class PrivacyMaskingService {

    private static final String MASK = "[***]";
    private static final List<MaskingRule> RULES = List.of(
            new MaskingRule(PrivacyDetectedType.NAME, Pattern.compile("(내\\s*이름은|제\\s*이름은|이름\\s*[:=])\\s*([가-힣]{2,4}|[A-Za-z]{2,20}(\\s+[A-Za-z]{2,20}){0,2})")),
            new MaskingRule(PrivacyDetectedType.PHONE, Pattern.compile("\\b\\d{2,3}[-.\\s]?\\d{3,4}[-.\\s]?\\d{4}\\b")),
            new MaskingRule(PrivacyDetectedType.EMAIL, Pattern.compile("\\b[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}\\b")),
            new MaskingRule(PrivacyDetectedType.DATE, Pattern.compile("\\b\\d{4}[-.]\\d{1,2}[-.]\\d{1,2}\\b")),
            new MaskingRule(PrivacyDetectedType.URL, Pattern.compile("\\bhttps?://[^\\s]+\\b")),
            new MaskingRule(PrivacyDetectedType.AGE, Pattern.compile("\\b\\d{1,2}\\s*(살|세)\\b")),
            new MaskingRule(PrivacyDetectedType.ADDRESS, Pattern.compile("((서울|부산|대구|인천|광주|대전|울산|세종|경기|강원|충북|충남|전북|전남|경북|경남|제주)[^\\s,]{0,20}\\s*)?([가-힣]{1,20}(시|군|구)\\s*)?[가-힣0-9]{1,30}(로|길)\\s*\\d{1,5}(-\\d{1,5})?")),
            new MaskingRule(PrivacyDetectedType.ADDRESS, Pattern.compile("([가-힣]{1,20}(시|도)\\s*)?([가-힣]{1,20}(시|군|구)\\s*)?[가-힣]{1,20}(동|읍|면)\\s*\\d{1,5}(-\\d{1,5})?")),
            new MaskingRule(PrivacyDetectedType.SCHOOL, Pattern.compile("[가-힣A-Za-z0-9]{2,20}(초등학교|중학교|고등학교|학교)"))
    );

    public MaskingResult mask(String text) {
        String result = text == null ? "" : text;
        Set<PrivacyDetectedType> detectedTypes = EnumSet.noneOf(PrivacyDetectedType.class);

        for (MaskingRule rule : RULES) {
            if (rule.pattern().matcher(result).find()) {
                detectedTypes.add(rule.detectedType());
                result = rule.pattern().matcher(result).replaceAll(MASK);
            }
        }

        return new MaskingResult(result, detectedTypes);
    }

    private record MaskingRule(
            PrivacyDetectedType detectedType,
            Pattern pattern
    ) {
    }

    public record MaskingResult(
            String sanitizedMessage,
            Set<PrivacyDetectedType> detectedTypes
    ) {
    }
}
