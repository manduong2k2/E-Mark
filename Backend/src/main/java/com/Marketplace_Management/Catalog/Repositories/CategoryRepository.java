package com.Marketplace_Management.Catalog.Repositories;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.Marketplace_Management.Catalog.Contracts.ICategoryRepository;
import com.Marketplace_Management.Catalog.DTOs.Commands.Category.GetListCategoryCommand;
import com.Marketplace_Management.Catalog.Entities.CategoryEntity;
import com.Marketplace_Management.Shared.DTOs.Responses.PaginatedResponse;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.EntityManager;

@Repository
public class CategoryRepository implements ICategoryRepository {

    private final CategoryJpaRepository jpaRepository;
    private final EntityManager entityManager;

    public CategoryRepository(CategoryJpaRepository jpaRepository, EntityManager entityManager) {
        this.jpaRepository = jpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    public PaginatedResponse<CategoryEntity> findAll(GetListCategoryCommand command) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<CategoryEntity> query = builder.createQuery(CategoryEntity.class);

        Root<CategoryEntity> root = query.from(CategoryEntity.class);

        List<Predicate> predicates = new ArrayList<>();

        if (command.getSearch() != null && !command.getSearch().trim().isEmpty()) {
            String searchPattern = "%" + command.getSearch().toLowerCase() + "%";
            predicates.add(
                builder.or(
                    builder.like(builder.lower(root.get("name")), searchPattern),
                    builder.like(builder.lower(root.get("description")), searchPattern)
                )
            );
        }

        query.where(predicates.toArray(new Predicate[0]));

        // Sorting
        if (command.getSortBy() != null && !command.getSortBy().trim().isEmpty()) {
            jakarta.persistence.criteria.Path<Object> sortPath = root.get(command.getSortBy());
            Order order;
            if ("desc".equalsIgnoreCase(command.getSortOrder())) {
                order = builder.desc(sortPath);
            } else {
                order = builder.asc(sortPath);
            }
            query.orderBy(order);
        }

        // Get total count
        CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
        Root<CategoryEntity> countRoot = countQuery.from(CategoryEntity.class);
        List<Predicate> countPredicates = new ArrayList<>();

        if (command.getSearch() != null && !command.getSearch().trim().isEmpty()) {
            String searchPattern = "%" + command.getSearch().toLowerCase() + "%";
            countPredicates.add(
                builder.or(
                    builder.like(builder.lower(countRoot.get("name")), searchPattern),
                    builder.like(builder.lower(countRoot.get("description")), searchPattern)
                )
            );
        }

        countQuery.where(countPredicates.toArray(new Predicate[0]));
        countQuery.select(builder.count(countRoot));
        Long totalElements = entityManager.createQuery(countQuery).getSingleResult();

        // Pagination
        int firstResult = command.getPage() * command.getSize();
        int maxResults = command.getSize();

        List<CategoryEntity> entities = entityManager.createQuery(query)
                .setFirstResult(firstResult)
                .setMaxResults(maxResults)
                .getResultList();

        return PaginatedResponse.of(entities, command.getPage(), command.getSize(), totalElements);
    }

    @Override
    public CategoryEntity save(CategoryEntity category) {
        return jpaRepository.save(category);
    }

    @Override
    public Optional<CategoryEntity> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<CategoryEntity> findByName(String name) {
        return jpaRepository.findByName(name);
    }
    
    @Override
    public CategoryEntity update(CategoryEntity category) {
        return save(category);
    }
    
    @Override
    public void delete(UUID id) {
        jpaRepository.deleteById(id);
    }
}
