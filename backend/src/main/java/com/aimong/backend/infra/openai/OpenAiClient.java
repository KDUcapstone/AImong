package com.aimong.backend.infra.openai;

import com.aimong.backend.global.config.OpenAiProperties;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private final RestClient openAiRestClient;
    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;

    public String createChatReply(String model, String developerPrompt, String userPrompt) {
        if (!properties.isChatConfigured()) {
            throw new AimongException(ErrorCode.INTERNAL_SERVER_ERROR, "OPENAI_API_CHAT_KEY is not configured");
        }

        Map<String, Object> payload = Map.of(
                "model", model,
                "input", List.of(
                        Map.of("role", "developer", "content", developerPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            JsonNode response = openAiRestClient.post()
                    .uri(properties.responsesPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.resolvedChatApiKey())
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);

            String outputText = extractOutputText(response);
            if (outputText == null || outputText.isBlank()) {
                throw new AimongException(ErrorCode.INTERNAL_SERVER_ERROR, "OpenAI text output is missing");
            }
            return outputText.strip();
        } catch (AimongException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new AimongException(ErrorCode.INTERNAL_SERVER_ERROR, "AI 친구가 지금 쉬고 있어요. 잠시 후 다시 시도해주세요");
        }
    }

    public JsonNode createStructuredResponse(
            String model,
            String developerPrompt,
            String userPrompt,
            String schemaName,
            JsonNode schema
    ) {
        if (!properties.isMissionsConfigured()) {
            throw new AimongException(ErrorCode.INTERNAL_SERVER_ERROR, "OPENAI_API_MISSIONS_KEY is not configured");
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
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.resolvedMissionsApiKey())
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

    private String extractOutputText(JsonNode response) {
        if (response == null) {
            return null;
        }

        JsonNode outputText = response.path("output_text");
        if (outputText.isTextual()) {
            return outputText.asText();
        }

        for (JsonNode output : response.path("output")) {
            for (JsonNode content : output.path("content")) {
                if ("output_text".equals(content.path("type").asText()) && content.path("text").isTextual()) {
                    return content.path("text").asText();
                }
            }
        }

        return null;
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception exception) {
            throw new AimongException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
    }
}
