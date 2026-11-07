package com.Marketplace_Management.Auth.Contracts;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.Marketplace_Management.Auth.Constants.OAuthProvider;
import com.Marketplace_Management.Auth.Entities.OAuthInfoEntity;

public interface IOAuthInfoRepository {
    Optional<OAuthInfoEntity> findByProviderAndSubject(OAuthProvider provider, String subject);
    OAuthInfoEntity save(OAuthInfoEntity info);
    /** userId -> providers linked to that user (users without links are absent). */
    Map<UUID, List<OAuthProvider>> findProvidersByUserIds(Collection<UUID> userIds);
}
