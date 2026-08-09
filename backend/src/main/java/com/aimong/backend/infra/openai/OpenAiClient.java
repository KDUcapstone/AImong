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
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private final RestClient openAiRestClient;
    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;

    public GeneratedImage createImage(String model, String prompt, String size, String quality) {
        if (!properties.isChatConfigured()) {
            throw new AimongException(ErrorCode.INTERNAL_SERVER_ERROR, "OPENAI_API_CHAT_KEY is not configured");
        }

        Map<String, Object> payload = Map.of(
                "model", model,
                "prompt", prompt,
                "n", 1,
                "size", size,
                "quality", quality,
                "output_format", "png"
        );

        try {
            JsonNode response = openAiRestClient.post()
                    .uri("/images/generations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.resolvedChatApiKey())
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode image = response == null ? null : response.path("data").path(0);
            if (image == null || !image.path("b64_json").isTextual() || image.path("b64_json").asText().isBlank()) {
                throw new AimongException(ErrorCode.INTERNAL_SERVER_ERROR, "OpenAI image output is missing");
            }
            String outputFormat = firstText(response.path("output_format"), image.path("output_format"), "png");
            String responseSize = firstText(response.path("size"), image.path("size"), size);
            String responseQuality = firstText(response.path("quality"), image.path("quality"), quality);
            return new GeneratedImage(image.path("b64_json").asText(), outputFormat, responseSize, responseQuality);
        } catch (AimongException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw new AimongException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "OpenAI image generation request failed: HTTP " + exception.getStatusCode().value()
            );
        } catch (RestClientException exception) {
            throw new AimongException(ErrorCode.INTERNAL_SERVER_ERROR, "OpenAI image generation request failed");
        }
    }

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

        try {
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
        } catch (AimongException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw new AimongException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "OpenAI mission generation request failed: HTTP " + exception.getStatusCode().value()
            );
        } catch (RestClientException exception) {
            throw new AimongException(ErrorCode.INTERNAL_SERVER_ERROR, "OpenAI mission generation request failed");
        }
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

    private String firstText(JsonNode first, JsonNode second, String fallback) {
        if (first != null && first.isTextual() && !first.asText().isBlank()) {
            return first.asText();
        }
        if (second != null && second.isTextual() && !second.asText().isBlank()) {
            return second.asText();
        }
        return fallback;
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception exception) {
            throw new AimongException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
    }

    public record GeneratedImage(
            String b64Json,
            String outputFormat,
            String size,
            String quality
    ) {
    }
}
