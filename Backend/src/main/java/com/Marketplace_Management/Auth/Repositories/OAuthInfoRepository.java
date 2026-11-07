package com.Marketplace_Management.Auth.Repositories;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.Marketplace_Management.Auth.Constants.OAuthProvider;
import com.Marketplace_Management.Auth.Contracts.IOAuthInfoRepository;
import com.Marketplace_Management.Auth.Entities.OAuthInfo;

@Repository
public class OAuthInfoRepository implements IOAuthInfoRepository {
    private final OAuthInfoJpaRepository jpaRepository;

    public OAuthInfoRepository(OAuthInfoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<OAuthInfo> findByProviderAndSubject(OAuthProvider provider, String subject) {
        return jpaRepository.findByOauthProviderAndOauthProviderSubject(provider, subject);
    }

    @Override
    public OAuthInfo save(OAuthInfo info) {
        return jpaRepository.save(info);
    }

    @Override
    public Map<UUID, List<OAuthProvider>> findProvidersByUserIds(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return jpaRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(
                        OAuthInfo::getUserId,
                        Collectors.mapping(OAuthInfo::getOauthProvider, Collectors.toList())));
    }

}
