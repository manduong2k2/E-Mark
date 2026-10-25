package com.Marketplace_Management.Assistant.DTOs.Response;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String reply;
    private Map<String, Object> attachments;
    private String model;
}
