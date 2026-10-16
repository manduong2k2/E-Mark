package com.Marketplace_Management.Auth.DTOs.Response;

import java.sql.Timestamp;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Admin view of a single user session row. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionResponse {
    private UUID id;
    private UUID userId;
    // Embedded user info (joined from users table)
    private String userName;
    private String userEmail;
    private String userAvatar;
    private String ipAddress;
    private Timestamp loginAt;
    private Timestamp expireAt;   // access_expires_at
}
