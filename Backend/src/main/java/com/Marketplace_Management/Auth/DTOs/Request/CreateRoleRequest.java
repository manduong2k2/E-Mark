package com.Marketplace_Management.Auth.DTOs.Request;

import com.Marketplace_Management.Shared.Annotation.Rules.Unique;

import jakarta.validation.constraints.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoleRequest {
    @NotBlank(message = "Name must not be empty")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    // Stored in JWT "roles" and checked by hasAuthority(...), e.g. SUPPORT, CONTENT_MANAGER
    @NotBlank(message = "Code must not be empty")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "Code must be UPPER_SNAKE_CASE (A-Z, 0-9, _)")
    @Unique(table = "roles", deletedAtColumn = "deleted_at", column = "code", message = "Code already exists")
    private String code;
}
