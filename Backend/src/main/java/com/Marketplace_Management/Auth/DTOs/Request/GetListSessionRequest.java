package com.Marketplace_Management.Auth.DTOs.Request;

import java.util.UUID;

import com.Marketplace_Management.Shared.DTOs.Requests.PageRequest;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GetListSessionRequest extends PageRequest {

    @Nullable
    private UUID userId;

    @Nullable
    private Long createdFrom;

    @Nullable
    private Long createdTo;

    public GetListSessionRequest() {
        super("loginAt", "desc", 10);
    }

    @Override
    @Max(value = 100, message = "size must not exceed 100")
    public int getSize() {
        return super.getSize();
    }

    @Override
    @Pattern(regexp = "loginAt|ipAddress|expireAt", message = "sortBy must be one of: loginAt, ipAddress, expireAt")
    public String getSortBy() {
        return super.getSortBy();
    }
}
