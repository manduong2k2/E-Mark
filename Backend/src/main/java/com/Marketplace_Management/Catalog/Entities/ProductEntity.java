package com.Marketplace_Management.Catalog.Entities;

import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.Marketplace_Management.Shared.Entities.UuidEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
    name = "products",
    indexes = {
        @Index(name = "idx_product_brand_price", columnList = "brand_id, price"),
        @Index(name = "idx_product_status", columnList = "status")
    }
)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(onlyExplicitlyIncluded = true)
@Data
@SuperBuilder
public class ProductEntity extends UuidEntity {
    @EqualsAndHashCode.Include
    @Column(nullable = false)
    @Size(max = 100)
    @Nationalized
    private String name;

    @Column(nullable = false)
    private String status;

    @Column(nullable = true)
    @Size(max = 500)
    @Nationalized
    private String description;

    // relationships

    @ManyToOne
    @JoinColumn(name = "brand_id", nullable = false)
    private BrandEntity brand;

    @ManyToMany
    @JoinTable(
        name = "product_category", 
        joinColumns = @JoinColumn(name = "product_id"), 
        inverseJoinColumns = @JoinColumn(name = "category_id"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "category_id"})
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<CategoryEntity> categories;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProductVariantEntity> variants;

    @Column(nullable = true)
    private UUID vendorId;

    public ProductEntity() {
    }
}
