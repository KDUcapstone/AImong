package com.aimong.backend.infra.openai;

import com.aimong.backend.global.config.OpenAiProperties;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private final RestClient openAiRestClient;
    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;

    public JsonNode createStructuredResponse(
            String model,
            String developerPrompt,
            String userPrompt,
            String schemaName,
            JsonNode schema
    ) {
        if (!properties.isConfigured()) {
            throw new AimongException(ErrorCode.INTERNAL_SERVER_ERROR, "OPENAI_API_KEY is not configured");
        }

        Map<String, Object> payload = Map.of(
                "model", model,
                "input", List.of(
                        Map.of("role", "developer", "content", developerPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "text", Map.of(
                        "format", Map.of(
                                "type", "json_schema",
                                "name", schemaName,
                                "strict", true,
                                "schema", schema
                        )
                )
        );

        JsonNode response = openAiRestClient.post()
                .uri(properties.responsesPath())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new AimongException(ErrorCode.INTERNAL_SERVER_ERROR, "OpenAI response body is empty");
        }

        JsonNode outputText = response.path("output_text");
        if (outputText.isTextual() && !outputText.asText().isBlank()) {
            return readJson(outputText.asText());
        }

        for (JsonNode output : response.path("output")) {
            for (JsonNode content : output.path("content")) {
                if ("output_text".equals(content.path("type").asText()) && content.path("text").isTextual()) {
                    return readJson(content.path("text").asText());
                }
            }
        }

        throw new AimongException(ErrorCode.INTERNAL_SERVER_ERROR, "OpenAI structured output is missing");
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception exception) {
            throw new AimongException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
    }
}
