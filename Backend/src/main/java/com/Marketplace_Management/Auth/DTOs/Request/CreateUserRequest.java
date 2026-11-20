package com.Marketplace_Management.Auth.DTOs.Request;

import java.util.List;
import java.util.UUID;

import com.Marketplace_Management.Shared.Annotation.Rules.Unique;

import jakarta.validation.constraints.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    @NotBlank(message = "Email must not be empty")
    @Unique(table = "users", deletedAtColumn = "deleted_at", column = "email", message = "Email already exists")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password must not be empty")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    // Uniqueness is checked in the service, after blank is normalized to null
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    // Defaults to ACTIVE: accounts created by an admin do not need email activation
    @Pattern(regexp = "ACTIVE|INACTIVE", message = "status must be ACTIVE or INACTIVE")
    private String status;

    // Defaults to the USER role when empty
    private List<UUID> roleIds;
}
