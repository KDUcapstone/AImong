package com.aimong.backend.domain.mission.service.generation;

import com.aimong.backend.infra.openai.OpenAiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
public class OpenAiQuestionCandidateGenerator implements QuestionCandidateGenerator {

    private final OpenAiClient openAiClient;
    private final KerisCurriculumRegistry kerisCurriculumRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public List<StructuredQuestionSchema> generate(
            QuestionGenerationService.QuestionGenerationRequest request,
            String selectedModel
    ) {
        KerisCurriculumRegistry.KerisMissionRule missionRule = kerisCurriculumRegistry.findMissionRule(request.missionCode())
                .orElseThrow(() -> new IllegalArgumentException("Unknown missionCode: " + request.missionCode()));

        JsonNode responseJson = openAiClient.createStructuredResponse(
                selectedModel,
                buildDeveloperPrompt(missionRule, request),
                buildUserPrompt(missionRule, request),
                "aimong_question_candidates",
                buildSchema(request.candidateCount())
        );

        List<StructuredQuestionSchema> results = new ArrayList<>();
        for (JsonNode node : responseJson.path("questions")) {
            results.add(readCandidate(node));
        }
        return results;
    }

    private String buildDeveloperPrompt(
            KerisCurriculumRegistry.KerisMissionRule missionRule,
            QuestionGenerationService.QuestionGenerationRequest request
    ) {
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("You generate Korean elementary AI literacy questions for grade 5-6.");
        joiner.add("Return strict JSON only. Do not add commentary.");
        joiner.add("Question lookup remains missionId-based externally, but generation is for one missionCode.");
        joiner.add("Never ask for real names, addresses, phone numbers, face photos, voiceprints, or fingerprints.");
        joiner.add("Explanation must be 2 sentences or fewer.");
        joiner.add("Mission code: " + missionRule.missionCode());
        joiner.add("Stage: " + missionRule.stage() + " / " + missionRule.stageTitle());
        joiner.add("Mission title: " + missionRule.missionTitle());
        joiner.add("Allowed concepts: " + String.join(", ", missionRule.allowedConcepts()));
        joiner.add("Banned concepts: " + String.join(", ", missionRule.bannedConcepts()));
        joiner.add("Good scenario patterns: " + String.join(", ", missionRule.goodScenarioPatterns()));
        joiner.add("Avoid scenario patterns: " + String.join(", ", missionRule.avoidScenarioPatterns()));
        joiner.add("Preferred content tags: " + String.join(", ", missionRule.preferredContentTags()));
        joiner.add("Curriculum ref: " + missionRule.curriculumRef());
        joiner.add("Required packNo: " + request.packNo());
        joiner.add("Required difficultyBand: " + request.difficultyBand());
        joiner.add("Required numeric difficulty: " + request.numericDifficulty());
        joiner.add("Required type: " + request.desiredType());
        joiner.add("Generate " + request.candidateCount() + " candidate(s).");
        return joiner.toString();
    }

    private String buildUserPrompt(
            KerisCurriculumRegistry.KerisMissionRule missionRule,
            QuestionGenerationService.QuestionGenerationRequest request
    ) {
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("Write Korean only.");
        joiner.add("Target learner: Korean elementary grade 5-6.");
        joiner.add("Use short concrete sentences.");
        joiner.add("Mission title: " + missionRule.missionTitle());
        joiner.add("Question type must be exactly: " + request.desiredType());
        joiner.add("Difficulty band must be exactly: " + request.difficultyBand());
        joiner.add("Pack number must be exactly: " + request.packNo());
        joiner.add("Numeric difficulty must be exactly: " + request.numericDifficulty());
        if (!request.existingMissionPrompts().isEmpty()) {
            joiner.add("Avoid semantic duplicates of these existing prompts:");
            for (String prompt : request.existingMissionPrompts()) {
                joiner.add("- " + prompt);
            }
        }
        if (!request.goldExamplePrompts().isEmpty()) {
            joiner.add("Gold anchors to learn style from but not copy:");
            for (String prompt : request.goldExamplePrompts()) {
                joiner.add("- " + prompt);
            }
        }
        if (!request.repairHints().isEmpty()) {
            joiner.add("Fix these issues that caused previous candidates to be rejected:");
            for (String repairHint : request.repairHints()) {
                joiner.add("- " + repairHint);
            }
        }
        joiner.add("Return candidates that obey the official tags and type-specific answer shape.");
        return joiner.toString();
    }

    private JsonNode buildSchema(int candidateCount) {
        try {
            return objectMapper.readTree("""
                    {
                      "type": "object",
                      "additionalProperties": false,
                      "required": ["questions"],
                      "properties": {
                        "questions": {
                          "type": "array",
                          "minItems": %d,
                          "maxItems": %d,
                          "items": {
                            "type": "object",
                            "additionalProperties": false,
                            "required": [
                              "missionCode",
                              "packNo",
                              "difficultyBand",
                              "type",
                              "question",
                              "options",
                              "answer",
                              "explanation",
                              "contentTags",
                              "curriculumRef",
                              "difficulty"
                            ],
                            "properties": {
                              "missionCode": {"type": "string"},
                              "packNo": {"type": "integer", "minimum": 1, "maximum": 6},
                              "difficultyBand": {"type": "string", "enum": ["LOW", "MEDIUM", "HIGH"]},
                              "type": {"type": "string", "enum": ["OX", "MULTIPLE", "FILL", "SITUATION"]},
                              "question": {"type": "string"},
                              "options": {
                                "anyOf": [
                                  {"type": "null"},
                                  {"type": "array", "items": {"type": "string"}, "minItems": 2, "maxItems": 5}
                                ]
                              },
                              "answer": {
                                "anyOf": [
                                  {"type": "boolean"},
                                  {"type": "integer", "minimum": 0, "maximum": 4},
                                  {
                                    "type": "array",
                                    "items": {"type": "integer", "minimum": 0, "maximum": 4},
                                    "minItems": 1,
                                    "maxItems": 1
                                  }
                                ]
                              },
                              "explanation": {"type": "string"},
                              "contentTags": {
                                "type": "array",
                                "items": {"type": "string", "enum": ["FACT", "PRIVACY", "PROMPT", "SAFETY", "VERIFICATION"]},
                                "minItems": 1,
                                "maxItems": 3
                              },
                              "curriculumRef": {"type": "string"},
                              "difficulty": {"type": "integer", "minimum": 1, "maximum": 4}
                            }
                          }
                        }
                      }
                    }
                    """.formatted(candidateCount, candidateCount));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to build structured output schema", exception);
        }
    }

    private StructuredQuestionSchema readCandidate(JsonNode node) {
        try {
            return objectMapper.treeToValue(node, StructuredQuestionSchema.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to parse structured question candidate", exception);
        }
    }
}
