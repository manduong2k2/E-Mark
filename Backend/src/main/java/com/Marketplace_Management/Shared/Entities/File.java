package com.Marketplace_Management.Shared.Entities;

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
public class File extends UuidEntity {

    @Column(nullable = false)
    private String url;

    @Any
    @Column(name = "entity_type", columnDefinition = "VARCHAR(255)")
    @JoinColumn(name = "entity_id")
    @AnyKeyJavaClass(UUID.class)
    private Object entity;

    public File() {
    }

    public File(UUID id, String url, Object entity) {
        this.setId(id);
        this.url = url;
        this.entity = entity;
    }
}
