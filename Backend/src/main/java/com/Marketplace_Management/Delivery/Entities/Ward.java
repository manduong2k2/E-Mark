package com.Marketplace_Management.Delivery.Entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(
    name = "wards",
    indexes = {
        @Index(name = "idx_wards_province", columnList = "province_code")
    }
)
@Data
public class Ward {

    @Id
    @Column(length = 20)
    private String id;

    @Column(nullable = false)
    private String name;

    private String nameEn;

    private String fullName;

    private String fullNameEn;

    private String codeName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_id")
    private Province province;

    // getters/setters
}