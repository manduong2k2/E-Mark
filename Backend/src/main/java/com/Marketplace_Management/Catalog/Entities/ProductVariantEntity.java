package com.Marketplace_Management.Catalog.Entities;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.SQLRestriction;

import com.Marketplace_Management.Shared.Entities.UuidEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.Builder;

@Entity
@Table(name = "product_variants")
@Data
@NoArgsConstructor
@SuperBuilder
public class ProductVariantEntity extends UuidEntity{
    @Column(nullable = false)
    @Size(max = 100)
    @Nationalized
    private String name;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private double price;

    @Column(nullable = false, columnDefinition = "int default 0")
    private int stock;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "entity_id", referencedColumnName = "id", insertable = false, updatable = false)
    @SQLRestriction("entity_type = 'com.Marketplace_Management.Catalog.Entities.ProductVariantEntity'")
    @ToString.Exclude
    private List<FileEntity> images;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    private ProductEntity product;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "product_variant_attribute_values",
        joinColumns = @JoinColumn(name = "variant_id", nullable = false),
        inverseJoinColumns = @JoinColumn(name = "attribute_value_id", nullable = false),
        uniqueConstraints = @UniqueConstraint(
            name = "uk_product_variant_attribute_values",
            columnNames = {"variant_id", "attribute_value_id"}
        )
    )
    @Builder.Default
    @ToString.Exclude
    private Set<ProductAttributeValueEntity> attributeValues = new HashSet<>();

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof ProductVariantEntity variant
                && getId() != null && getId().equals(variant.getId());
    }

    @Override
    public int hashCode() {
        return ProductVariantEntity.class.hashCode();
    }

}
