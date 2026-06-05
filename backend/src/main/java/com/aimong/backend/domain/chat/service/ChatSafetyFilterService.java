package com.aimong.backend.domain.chat.service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class ChatSafetyFilterService {

    private static final FilterDecision ALLOWED = new FilterDecision(true, null, null);

    private static final List<SafetyRule> RULES = List.of(
            new SafetyRule(
                    SafetyCategory.SELF_HARM,
                    "지금 마음이 많이 힘들 수 있어요. 혼자 해결하려 하지 말고 가까운 보호자나 선생님에게 바로 알려주세요. 지금 위험하다면 119 또는 112에 도움을 요청하세요.",
                    "자살|자해|죽고\\s*싶|목숨을?\\s*끊|사라지고\\s*싶|kill\\s+myself|suicide|self[-\\s]?harm"
            ),
            new SafetyRule(
                    SafetyCategory.PROMPT_INJECTION,
                    "규칙을 바꾸거나 숨은 지시를 보려는 요청은 도와줄 수 없어요. 궁금한 내용을 안전한 질문으로 바꿔서 물어봐 주세요.",
                    "시스템\\s*프롬프트|개발자\\s*프롬프트|프롬프트를?\\s*보여|규칙을?\\s*무시|이전\\s*지시를?\\s*무시|숨은\\s*지시|탈옥|jailbreak|ignore\\s+(all\\s+)?(previous|above)\\s+instructions|system\\s+prompt|developer\\s+prompt|show\\s+me\\s+your\\s+prompt"
            ),
            new SafetyRule(
                    SafetyCategory.SEXUAL,
                    "그 내용은 초등학생에게 맞지 않아 도와줄 수 없어요. 안전한 주제로 바꿔서 물어봐 주세요.",
                    "야한|성인물|음란|나체|노출|포르노|섹스|19금|porn|nude|sexually"
            ),
            new SafetyRule(
                    SafetyCategory.VIOLENCE,
                    "다치게 하거나 위협하는 방법은 도와줄 수 없어요. 갈등을 안전하게 해결하는 방법을 같이 생각해볼게요.",
                    "죽이는\\s*법|때리는\\s*법|해치는\\s*법|살해|테러|폭탄|흉기|칼로\\s*(찌르|위협)|총으로\\s*(쏘|위협)|how\\s+to\\s+(kill|hurt|attack)"
            ),
            new SafetyRule(
                    SafetyCategory.ILLEGAL,
                    "위험하거나 불법적인 행동은 도와줄 수 없어요. 안전하고 책임 있는 방법을 물어보면 도와줄게요.",
                    "마약|도박\\s*(사이트|하는\\s*법|추천)|해킹\\s*(하는\\s*법|툴|방법)|비밀번호를?\\s*(훔치|알아내|뚫)|폭발물\\s*(제조|만드는\\s*법)|drug\\s+dealing|gambling\\s+site|hack\\s+(an|a)?\\s*account|steal\\s+(a\\s+)?password"
            ),
            new SafetyRule(
                    SafetyCategory.ABUSIVE,
                    "상처 주는 말은 도와줄 수 없어요. 마음을 표현하되 상대를 해치지 않는 말로 바꿔볼까요?",
                    "시발|씨발|병신|개새끼|좆|ㅅㅂ|ㅂㅅ|fuck|shit|bitch"
            )
    );

    public FilterDecision evaluate(String text, boolean imageRequested) {
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return ALLOWED;
        }

        for (SafetyRule rule : RULES) {
            if (rule.matches(normalized)) {
                return new FilterDecision(false, rule.category(), imageReply(rule.reply(), imageRequested));
            }
        }
        return ALLOWED;
    }

    private String imageReply(String reply, boolean imageRequested) {
        if (!imageRequested) {
            return reply;
        }
        return "그 이미지는 만들 수 없어요. " + reply;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    public enum SafetyCategory {
        SELF_HARM,
        PROMPT_INJECTION,
        SEXUAL,
        VIOLENCE,
        ILLEGAL,
        ABUSIVE
    }

    public record FilterDecision(
            boolean allowed,
            SafetyCategory category,
            String safeReply
    ) {
    }

    private record SafetyRule(
            SafetyCategory category,
            String reply,
            Pattern pattern
    ) {
        private SafetyRule(SafetyCategory category, String reply, String regex) {
            this(category, reply, Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
        }

        private boolean matches(String text) {
            return pattern.matcher(text).find();
        }
    }
}
