package com.Marketplace_Management.Catalog.Entities;

import com.Marketplace_Management.Shared.Entities.UuidEntity;

import java.util.UUID;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyKeyJavaClass;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "files")
@EqualsAndHashCode(callSuper = false, exclude = "entity")
@ToString(exclude = "entity")
@Data
public class FileEntity extends UuidEntity {

    @Column(nullable = false)
    private String url;

    @Any
    @Column(name = "entity_type", columnDefinition = "VARCHAR(255)")
    @JoinColumn(name = "entity_id")
    @AnyKeyJavaClass(UUID.class)
    private Object entity;

    public FileEntity() {
    }

    public FileEntity(UUID id, String url, Object entity) {
        this.setId(id);
        this.url = url;
        this.entity = entity;
    }
}
