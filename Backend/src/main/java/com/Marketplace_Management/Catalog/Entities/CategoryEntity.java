package com.Marketplace_Management.Catalog.Entities;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Nationalized;

import com.Marketplace_Management.Shared.Entities.UuidEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.Builder;

@Entity
@Table(name = "categories")
@Data
@EqualsAndHashCode(callSuper = false, exclude = {"parent", "children"})
@ToString(exclude = {"parent", "children"})
@NoArgsConstructor
@SuperBuilder
public class CategoryEntity extends UuidEntity {
    @Column(nullable = false)
    @Size(max = 100)
    @Nationalized
    private String name;

    @Column(nullable = true)
    private String image;

    @Column(nullable = true, length = 500)
    @Nationalized
    private String description;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private CategoryEntity parent;

    @OneToMany(mappedBy = "parent",
               cascade = CascadeType.ALL,
               orphanRemoval = true)
    @Builder.Default
    private List<CategoryEntity> children = new ArrayList<>();

}
