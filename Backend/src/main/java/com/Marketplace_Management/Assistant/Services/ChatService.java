package com.Marketplace_Management.Assistant.Services;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.Marketplace_Management.Assistant.Contracts.AssistantStrategy;
import com.Marketplace_Management.Assistant.Contracts.IChatService;
import com.Marketplace_Management.Assistant.DTOs.Commands.ChatCommand;
import com.Marketplace_Management.Assistant.DTOs.Response.ChatResponse;

@Service
public class ChatService implements IChatService {
     private final Map<String, AssistantStrategy> strategies;

    public ChatService(Map<String, AssistantStrategy> strategies) {
        this.strategies = strategies;
    }

    @Override
    public ChatResponse chat(ChatCommand command) {
        String provider = command.getProvider();

        AssistantStrategy strategy = strategies.get(provider);

        if (strategy == null) {
            throw new IllegalArgumentException(
                    "Unsupported assistant provider: " + provider);
        }

        String reply = strategy.generate(command.getMessage());

        return new ChatResponse(reply, strategy.getModel());
    }
}
