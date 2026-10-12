package com.Marketplace_Management.Auth.Controllers;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.*;

import com.Marketplace_Management.Shared.Constants.Http;
import com.Marketplace_Management.Shared.Utils.Http.CookieUtils;
import com.Marketplace_Management.Auth.Constants.Message;
import com.Marketplace_Management.Auth.Constants.OAuthProvider;
import com.Marketplace_Management.Auth.Contracts.IAuthService;
import com.Marketplace_Management.Auth.Contracts.ICookieService;
import com.Marketplace_Management.Auth.DTOs.Commands.ActivateUserCommand;
import com.Marketplace_Management.Auth.DTOs.Commands.ChangePasswordCommand;
import com.Marketplace_Management.Auth.DTOs.Commands.ForgotPasswordCommand;
import com.Marketplace_Management.Auth.DTOs.Commands.OAuthLoginCommand;
import com.Marketplace_Management.Auth.DTOs.Commands.LoginCommand;
import com.Marketplace_Management.Auth.DTOs.Commands.RefreshTokenCommand;
import com.Marketplace_Management.Auth.DTOs.Commands.RegisterCommand;
import com.Marketplace_Management.Auth.DTOs.Commands.ResetPasswordCommand;
import com.Marketplace_Management.Auth.DTOs.Commands.UpdateProfileCommand;
import com.Marketplace_Management.Auth.DTOs.Request.ActivateUserRequest;
import com.Marketplace_Management.Auth.DTOs.Request.ChangePasswordRequest;
import com.Marketplace_Management.Auth.DTOs.Request.ForgotPasswordRequest;
import com.Marketplace_Management.Auth.DTOs.Request.OAuthLoginRequest;
import com.Marketplace_Management.Auth.DTOs.Request.LoginRequest;
import com.Marketplace_Management.Auth.DTOs.Request.RefreshTokenRequest;
import com.Marketplace_Management.Auth.DTOs.Request.RegisterRequest;
import com.Marketplace_Management.Auth.DTOs.Request.ResetPasswordRequest;
import com.Marketplace_Management.Auth.DTOs.Request.UpdateProfileRequest;
import com.Marketplace_Management.Auth.DTOs.Response.AuthResponse;
import com.Marketplace_Management.Auth.DTOs.Response.ProfileResponse;
import com.Marketplace_Management.Auth.Models.User;
import com.Marketplace_Management.Shared.Annotation.Auth.Authenticated;
import com.Marketplace_Management.Shared.Controllers.BaseController;
import com.Marketplace_Management.Shared.Errors.Exceptions.BadRequestException;
import com.Marketplace_Management.Shared.Security.SecurityUtils;

@RestController
@RequestMapping("/api/auth")
public class AuthController extends BaseController {

    private final IAuthService auth;
    private final ICookieService cookieService;

    @Value("${spring.application.base-url}")
    private String baseUrl;

    @Value("${application.frontend.base-url}")
    private String frontendBaseUrl;

    public AuthController(IAuthService auth, ICookieService cookieService) {
        this.auth = auth;
        this.cookieService = cookieService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @ModelAttribute RegisterRequest req)
            throws MessagingException {
        RegisterCommand command = RegisterCommand.fromRequest(req);
        return objectResponse(auth.register(command));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody(required = true) LoginRequest req) {
        LoginCommand command = LoginCommand.fromRequest(req);
        AuthResponse authRes = auth.login(command);
        return authCookieResponse(authRes);
    }

    /** Sign in with a provider: /api/auth/oauth/google (ID token) or /api/auth/oauth/facebook (access token). */
    @PostMapping("/oauth/{provider}")
    public ResponseEntity<Map<String, Object>> loginWithOAuth(
            @PathVariable String provider, @Valid @RequestBody(required = true) OAuthLoginRequest req) {
        OAuthProvider oauthProvider = OAuthProvider.fromPath(provider)
                .orElseThrow(() -> new BadRequestException(Message.OAUTH_PROVIDER_UNSUPPORTED));
        OAuthLoginCommand command = OAuthLoginCommand.fromRequest(oauthProvider, req);
        AuthResponse authRes = auth.loginWithOAuth(command);

        return authCookieResponse(authRes);
    }

    @PostMapping("/admin/login")
    public ResponseEntity<Map<String, Object>> loginAdmin(@Valid @RequestBody(required = true) LoginRequest req) {
        LoginCommand command = LoginCommand.fromRequest(req);
        AuthResponse authRes = auth.loginAdmin(command);
        return authCookieResponse(authRes);
    }

    /**
     * Body { refreshToken } is optional: the web app cannot read the httpOnly REFRESH_TOKEN cookie,
     * so when the body has no token the cookie is used instead.
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<Map<String, Object>> refreshToken(
            @RequestBody(required = false) RefreshTokenRequest req, HttpServletRequest request) {
        String refreshToken = req != null && req.getRefreshToken() != null && !req.getRefreshToken().isBlank()
                ? req.getRefreshToken()
                : CookieUtils.read(request, Http.REFRESH_TOKEN_COOKIE);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthenticationCredentialsNotFoundException(Message.TOKEN_INVALID);
        }

        RefreshTokenCommand command = new RefreshTokenCommand(refreshToken);
        var authRes = auth.refreshToken(command);
        return authCookieResponse(authRes);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> activeUser(@Valid @ModelAttribute ActivateUserRequest request) {
        ActivateUserCommand command = ActivateUserCommand.fromRequest(request);
        var authRes = auth.activeUser(command);
        HttpHeaders headers = cookieService.createAuthCookies(authRes.getAccessToken(), authRes.getRefreshToken());

        headers.setLocation(
                URI.create(frontendBaseUrl + "/onboarding"));

        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .headers(headers)
                .build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@Valid @RequestBody(required = true) ForgotPasswordRequest req)
            throws MessagingException {
        ForgotPasswordCommand command = ForgotPasswordCommand.fromRequest(req);
        auth.sendResetPasswordEmail(command);
        return successResponse(Message.RECOVERY_MAIL_SENT);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@Valid @RequestBody(required = true) ResetPasswordRequest req) {
        ResetPasswordCommand command = ResetPasswordCommand.fromRequest(req);
        auth.resetPassword(command);
        return successResponse(Message.PASSWORD_UPDATED);
    }

    @Authenticated
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> profile() {
        UUID userId = SecurityUtils.currentUserId();
        User user = auth.getUserById(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("data", new ProfileResponse(user).withUrl(baseUrl));
        return ResponseEntity.ok(response);
    }

    @Authenticated
    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(@Valid @ModelAttribute UpdateProfileRequest req) throws IOException {
        UUID userId = SecurityUtils.currentUserId();
        UpdateProfileCommand command = UpdateProfileCommand.fromRequest(req);
        auth.updateProfile(userId, command);

        Map<String, Object> response = new HashMap<>();
        response.put("message", Message.PROFILE_UPDATED);
        return ResponseEntity.ok(response);
    }

    /** Changes the password, signs out every other device and starts a new session for this browser. */
    @Authenticated
    @PutMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@Valid @RequestBody(required = true) ChangePasswordRequest req) {
        UUID userId = SecurityUtils.currentUserId();
        AuthResponse authRes = auth.changePassword(userId, ChangePasswordCommand.fromRequest(req));
        return authCookieResponse(authRes);
    }

    // No @Authenticated: logout must work even when the access token has already expired.
    // Revokes both tokens, deletes their session and clears the cookies.
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
        String accessToken = CookieUtils.read(request, Http.ACCESS_TOKEN_COOKIE);
        String refreshToken = CookieUtils.read(request, Http.REFRESH_TOKEN_COOKIE);

        auth.logout(accessToken, refreshToken);

        HttpHeaders headers = cookieService.createClearCookies();

        Map<String, Object> response = new HashMap<>();
        response.put("message", Message.LOGOUT_SUCCESS);

        return ResponseEntity.ok()
                .headers(headers)
                .body(response);
    }

    /** { message } + the ACCESS_TOKEN / REFRESH_TOKEN cookies of a newly issued session. */
    private ResponseEntity<Map<String, Object>> authCookieResponse(AuthResponse authRes) {
        HttpHeaders cookies = cookieService.createAuthCookies(authRes.getAccessToken(), authRes.getRefreshToken());
        Map<String, Object> response = new HashMap<>();
        response.put("message", authRes.getMessage());
        response.put("isNewUser", authRes.isNewUser());
        return ResponseEntity.ok().headers(cookies).body(response);
    }

}
