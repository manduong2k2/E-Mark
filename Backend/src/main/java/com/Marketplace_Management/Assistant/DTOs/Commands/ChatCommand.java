package com.Marketplace_Management.Assistant.DTOs.Commands;

import java.util.UUID;

import com.Marketplace_Management.Assistant.DTOs.Request.ChatRequest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatCommand {
    private UUID userId;
    private String message;
    private String provider;

    public static ChatCommand fromRequest(UUID userId, ChatRequest request) {
        return new ChatCommand(userId, request.getMessage().trim(), request.getProvider());
    }
}
