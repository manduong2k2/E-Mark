package com.Marketplace_Management.Vendor.DTOs.Request;

import org.springframework.web.multipart.MultipartFile;

import com.Marketplace_Management.Shared.Annotation.Rules.Uuid;
import com.Marketplace_Management.Shared.Annotation.Rules.Unique;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterVendorRequest implements VendorProfileFields {
    @NotBlank(message = "Vendor name is required")
    @Size(min = 1, max = 100, message = "Vendor name must be between 1 and 100 characters")
    private String name;

    @Nullable
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Nullable
    private MultipartFile logo;

    @Nullable
    private MultipartFile banner;

    @Nullable
    @NotBlank(message = "Tax code is required")
    @Pattern(regexp = "^[A-Za-z0-9]{5,100}$", message = "Tax code must contain only alphanumeric characters and be 5 to 100 characters long")
    @Size(max = 100, message = "Tax code must not exceed 100 characters")
    private String taxCode;

    @Nullable
    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Unique(table = "vendors", deletedAtColumn = "deleted_at", column = "email", message = "Email already registered")
    private String email;

    @Nullable
    @Uuid(message = "Address ID must be a valid id")
    private String addressId;

    @Nullable
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Phone number must be valid")
    @Size(max = 15, message = "Phone must not exceed 15 characters")
    @Unique(table = "vendors", deletedAtColumn = "deleted_at", column = "phone", message = "Phone already registered")
    private String phone;
}
