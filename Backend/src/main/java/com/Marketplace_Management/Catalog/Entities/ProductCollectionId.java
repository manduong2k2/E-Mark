package com.Marketplace_Management.Catalog.Entities;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class ProductCollectionId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "collection_id")
    private UUID collectionId;
}
