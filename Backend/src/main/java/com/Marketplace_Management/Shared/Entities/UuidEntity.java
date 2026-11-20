package com.Marketplace_Management.Shared.Entities;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@MappedSuperclass
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@ToString
public abstract class UuidEntity extends JpaEntity {
    @Id
    // UUID v7: time-ordered, so new rows are appended to the primary-key index
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    protected UUID id;
}
