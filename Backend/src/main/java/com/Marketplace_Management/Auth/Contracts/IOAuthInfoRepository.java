package com.Marketplace_Management.Auth.Contracts;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.Marketplace_Management.Auth.Constants.OAuthProvider;
import com.Marketplace_Management.Auth.Entities.OAuthInfo;

public interface IOAuthInfoRepository {
    Optional<OAuthInfo> findByProviderAndSubject(OAuthProvider provider, String subject);
    OAuthInfo save(OAuthInfo info);
    /** userId -> providers linked to that user (users without links are absent). */
    Map<UUID, List<OAuthProvider>> findProvidersByUserIds(Collection<UUID> userIds);
}
