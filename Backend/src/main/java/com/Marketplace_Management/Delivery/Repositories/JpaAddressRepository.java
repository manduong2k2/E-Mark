package com.Marketplace_Management.Delivery.Repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.Marketplace_Management.Delivery.Entities.Address;

public interface JpaAddressRepository extends JpaRepository<Address, Long> {
    /** Default address first, then the most recent. */
    @EntityGraph(attributePaths = { "ward", "ward.province" })
    List<Address> findByUserIdOrderByIsDefaultDescCreatedAtDesc(UUID userId);

    @EntityGraph(attributePaths = { "ward", "ward.province" })
    Optional<Address> findByIdAndUserId(Long id, UUID userId);

    @EntityGraph(attributePaths = { "ward", "ward.province" })
    Optional<Address> findFirstByUserIdAndIsDefaultTrue(UUID userId);

    @EntityGraph(attributePaths = { "ward", "ward.province" })
    Optional<Address> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.userId = :userId AND a.isDefault = true")
    int clearDefault(@Param("userId") UUID userId);
}
