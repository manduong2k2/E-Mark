
package com.Marketplace_Management.Assistant.Clients;

import java.time.Duration;
import java.util.List;

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

@Component("groq")
public class GroqStrategy implements AssistantStrategy {

    private static final Logger logger =
            LoggerFactory.getLogger(GroqStrategy.class);

    private final RestClient restClient;
    private final String model;

    public GroqStrategy(
            RestClient.Builder builder,
            @Value("${third-party.groq.api-key}") String apiKey,
            @Value("${third-party.groq.model:openai/gpt-oss-20b}") String model,
            @Value("${third-party.groq.base-url:https://api.groq.com/openai/v1}") String baseUrl) {

        this.model = model;

        this.restClient = RestClients
                .withTimeouts(
                        builder,
                        baseUrl,
                        Duration.ofSeconds(60),
                        Duration.ofSeconds(120))
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public String getModel() {
        return model;
    }

    /**
     * Sends a single user message and returns the model's text reply.
     */
    public String generate(String userMessage) {
        ChatCompletionRequest body = new ChatCompletionRequest(
                model,
                List.of(new MessageRequest("user", userMessage)));

        ChatCompletionResponse response;

        try {
            response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(ChatCompletionResponse.class);

        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();

            // Do not log the response body: it may contain sensitive data.
            logger.error("Groq API error, status={}", status);

            // Rate limit or quota exceeded
            if (status == HttpStatus.TOO_MANY_REQUESTS.value()) {
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        Message.ASSISTANT_BUSY);
            }

            // Service temporarily unavailable
            if (status == HttpStatus.SERVICE_UNAVAILABLE.value()
                    || status == HttpStatus.BAD_GATEWAY.value()
                    || status == HttpStatus.GATEWAY_TIMEOUT.value()) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        Message.ASSISTANT_BUSY);
            }

            // Authentication, invalid request, model not found, etc.
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    Message.ASSISTANT_UNAVAILABLE);

        } catch (ResourceAccessException e) {
            // Connection failure or timeout
            logger.error("Groq API unreachable: {}", e.getMessage());

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    Message.ASSISTANT_UNAVAILABLE);
        }

        return extractText(response);
    }

    private String extractText(ChatCompletionResponse response) {
        if (response == null
                || response.choices() == null
                || response.choices().isEmpty()) {

            logger.warn("Groq returned no choices");

            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    Message.ASSISTANT_BLOCKED);
        }

        ChatCompletionChoice choice = response.choices().get(0);

        String text = choice.message() == null
                ? null
                : choice.message().content();

        if (text == null || text.isBlank()) {
            logger.warn(
                    "Groq returned an empty reply (finishReason={})",
                    choice.finishReason());

            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    Message.ASSISTANT_BLOCKED);
        }

        return text;
    }

    // ── Groq OpenAI-compatible REST payloads ──────────────────────────────

    record ChatCompletionRequest(
            String model,
            List<MessageRequest> messages) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MessageRequest(
            String role,
            String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatCompletionResponse(
            List<ChatCompletionChoice> choices) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatCompletionChoice(
            ChatMessage message,
            String finishReason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatMessage(
            String role,
            String content) {}
}
