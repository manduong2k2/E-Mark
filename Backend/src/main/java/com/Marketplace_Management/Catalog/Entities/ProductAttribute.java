package com.Marketplace_Management.Catalog.Entities;

import org.hibernate.annotations.Nationalized;

import com.Marketplace_Management.Shared.Entities.NumericEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "product_attributes", uniqueConstraints = @UniqueConstraint(
        name = "uk_product_attributes_product_code", columnNames = {"product_id", "code"}))
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@ToString(onlyExplicitlyIncluded = true)
public class ProductAttribute extends NumericEntity {
    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    @ToString.Include
    private String code;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    @Nationalized
    @ToString.Include
    private String name;

    @Size(max = 255)
    @Column(length = 255)
    private String icon;

    @Size(max = 500)
    @Column(length = 500)
    @Nationalized
    private String description;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
}
