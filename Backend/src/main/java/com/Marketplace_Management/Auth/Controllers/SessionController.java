package com.Marketplace_Management.Auth.Controllers;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Marketplace_Management.Auth.DTOs.Commands.GetListSessionCommand;
import com.Marketplace_Management.Auth.DTOs.Request.GetListSessionRequest;
import com.Marketplace_Management.Auth.DTOs.Request.TerminateSessionsRequest;
import com.Marketplace_Management.Auth.Services.UserSessionService;
import com.Marketplace_Management.Shared.Constants.UserRole;
import com.Marketplace_Management.Shared.Controllers.BaseController;

import jakarta.validation.Valid;

/**
 * Admin endpoints for managing active user sessions.
 * All routes require the ADMIN authority.
 */
@RestController
@RequestMapping("/api/admin/sessions")
@PreAuthorize("hasAuthority('" + UserRole.ADMIN + "')")
public class SessionController extends BaseController {

    private final UserSessionService sessionService;

    public SessionController(UserSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll(
            @Valid @ModelAttribute GetListSessionRequest request) {
        return paginatedResponse(sessionService.getSessions(GetListSessionCommand.fromRequest(request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> terminate(@PathVariable UUID id) {
        sessionService.terminateSession(id);
        return objectResponse(Map.of("id", id));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Object>> terminateMany(
            @Valid @RequestBody TerminateSessionsRequest request) {
        int terminated = sessionService.terminateSessions(request.getSessionIds());
        return objectResponse(Map.of("terminated", terminated));
    }
}
