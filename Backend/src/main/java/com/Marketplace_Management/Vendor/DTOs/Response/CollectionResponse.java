package com.Marketplace_Management.Vendor.DTOs.Response;

import java.util.UUID;

import com.Marketplace_Management.Vendor.Entities.CollectionEntity;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonPropertyOrder({"id", "name", "displayOrder"})
public class CollectionResponse {
    private UUID id;
    private String name;
    private Integer displayOrder;
    
    public CollectionResponse(CollectionEntity collection) {
        if (collection == null) {
            return;
        }
        this.id = collection.getId();
        this.name = collection.getName();
        this.displayOrder = collection.getDisplayOrder();
    }
}
