package com.Marketplace_Management.Order.Repositories;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.Marketplace_Management.Order.Contracts.IOrderRepository;
import com.Marketplace_Management.Order.DTOs.Commands.ListOrderCommand;
import com.Marketplace_Management.Order.DTOs.Responses.HistoryResponse;
import com.Marketplace_Management.Order.Entities.OrderEntity;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;
import com.Marketplace_Management.Shared.Errors.Exceptions.ResourceNotFoundException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@Repository
public class OrderRepository implements IOrderRepository {
    // Columns a client may sort by (sortBy goes into the query, so it must be whitelisted)
    private static final Set<String> SORTABLE = Set.of("createdAt", "updatedAt", "total", "status", "name");


    private final OrderJpaRepository jpaRepository;
    private final EntityManager entityManager;

    public OrderRepository(OrderJpaRepository jpaRepository, EntityManager entityManager) {
        this.jpaRepository = jpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    public PaginatedResponse<HistoryResponse> findAll(ListOrderCommand command) {
        return findByUserIdWithFilters(null, command);
    }

    @Override
    public PaginatedResponse<HistoryResponse> findByUserIdWithFilters(UUID userId, ListOrderCommand command) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<HistoryResponse> query = cb.createQuery(HistoryResponse.class);
        Root<OrderEntity> root = query.from(OrderEntity.class);

        query.select(cb.construct(
                HistoryResponse.class,
                root.get("id").as(UUID.class),
                root.get("userId").as(UUID.class),
                root.get("status"),
                root.get("total").as(Double.class),
                root.get("name"),
                root.get("phone"),
                root.get("address"),
                root.get("note"),
                root.get("createdAt"),
                root.get("updatedAt")));

        query.where(filters(cb, root, userId, command));
        String sortBy = SORTABLE.contains(command.getSortBy()) ? command.getSortBy() : "createdAt";
        query.orderBy("asc".equalsIgnoreCase(command.getSortOrder()) ? cb.asc(root.get(sortBy)) : cb.desc(root.get(sortBy)));

        int page = command.getPage();
        int size = command.getSize();

        // The count needs its own root: predicates are bound to the root they were built from
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<OrderEntity> countRoot = countQuery.from(OrderEntity.class);
        countQuery.select(cb.count(countRoot)).where(filters(cb, countRoot, userId, command));
        Long totalElements = entityManager.createQuery(countQuery).getSingleResult();

        List<HistoryResponse> data = entityManager.createQuery(query)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();

        return PaginatedResponse.of(data, page, size, totalElements);
    }

    private Predicate[] filters(CriteriaBuilder cb, Root<OrderEntity> root, UUID userId, ListOrderCommand command) {
        List<Predicate> predicates = new ArrayList<>();
        if (userId != null) {
            predicates.add(cb.equal(root.get("userId"), userId));
        }
        if (command.getSearch() != null && !command.getSearch().isEmpty()) {
            String searchTerm = "%" + command.getSearch().toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("name")), searchTerm),
                    cb.like(cb.lower(root.get("phone")), searchTerm),
                    cb.like(cb.lower(root.get("note")), searchTerm)));
        }
        if (command.getStatus() != null && !command.getStatus().isEmpty()) {
            predicates.add(cb.equal(root.get("status"), command.getStatus()));
        }
        if (command.getTotalMin() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("total"), command.getTotalMin()));
        }
        if (command.getTotalMax() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("total"), command.getTotalMax()));
        }
        if (command.getDateFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), command.getDateFrom()));
        }
        if (command.getDateTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), command.getDateTo()));
        }
        return predicates.toArray(new Predicate[0]);
    }

    @Override
    public OrderEntity create(OrderEntity order) {
        return jpaRepository.save(order);
    }

    @Override
    public Optional<OrderEntity> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public OrderEntity updateStatus(UUID id, String status) {
        OrderEntity entity = jpaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        entity.changeStatus(status);
        return jpaRepository.save(entity);
    }

    @Override
    public void delete(UUID id) {
        jpaRepository.deleteById(id);
    }
}
