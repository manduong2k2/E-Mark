package com.Marketplace_Management.Vendor.Entities;

import java.util.UUID;

import org.hibernate.annotations.Nationalized;

import com.Marketplace_Management.Shared.Entities.UuidEntity;
import com.Marketplace_Management.Vendor.Constants.VendorStatus;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "vendors")
@Data
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
@NoArgsConstructor
public class VendorEntity extends UuidEntity {
    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VendorStatus status = VendorStatus.PENDING;

    @Column(length = 500)
    @Nationalized
    private String description;

    @Column(length = 500)
    private String logo;

    @Column(length = 500)
    private String banner;

    @Column(length = 100, unique = true)
    private String taxCode;

    @Column(length = 100, unique = true)
    private String email;

    @Column(length = 100, unique = true)
    private UUID addressId;

    @Column(length = 15, unique = true)
    private String phone;

    public void activate() {
        if (status != VendorStatus.PENDING) {
            throw new IllegalStateException("Vendor must be PENDING to activate");
        }
        status = VendorStatus.ACTIVE;
    }

    public void ban() {
        status = VendorStatus.BANNED;
    }
}
