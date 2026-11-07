package com.Marketplace_Management.Auth.Repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.Marketplace_Management.Auth.Entities.User;

public interface UserJpaRepository extends JpaRepository<User, UUID> {
    @EntityGraph(attributePaths = "roles")
    Optional<User> findByEmail(String email);
    @EntityGraph(attributePaths = "roles")
    Optional<User> findByPhone(String phone);
    @Override
    @EntityGraph(attributePaths = "roles")
    Optional<User> findById(UUID id);
}
