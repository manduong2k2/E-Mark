package com.Marketplace_Management.Auth.Repositories;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.Marketplace_Management.Auth.Entities.UserSessionEntity;

public interface UserSessionJpaRepository extends JpaRepository<UserSessionEntity, UUID> {

    @Query("SELECT s FROM UserSessionEntity s WHERE s.user.id IN :userIds")
    List<UserSessionEntity> findByUserIdIn(@Param("userIds") Collection<UUID> userIds);

    @Modifying
    @Query("DELETE FROM UserSessionEntity s WHERE s.refreshJti = :jti")
    int deleteByRefreshJti(@Param("jti") String jti);

    @Modifying
    @Query("DELETE FROM UserSessionEntity s WHERE s.user.id = :userId AND s.accessJti = :jti")
    int deleteByUserIdAndAccessJti(@Param("userId") UUID userId, @Param("jti") String jti);

    @Modifying
    @Query("DELETE FROM UserSessionEntity s WHERE s.id IN :ids")
    int deleteByIds(@Param("ids") Collection<UUID> ids);

    @Modifying
    @Query("DELETE FROM UserSessionEntity s WHERE s.accessExpiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
