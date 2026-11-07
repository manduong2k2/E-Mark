package com.Marketplace_Management.Vendor.Entities;

import com.Marketplace_Management.Shared.Entities.UuidEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "collections")
@SuperBuilder
@NoArgsConstructor
public class Collection extends UuidEntity {
    @Column(nullable = false)
    private String name;

    @Column(nullable = true)
    private Integer displayOrder;

    @ManyToOne
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;


}
