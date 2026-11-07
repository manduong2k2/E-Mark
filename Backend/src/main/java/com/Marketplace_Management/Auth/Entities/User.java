package com.Marketplace_Management.Auth.Entities;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.Marketplace_Management.Shared.Entities.UuidEntity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Set;
import lombok.experimental.SuperBuilder;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Entity
@Data
@Table(name = "users")
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
@NoArgsConstructor
public class User extends UuidEntity {
    @Column(unique = true, columnDefinition = "varchar(255)", nullable = false)
    private String email;

    @Column(columnDefinition = "varchar(200)", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private String password;

    @Column(columnDefinition = "varchar(255)", nullable = true)
    private String name;

    @Column(columnDefinition = "varchar(255)", nullable = true)
    private String avatar;

    @Column(unique = true, columnDefinition = "varchar(255)", nullable = true)
    private String phone;

    @Column(name = "status", length = 20, columnDefinition = "varchar(20) default 'INACTIVE'")
    private String status;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Role> roles = new java.util.HashSet<>();



}
