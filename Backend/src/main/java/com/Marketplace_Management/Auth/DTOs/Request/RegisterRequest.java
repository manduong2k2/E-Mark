package com.Marketplace_Management.Auth.DTOs.Request;

import com.Marketplace_Management.Shared.Annotation.Rules.Unique;

import jakarta.validation.constraints.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
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
    
    @Unique(table = "users", deletedAtColumn = "deleted_at", column = "phone", message = "Phone already exists")
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;
}
