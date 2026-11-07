package com.Marketplace_Management.Assistant.Entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.Marketplace_Management.Shared.Entities.UuidEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity 
@Table(name = "messages")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@SuperBuilder
public class Message extends UuidEntity {

    @Column(nullable = false)
    private String content;
    
    @Column(nullable = false)
    private String role;
    
    @Column(nullable = false)
    private String metadata;
}
