package com.Marketplace_Management.Auth.DTOs.Response;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import com.Marketplace_Management.Auth.Entities.RoleEntity;
import com.Marketplace_Management.Auth.Entities.UserEntity;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;
import com.Marketplace_Management.Shared.Utils.Helpers.UrlHelper;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonPropertyOrder({"id", "email", "name", "avatar", "phone", "status", "roles", "createdAt" })
public class ProfileResponse {
    private UUID id;
    private String email;
    private String name;
    private String avatar;
    private String phone;
    private String status;
    private Set<String> roles;
    private LocalDateTime createdAt;

    public ProfileResponse(UserEntity user) {
        if (user == null) {
            return;
        }
        this.id = user.getId();
        this.email = user.getEmail();
        this.name = user.getName();
        this.avatar = user.getAvatar();
        this.phone = user.getPhone();
        this.status = user.getStatus();
        this.roles = user.getRoles() != null
            ? user.getRoles().stream().map(RoleEntity::getName).collect(java.util.stream.Collectors.toSet())
            : null;
        this.createdAt = user.getCreatedAt();
    }

    public ProfileResponse withUrl(String baseUrl) {
        this.avatar = UrlHelper.toPublicUrl(baseUrl, this.avatar);
        return this;
    }
}
