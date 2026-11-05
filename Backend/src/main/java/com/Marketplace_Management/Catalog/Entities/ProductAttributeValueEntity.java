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
@Table(name = "product_attribute_values", uniqueConstraints = @UniqueConstraint(
        name = "uk_product_attribute_values_attribute_value", columnNames = {"product_attribute_id", "value"}))
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@ToString(onlyExplicitlyIncluded = true)
public class ProductAttributeValueEntity extends NumericEntity {
    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    @Nationalized
    @ToString.Include
    private String value;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_attribute_id", nullable = false)
    private ProductAttributeEntity productAttribute;
}
