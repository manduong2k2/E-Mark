package com.Marketplace_Management.Catalog.Entities;

import java.util.List;

import org.hibernate.annotations.Nationalized;

import com.Marketplace_Management.Shared.Entities.UuidEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "brands")
@Data
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
@NoArgsConstructor
public class Brand extends UuidEntity {
    @Column(nullable = false)
    @Size(max = 100)
    @Nationalized
    private String name;

    @Column(nullable = true)
    private String image;

    @Column(nullable = true, length = 500)
    @Nationalized
    private String description;

    @OneToMany(mappedBy = "brand")
    private List<Product> products;


}