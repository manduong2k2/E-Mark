package com.Marketplace_Management.Auth.Contracts;
import java.io.IOException;
import java.util.UUID;

import com.Marketplace_Management.Auth.DTOs.Commands.ActivateUserCommand;
import com.Marketplace_Management.Auth.DTOs.Commands.ChangePasswordCommand;
import com.Marketplace_Management.Auth.DTOs.Commands.ForgotPasswordCommand;
import com.Marketplace_Management.Auth.DTOs.Commands.OAuthLoginCommand;
import com.Marketplace_Management.Auth.DTOs.Commands.LoginCommand;
import com.Marketplace_Management.Auth.DTOs.Commands.RefreshTokenCommand;
import com.Marketplace_Management.Auth.DTOs.Commands.RegisterCommand;
import com.Marketplace_Management.Auth.DTOs.Commands.ResetPasswordCommand;
import com.Marketplace_Management.Auth.DTOs.Commands.UpdateProfileCommand;
import com.Marketplace_Management.Auth.DTOs.Response.AuthResponse;
import com.Marketplace_Management.Auth.DTOs.Response.RegisterResponse;
import com.Marketplace_Management.Auth.DTOs.Response.ProfileResponse;

import jakarta.mail.MessagingException;

public interface IAuthService {
    RegisterResponse register(RegisterCommand command) throws MessagingException;
    AuthResponse activeUser(ActivateUserCommand command);
    AuthResponse login(LoginCommand command);
    AuthResponse loginAdmin(LoginCommand command);
    AuthResponse loginWithOAuth(OAuthLoginCommand command);
    AuthResponse refreshToken(RefreshTokenCommand command);
    void sendResetPasswordEmail(ForgotPasswordCommand command) throws MessagingException;
    boolean resetPassword(ResetPasswordCommand command);
    AuthResponse changePassword(UUID userId, ChangePasswordCommand command);
    void logout(String accessToken, String refreshToken);
    ProfileResponse getUserById(UUID userId);
    void updateProfile(UUID userId, UpdateProfileCommand command) throws IOException;
    void grantRole(UUID userId, String role);
}
