package com.Marketplace_Management.Auth.Repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.Marketplace_Management.Auth.Entities.UserEntity;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
    @EntityGraph(attributePaths = "roles")
    Optional<UserEntity> findByEmail(String email);
    @EntityGraph(attributePaths = "roles")
    Optional<UserEntity> findByPhone(String phone);
    @Override
    @EntityGraph(attributePaths = "roles")
    Optional<UserEntity> findById(UUID id);
}
