package com.Marketplace_Management.Auth.DTOs.Request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/** Body for the batch-terminate endpoint: { "sessionIds": [...] }. */
@Data
public class TerminateSessionsRequest {
    @NotEmpty(message = "sessionIds must not be empty")
    private List<UUID> sessionIds;
}
