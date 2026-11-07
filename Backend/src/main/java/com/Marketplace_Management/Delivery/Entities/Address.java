package com.Marketplace_Management.Delivery.Entities;

import java.util.UUID;

import org.hibernate.annotations.Nationalized;

import com.Marketplace_Management.Shared.Entities.NumericEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A delivery address of a user: ward (which belongs to a province) + free-text detail
 * (house number, street, building...). Each user has at most one default address.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@SuperBuilder
@Table(name = "addresses", indexes = {
        @Index(name = "idx_address_ward", columnList = "wardId"),
        @Index(name = "idx_addresses_user_id", columnList = "user_id"),
})
public class Address extends NumericEntity {
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Nationalized
    @Column(nullable = false, length = 255)
    private String detail;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isDefault;

    @ManyToOne
    @JoinColumn(name = "ward_id", nullable = false)
    private Ward ward;

    public boolean isDefaultAddress() {
        return Boolean.TRUE.equals(isDefault);
    }

    /** Full address shown to the user and captured on an order. */
    public String fullAddress() {
        StringBuilder address = new StringBuilder(detail == null ? "" : detail);
        if (ward != null) {
            address.append(", ").append(ward.getFullName());
            if (ward.getProvince() != null) {
                address.append(", ").append(ward.getProvince().getFullName());
            }
        }
        return address.toString();
    }
}
