package com.Marketplace_Management.Assistant.Services;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.Marketplace_Management.Assistant.Contracts.AssistantStrategy;
import com.Marketplace_Management.Assistant.Contracts.IChatService;
import com.Marketplace_Management.Assistant.DTOs.Commands.ChatCommand;
import com.Marketplace_Management.Assistant.DTOs.Response.ChatResponse;

import tools.jackson.databind.ObjectMapper;

@Service
public class ChatService implements IChatService {
    private final Map<String, AssistantStrategy> strategies;
    private final PromptBuilder promptBuilder;

    private final ServiceExecutor serviceExecutor;
    private final ObjectMapper objectMapper;

    public ChatService(Map<String, AssistantStrategy> strategies, ObjectMapper objectMapper,
            ServiceExecutor serviceExecutor, PromptBuilder promptBuilder) {
        this.strategies = strategies;
        this.objectMapper = objectMapper;
        this.serviceExecutor = serviceExecutor;
        this.promptBuilder = promptBuilder;
    }

    @Override
    public ChatResponse chat(ChatCommand command) {
        String provider = command.getProvider();
        AssistantStrategy strategy = strategies.get(provider);

        if (strategy == null) {
            throw new IllegalArgumentException(
                    "Unsupported assistant provider: " + provider);
        }

        String prompt = promptBuilder.buildPrompt(command.getMessage());
        String reply = strategy.generate(prompt);
        ResolvedIntent resolvedIntent = extractResolvedIntent(reply);
        String answer = extractAnswer(reply);

        Object executionResult = resolvedIntent == null ? null : serviceExecutor.execute(resolvedIntent);
        Map<String, Object> metadata = new HashMap<>();
        if (resolvedIntent != null) {
            metadata.put("intent", resolvedIntent.domain());
        }
        if (executionResult != null) {
            metadata.put("data", executionResult);
        }

        return new ChatResponse(answer, metadata, strategy.getModel());
    }

    private ResolvedIntent extractResolvedIntent(String reply) {
        try {
            var root = objectMapper.readTree(reply);
            var intentNode = root.get("resolvedIntent");

            if (intentNode == null || intentNode.isNull()) {
                return null;
            }

            return objectMapper.treeToValue(intentNode, ResolvedIntent.class);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to extract resolvedIntent from AI response", e);
        }
    }

    private String extractAnswer(String reply) {
        try {
            var root = objectMapper.readTree(reply);
            var answerNode = root.get("answer");

            if (answerNode == null || answerNode.isNull()) {
                return null;
            }

            return answerNode.toPrettyString();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to extract answer from AI response", e);
        }
    }

    public record ResolvedIntent(
            String domain,
            String method,
            Map<String, Object> params) {
    }
}
