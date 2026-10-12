package com.Marketplace_Management.Auth.DTOs.Response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String message;
    private boolean isNewUser;

    public AuthResponse(String accessToken, String refreshToken, String message) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.message = message;
    }

    public AuthResponse isNewUser(boolean isNewUser) {
        this.isNewUser = isNewUser;
        return this;
    }
}
