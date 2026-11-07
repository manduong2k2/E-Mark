package com.Marketplace_Management.Order.Repositories;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.Marketplace_Management.Order.Contracts.IOrderRepository;
import com.Marketplace_Management.Order.DTOs.Commands.ListOrderCommand;
import com.Marketplace_Management.Order.DTOs.Responses.HistoryResponse;
import com.Marketplace_Management.Order.Entities.Order;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;
import com.Marketplace_Management.Shared.Errors.Exceptions.ResourceNotFoundException;
import com.Marketplace_Management.Shared.Utils.QueryBuilder.EntityMetadataRegistry;
import com.Marketplace_Management.Shared.Utils.QueryBuilder.QueryBuilder;
import com.Marketplace_Management.Shared.Utils.QueryBuilder.QueryResults;

import tools.jackson.databind.ObjectMapper;

@Repository
public class OrderRepository implements IOrderRepository {
    private static final Set<String> SORTABLE = Set.of("createdAt", "updatedAt", "total", "status", "name");

    private final OrderJpaRepository jpaRepository;
    private final DSLContext dslContext;
    private final EntityMetadataRegistry metadataRegistry;
    private final ObjectMapper objectMapper;

    public OrderRepository(OrderJpaRepository jpaRepository, DSLContext dslContext,
            EntityMetadataRegistry metadataRegistry, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.dslContext = dslContext;
        this.metadataRegistry = metadataRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public PaginatedResponse<HistoryResponse> findAll(ListOrderCommand command) {
        return findByUserIdWithFilters(null, command);
    }

    @Override
    public PaginatedResponse<HistoryResponse> findByUserIdWithFilters(UUID userId, ListOrderCommand command) {
        String sortBy = command.getSortBy() != null && SORTABLE.contains(command.getSortBy())
                ? command.getSortBy() : "createdAt";
        String sortOrder = "asc".equalsIgnoreCase(command.getSortOrder()) ? "ASC" : "DESC";

        QueryBuilder<Order> queryBuilder = new QueryBuilder<>(dslContext, metadataRegistry, objectMapper);
        queryBuilder.query(Order.class)
                .select("id", "userId", "status", "total", "name", "phone", "address", "note", "createdAt", "updatedAt")
                // Group the search alternatives before AND-ing ownership and the other filters.
                .when(command.getSearch() != null && !command.getSearch().isEmpty(), query -> {
                    String pattern = "%" + command.getSearch() + "%";
                    query.where("name", "ILIKE", pattern)
                            .orWhere("phone", "ILIKE", pattern)
                            .orWhere("note", "ILIKE", pattern);
                })
                .when(userId != null, query -> query.where("userId", "=", userId))
                .when(command.getStatus() != null && !command.getStatus().isEmpty(),
                        query -> query.where("status", "=", command.getStatus()))
                .when(command.getTotalMin() != null, query -> query.where("total", ">=", command.getTotalMin()))
                .when(command.getTotalMax() != null, query -> query.where("total", "<=", command.getTotalMax()))
                .when(command.getDateFrom() != null, query -> query.where("createdAt", ">=", command.getDateFrom()))
                .when(command.getDateTo() != null, query -> query.where("createdAt", "<=", command.getDateTo()))
                .orderBy(sortBy, sortOrder);

        long total = queryBuilder.count();
        int offset = command.getPage() * command.getSize();
        List<HistoryResponse> data = queryBuilder.get(command.getSize(), offset).stream()
                .map(row -> queryBuilder.to(QueryResults.normalize(row), HistoryResponse.class))
                .toList();

        return PaginatedResponse.of(data, command.getPage(), command.getSize(), total);
    }

    @Override
    public Order create(Order order) {
        return jpaRepository.save(order);
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Order updateStatus(UUID id, String status) {
        Order entity = jpaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        entity.changeStatus(status);
        return jpaRepository.save(entity);
    }

    @Override
    public void delete(UUID id) {
        jpaRepository.deleteById(id);
    }
}
