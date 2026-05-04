package com.aimong.backend.tools.questionbank;

import com.aimong.backend.domain.mission.entity.QuestionType;
import com.aimong.backend.domain.mission.entity.GenerationPhase;
import com.aimong.backend.domain.mission.entity.QuestionPoolStatus;
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

        sql.append("INSERT INTO question_bank (id, mission_id, question_type, prompt, options, source_type, is_active) VALUES\n");
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

    public String exportServeBank(AuditQuestionBank bank) {
        StringBuilder sql = new StringBuilder();
        Map<String, UUID> missionIds = new LinkedHashMap<>();
        bank.questions().forEach(question ->
                missionIds.computeIfAbsent(question.missionCode(), code -> uuid("mission:" + code))
        );

        sql.append("-- generated at ").append(Instant.now()).append('\n');
        sql.append("-- source: ").append(bank.sourceTitle()).append('\n');
        sql.append("-- generation version: ").append(bank.generationVersion()).append('\n').append('\n');

        sql.append("INSERT INTO missions (id, stage, title, mission_code, description, unlock_condition, is_active) VALUES\n");
        java.util.List<AuditQuestion> missions = distinctMissionQuestions(bank);
        for (int index = 0; index < missions.size(); index++) {
            AuditQuestion question = missions.get(index);
            UUID missionId = missionIds.get(question.missionCode());
            String description = question.curriculumRef() == null || question.curriculumRef().isBlank()
                    ? bank.sourceReference()
                    : question.curriculumRef();
            sql.append("    ('").append(missionId).append("', ")
                    .append(question.stage()).append(", '")
                    .append(escape(question.missionTitle())).append("', '")
                    .append(escape(question.missionCode())).append("', '")
                    .append(escape(description)).append("', NULL, TRUE)");
            sql.append(index < missions.size() - 1 ? ",\n" : "\n");
        }
        sql.append("ON CONFLICT (id) DO UPDATE SET\n")
                .append("    stage = EXCLUDED.stage,\n")
                .append("    title = EXCLUDED.title,\n")
                .append("    mission_code = EXCLUDED.mission_code,\n")
                .append("    description = EXCLUDED.description,\n")
                .append("    is_active = TRUE;\n\n");

        sql.append("UPDATE missions SET is_active = FALSE WHERE mission_code IS NULL OR mission_code NOT IN (");
        appendMissionCodes(sql, missionIds);
        sql.append(");\n");
        sql.append("UPDATE question_bank SET is_active = FALSE WHERE mission_id IN (")
                .append("SELECT id FROM missions WHERE is_active = FALSE")
                .append(");\n\n");

        sql.append("INSERT INTO question_bank (")
                .append("id, mission_id, question_type, prompt, options, content_tags, curriculum_ref, difficulty, ")
                .append("source_type, generation_phase, pack_no, difficulty_band, question_pool_status, is_active")
                .append(") VALUES\n");
        for (int index = 0; index < bank.questions().size(); index++) {
            AuditQuestion question = bank.questions().get(index);
            sql.append("    ('").append(uuid("question:" + question.externalId())).append("', '")
                    .append(missionIds.get(question.missionCode())).append("', '")
                    .append(question.type()).append("', '")
                    .append(escape(question.question())).append("', ")
                    .append(question.options() == null ? "NULL" : "'" + escape(toJson(question.options())) + "'")
                    .append(", ")
                    .append(question.contentTags() == null ? "NULL" : "'" + escape(toJson(question.contentTags())) + "'")
                    .append(", '").append(escape(question.curriculumRef())).append("', ")
                    .append(question.difficultyBand() == null ? "NULL" : "'" + question.difficultyBand().name() + "'")
                    .append(", '")
                    .append(escape(defaultString(question.sourceType(), "STATIC"))).append("', '")
                    .append(defaultGenerationPhase(question)).append("', ")
                    .append(question.packNo() == null ? "NULL" : question.packNo()).append(", ")
                    .append(question.difficultyBand() == null ? "NULL" : "'" + question.difficultyBand().name() + "'")
                    .append(", '").append(QuestionPoolStatus.ACTIVE.name()).append("', TRUE)");
            sql.append(index < bank.questions().size() - 1 ? ",\n" : "\n");
        }
        sql.append("ON CONFLICT (id) DO UPDATE SET\n")
                .append("    mission_id = EXCLUDED.mission_id,\n")
                .append("    question_type = EXCLUDED.question_type,\n")
                .append("    prompt = EXCLUDED.prompt,\n")
                .append("    options = EXCLUDED.options,\n")
                .append("    content_tags = EXCLUDED.content_tags,\n")
                .append("    curriculum_ref = EXCLUDED.curriculum_ref,\n")
                .append("    difficulty = EXCLUDED.difficulty,\n")
                .append("    source_type = EXCLUDED.source_type,\n")
                .append("    generation_phase = EXCLUDED.generation_phase,\n")
                .append("    pack_no = EXCLUDED.pack_no,\n")
                .append("    difficulty_band = EXCLUDED.difficulty_band,\n")
                .append("    question_pool_status = EXCLUDED.question_pool_status,\n")
                .append("    is_active = TRUE;\n\n");

        sql.append("INSERT INTO private.question_answer_keys (question_id, answer_payload, explanation) VALUES\n");
        for (int index = 0; index < bank.questions().size(); index++) {
            AuditQuestion question = bank.questions().get(index);
            sql.append("    ('").append(uuid("question:" + question.externalId())).append("', '")
                    .append(escape(toAnswerPayload(question))).append("', '")
                    .append(escape(question.explanation())).append("')");
            sql.append(index < bank.questions().size() - 1 ? ",\n" : "\n");
        }
        sql.append("ON CONFLICT (question_id) DO UPDATE SET\n")
                .append("    answer_payload = EXCLUDED.answer_payload,\n")
                .append("    explanation = EXCLUDED.explanation;\n");
        return sql.toString();
    }

    private java.util.List<QuestionDraft> distinctMissionQuestions(QuestionBankDraft draft) {
        Map<String, QuestionDraft> missions = new LinkedHashMap<>();
        draft.questions().forEach(question -> missions.putIfAbsent(question.missionCode(), question));
        return java.util.List.copyOf(missions.values());
    }

    private java.util.List<AuditQuestion> distinctMissionQuestions(AuditQuestionBank bank) {
        Map<String, AuditQuestion> missions = new LinkedHashMap<>();
        bank.questions().forEach(question -> missions.putIfAbsent(question.missionCode(), question));
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

    private String toAnswerPayload(AuditQuestion question) {
        if (question.type() == QuestionType.MULTIPLE || question.type() == QuestionType.SITUATION) {
            return toJson(question.options().get((Integer) question.answer()));
        }
        if (question.type() == QuestionType.FILL) {
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

    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String defaultGenerationPhase(AuditQuestion question) {
        return GenerationPhase.PREGENERATED.name();
    }

    private void appendMissionCodes(StringBuilder sql, Map<String, UUID> missionIds) {
        int index = 0;
        for (String missionCode : missionIds.keySet()) {
            if (index++ > 0) {
                sql.append(", ");
            }
            sql.append('\'').append(escape(missionCode)).append('\'');
        }
    }

    private static UUID uuid(String seed) {
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}
