package com.aimong.backend.tools.questionbank;

import com.aimong.backend.domain.mission.entity.QuestionType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class QuestionBankSqlExporter {

    private final ObjectMapper objectMapper;

    public QuestionBankSqlExporter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String export(QuestionBankDraft draft) {
        StringBuilder sql = new StringBuilder();
        Map<String, UUID> missionIds = new LinkedHashMap<>();
        draft.questions().forEach(question ->
                missionIds.computeIfAbsent(question.missionCode(), code -> uuid("mission:" + code))
        );

        sql.append("-- generated at ").append(Instant.now()).append('\n');
        sql.append("-- source: ").append(draft.sourceTitle()).append('\n').append('\n');

        sql.append("INSERT INTO missions (id, stage, title, description, unlock_condition, is_active) VALUES\n");
        int missionIndex = 0;
        for (QuestionDraft question : distinctMissionQuestions(draft)) {
            UUID missionId = missionIds.get(question.missionCode());
            sql.append("    ('").append(missionId).append("', ")
                    .append(question.stage()).append(", '")
                    .append(escape(question.missionTitle())).append("', '")
                    .append(escape(question.missionTitle())).append(" - ")
                    .append(escape(question.sourceReference())).append("', NULL, TRUE)");
            sql.append(missionIndex++ < missionIds.size() - 1 ? ",\n" : "\n");
        }
        sql.append("ON CONFLICT (id) DO NOTHING;\n\n");

        sql.append("INSERT INTO question_bank (id, mission_id, question_type, prompt, options_json, source_type, is_active) VALUES\n");
        for (int i = 0; i < draft.questions().size(); i++) {
            QuestionDraft question = draft.questions().get(i);
            sql.append("    ('").append(uuid("question:" + question.externalId())).append("', '")
                    .append(missionIds.get(question.missionCode())).append("', '")
                    .append(QuestionType.valueOf(question.type())).append("', '")
                    .append(escape(question.question())).append("', ")
                    .append(question.options() == null ? "NULL" : "'" + escape(toJson(question.options())) + "'")
                    .append(", '").append(question.sourceType()).append("', TRUE)");
            sql.append(i < draft.questions().size() - 1 ? ",\n" : "\n");
        }
        sql.append("ON CONFLICT (id) DO NOTHING;\n\n");

        sql.append("INSERT INTO private.question_answer_keys (question_id, answer_payload, explanation) VALUES\n");
        for (int i = 0; i < draft.questions().size(); i++) {
            QuestionDraft question = draft.questions().get(i);
            sql.append("    ('").append(uuid("question:" + question.externalId())).append("', '")
                    .append(escape(toAnswerPayload(question))).append("', '")
                    .append(escape(question.explanation())).append("')");
            sql.append(i < draft.questions().size() - 1 ? ",\n" : "\n");
        }
        sql.append("ON CONFLICT (question_id) DO NOTHING;\n");
        return sql.toString();
    }

    private java.util.List<QuestionDraft> distinctMissionQuestions(QuestionBankDraft draft) {
        Map<String, QuestionDraft> missions = new LinkedHashMap<>();
        draft.questions().forEach(question -> missions.putIfAbsent(question.missionCode(), question));
        return java.util.List.copyOf(missions.values());
    }

    private String toAnswerPayload(QuestionDraft question) {
        if ("MULTIPLE".equals(question.type()) || "SITUATION".equals(question.type())) {
            return toJson(question.options().get((Integer) question.answer()));
        }
        if ("FILL".equals(question.type())) {
            @SuppressWarnings("unchecked")
            int index = ((java.util.List<Integer>) question.answer()).getFirst();
            return toJson(question.options().get(index));
        }
        return toJson(question.answer());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String escape(String value) {
        return value.replace("'", "''");
    }

    private static UUID uuid(String seed) {
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}
