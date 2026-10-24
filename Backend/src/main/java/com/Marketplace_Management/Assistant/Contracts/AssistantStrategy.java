package com.Marketplace_Management.Assistant.Contracts;

public interface AssistantStrategy {
    String getModel();
    String generate(String userMessage);
}
