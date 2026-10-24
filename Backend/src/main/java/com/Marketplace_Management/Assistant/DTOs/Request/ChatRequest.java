package com.Marketplace_Management.Assistant.DTOs.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    @NotBlank(message = "message must not be empty")
    @Size(max = 4000, message = "message must not exceed 4000 characters")
    private String message;
    
    private String provider;
}

