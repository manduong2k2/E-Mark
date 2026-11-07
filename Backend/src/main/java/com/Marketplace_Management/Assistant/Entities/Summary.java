package com.Marketplace_Management.Assistant.Entities;

import com.Marketplace_Management.Shared.Entities.UuidEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "summaries")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@SuperBuilder
public class Summary extends UuidEntity {
    
    @Column(nullable = false)
    private String content;
    
    @Column(nullable = false)
    private String title;
}
