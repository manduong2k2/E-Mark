package com.Marketplace_Management.Auth.DTOs.Commands;

import java.time.Instant;
import java.util.UUID;

import com.Marketplace_Management.Auth.DTOs.Request.GetListSessionRequest;
import com.Marketplace_Management.Shared.DTOs.Commands.PageCommand;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GetListSessionCommand extends PageCommand {
    private UUID userId;
    private Instant createdFrom;
    private Instant createdTo;

    public static GetListSessionCommand fromRequest(GetListSessionRequest request) {
        return GetListSessionCommand.builder()
                .paging(request)
                .userId(request.getUserId())
                .createdFrom(request.getCreatedFrom() != null ? Instant.ofEpochMilli(request.getCreatedFrom()) : null)
                .createdTo(request.getCreatedTo() != null ? Instant.ofEpochMilli(request.getCreatedTo()) : null)
                .build();
    }
}
