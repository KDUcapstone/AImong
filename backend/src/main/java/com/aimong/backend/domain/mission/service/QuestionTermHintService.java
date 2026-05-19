package com.aimong.backend.domain.mission.service;

import com.aimong.backend.domain.mission.dto.TermHintResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class QuestionTermHintService {

    private static final int MAX_HINTS = 3;
    private static final Map<String, String> TERM_DICTIONARY = new LinkedHashMap<>();

    static {
        TERM_DICTIONARY.put("인공지능", "사람처럼 배우고 판단하도록 만든 컴퓨터 기술이에요.");
        TERM_DICTIONARY.put("AI", "사람처럼 배우고 판단하도록 만든 컴퓨터 기술이에요.");
        TERM_DICTIONARY.put("자료", "컴퓨터가 배우거나 판단할 때 참고하는 글, 숫자, 그림 같은 정보예요.");
        TERM_DICTIONARY.put("데이터", "컴퓨터가 배우거나 판단할 때 참고하는 글, 숫자, 그림 같은 정보예요.");
        TERM_DICTIONARY.put("편향", "한쪽으로 치우쳐서 생각하거나 판단하는 것을 말해요.");
        TERM_DICTIONARY.put("개인정보", "이름, 전화번호, 주소처럼 나를 알아볼 수 있게 하는 정보예요.");
        TERM_DICTIONARY.put("출처", "정보가 처음 나온 곳이에요.");
        TERM_DICTIONARY.put("인식", "보고 듣거나 읽은 것을 알아차리는 것을 말해요.");
        TERM_DICTIONARY.put("알고리즘", "문제를 해결하기 위해 정해 둔 단계와 방법이에요.");
    }

    public List<TermHintResponse> findHints(String prompt, List<String> choices) {
        String searchText = buildSearchText(prompt, choices);
        if (searchText.isBlank()) {
            return List.of();
        }
        return TERM_DICTIONARY.entrySet()
                .stream()
                .filter(entry -> searchText.contains(entry.getKey()))
                .limit(MAX_HINTS)
                .map(entry -> new TermHintResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    private String buildSearchText(String prompt, List<String> choices) {
        StringBuilder builder = new StringBuilder(prompt == null ? "" : prompt);
        if (choices != null) {
            choices.forEach(choice -> builder.append(' ').append(choice == null ? "" : choice));
        }
        return builder.toString();
    }
}
