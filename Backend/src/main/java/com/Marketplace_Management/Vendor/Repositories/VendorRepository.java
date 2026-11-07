package com.Marketplace_Management.Vendor.Repositories;

import com.Marketplace_Management.Vendor.Contracts.IVendorRepository;
import com.Marketplace_Management.Vendor.DTOs.Command.GetListVendorCommand;
import com.Marketplace_Management.Vendor.DTOs.Response.VendorResponse;
import com.Marketplace_Management.Vendor.Entities.VendorEntity;

import tools.jackson.databind.ObjectMapper;

import com.Marketplace_Management.Shared.Utils.QueryBuilder.EntityMetadataRegistry;
import com.Marketplace_Management.Shared.Utils.QueryBuilder.QueryBuilder;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class VendorRepository implements IVendorRepository {

    private final VendorJpaRepository jpaRepository;
    private final DSLContext dslContext;
    private final EntityMetadataRegistry metadataRegistry;
    private final ObjectMapper objectMapper;

    public VendorRepository(VendorJpaRepository jpaRepository, DSLContext dslContext, EntityMetadataRegistry metadataRegistry, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.dslContext = dslContext;
        this.metadataRegistry = metadataRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<VendorResponse> findAll(GetListVendorCommand command) {
        QueryBuilder<VendorEntity> qb = new QueryBuilder<>(dslContext, metadataRegistry, objectMapper);
        qb.query(VendorEntity.class)
        .when(command.getSearch() != null, q -> {
                q.where("name", "like", "%" + command.getSearch() + "%");
        });

        var data = qb.get()
                .stream()
                .map(item -> qb.to(item, VendorResponse.class))
                .collect(Collectors.toList());

        return data;
    }

    @Override
    public VendorEntity save(VendorEntity vendor) {
        return jpaRepository.save(vendor);
    }

    @Override
    public Optional<VendorEntity> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<VendorEntity> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId);
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).isPresent();
    }
}
