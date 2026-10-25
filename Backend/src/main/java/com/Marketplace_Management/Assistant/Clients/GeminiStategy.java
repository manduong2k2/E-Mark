package com.Marketplace_Management.Assistant.Clients;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import com.Marketplace_Management.Assistant.Constants.Message;
import com.Marketplace_Management.Assistant.Contracts.AssistantStrategy;
import com.Marketplace_Management.Shared.Utils.Http.RestClients;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Thin HTTP client for the Gemini API (generateContent).
 * The API key is sent in the x-goog-api-key header (never in the URL, so it does not end up in logs).
 */
@Component("gemini")
public class GeminiStategy implements AssistantStrategy {
    private static final Logger logger = LoggerFactory.getLogger(GeminiStategy.class);

    private final RestClient restClient;
    private final String model;

    public GeminiStategy(
            RestClient.Builder builder,
            @Value("${third-party.gemini.api-key}") String apiKey,
            @Value("${third-party.gemini.model:gemini-3.5-flash-lite}") String model,
            @Value("${third-party.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl) {
        this.model = model;
        this.restClient = RestClients.withTimeouts(builder, baseUrl, Duration.ofSeconds(60), Duration.ofSeconds(120))
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
    }

    public String getModel() {
        return model;
    }

    /** Sends a single user message and returns the model's text reply. */
    public String generate(String userMessage) {
        GenerateContentRequest body = new GenerateContentRequest(
                List.of(new Content("user", List.of(new Part(userMessage, null)))));

        GenerateContentResponse response;
        try {
            response = restClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(GenerateContentResponse.class);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            logger.error("Gemini API error {}: {}", status, e.getResponseBodyAsString());
            // 429 = quota/rate limit, 503 = model overloaded ("high demand"): both are temporary
            if (status == HttpStatus.TOO_MANY_REQUESTS.value()) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, Message.ASSISTANT_BUSY);
            }
            if (status == HttpStatus.SERVICE_UNAVAILABLE.value()) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, Message.ASSISTANT_BUSY);
            }
            // Anything else (invalid key, unknown model, bad request...) is a server-side problem
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, Message.ASSISTANT_UNAVAILABLE);
        } catch (ResourceAccessException e) {
            // Connection failure or timeout
            logger.error("Gemini API unreachable: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, Message.ASSISTANT_UNAVAILABLE);
        }

        return extractText(response);
    }

    private String extractText(GenerateContentResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            // No candidate usually means the prompt itself was blocked by safety filters
            String reason = response != null && response.promptFeedback() != null
                    ? response.promptFeedback().blockReason()
                    : null;
            logger.warn("Gemini returned no candidates (blockReason={})", reason);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, Message.ASSISTANT_BLOCKED);
        }

        Candidate candidate = response.candidates().get(0);
        String text = candidate.content() == null || candidate.content().parts() == null
                ? ""
                : candidate.content().parts().stream()
                        // Skip thought summaries of thinking models, keep only the answer
                        .filter(part -> !Boolean.TRUE.equals(part.thought()))
                        .map(Part::text)
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining());

        if (text.isBlank()) {
            logger.warn("Gemini returned an empty reply (finishReason={})", candidate.finishReason());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, Message.ASSISTANT_BLOCKED);
        }
        return text;
    }

    // ── Gemini REST payloads ─────────────────────────────────────────────────

    record GenerateContentRequest(List<Content> contents) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Content(String role, List<Part> parts) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Part(String text, Boolean thought) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GenerateContentResponse(List<Candidate> candidates, PromptFeedback promptFeedback) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Candidate(Content content, String finishReason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PromptFeedback(String blockReason) {}
}
