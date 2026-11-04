package com.Marketplace_Management.Shared.DTOs.Responses;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.auto.value.AutoValue.Builder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@JsonPropertyOrder ({"id", "url"})
@Builder 
@AllArgsConstructor 
@NoArgsConstructor 
public class FileResponse {
    private String id;
    private String url;

    public FileResponse withBaseUrl(String baseUrl) {
        this.url = baseUrl + "/" + this.url;
        return this;
    }
}
