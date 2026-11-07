package com.Marketplace_Management.Auth.Entities;

import java.util.UUID;

import com.Marketplace_Management.Shared.Entities.UuidEntity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "roles")
@Data
@EqualsAndHashCode(callSuper = false)
public class Role extends UuidEntity {

    @Column(unique = false, nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String code;

    public Role() {}

    public Role(UUID id, String name, String code) {
        this.id = id;
        this.name = name;
        this.code = code;
    }
}