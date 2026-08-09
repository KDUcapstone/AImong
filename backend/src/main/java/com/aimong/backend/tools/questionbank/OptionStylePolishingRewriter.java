package com.aimong.backend.tools.questionbank;

import java.util.List;
import java.util.Map;

public final class OptionStylePolishingRewriter {

    private static final Map<String, String> REPLACEMENTS = Map.ofEntries(
            Map.entry("인공지능은 사람의 마음을 그대로 읽어요.", "사람 마음을 그대로 읽어요."),
            Map.entry("인공지능은 사람의 마음을 바로 읽어요.", "사람 마음을 바로 읽어요."),
            Map.entry("인공지능은 사람의 마음을 그냥 읽어요.", "사람 마음을 그냥 읽어요."),
            Map.entry("인공지능은 한 번 배운 뒤에는 절대 틀리지 않아요.", "한 번 배우면 절대 틀리지 않아요."),
            Map.entry("인공지능은 그림을 보지 않고 무작정 정답을 말해요.", "그림을 안 보고 무작정 정답을 말해요."),
            Map.entry("인공지능은 그림을 보지 않고 무작정 정답을 말해 봐요.", "그림을 안 보고 무작정 정답을 말해 봐요."),
            Map.entry("인공지능은 그림을 보지 않고 무작정 정답을 말합니다.", "그림을 안 보고 무작정 정답을 말해요."),
            Map.entry("인공지능은 선의 특징을 보고 무엇인지 짐작해요.", "선의 특징을 보고 무엇인지 짐작해요."),
            Map.entry("AI는 숫자만 다루는 도구예요.", "숫자만 다루는 도구예요."),
            Map.entry("AI는 전기 없이도 움직여요.", "전기 없이도 움직여요."),
            Map.entry("AI는 데이터를 보고 기준을 배워요.", "데이터를 보고 기준을 배워요."),
            Map.entry("계산기에는 화면이 없어요.", "계산기 화면이 없어도 같지 않아요."),
            Map.entry("사진에 적힌 배경색 정보", "배경색 정보"),
            Map.entry("사진에 붙인 과일 이름표", "과일 이름표"),
            Map.entry("사진 파일에 적힌 크기 정보", "파일 크기 정보"),
            Map.entry("사진 옆에 적힌 날짜 정보", "촬영 날짜 정보"),
            Map.entry("딥러닝은 데이터로 배우는 모델이라고 해요.", "데이터로 배우는 모델이라고 해요."),
            Map.entry("딥러닝은 사람처럼 감정을 느낀다고 말해요.", "사람처럼 감정을 느낀다고 말해요."),
            Map.entry("딥러닝은 한 번 배우면 절대 틀리지 않는다고 말해요.", "한 번 배우면 절대 틀리지 않는다고 말해요."),
            Map.entry("딥러닝은 전기가 없으면 똑똑하지 않다고 말해요.", "전기가 없으면 똑똑하지 않다고 말해요."),
            Map.entry("이 말은 어디에서 확인할 수 있을까?", "어디에서 확인할 수 있을까?"),
            Map.entry("이 말이 친구를 놀라게 할까?", "친구를 놀라게 하려는 말일까?"),
            Map.entry("이 말이 제일 길까?", "제일 눈길 끄는 말일까?"),
            Map.entry("이 말이 가장 멋지게 들릴까?", "가장 그럴듯해 보일까?")
    );

    public List<String> rewrite(AuditQuestion question) {
        if (question.options() == null) {
            return null;
        }
        return question.options().stream()
                .map(option -> REPLACEMENTS.getOrDefault(option, option))
                .toList();
    }
}
