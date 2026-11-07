package com.Marketplace_Management.Catalog.Repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.Marketplace_Management.Catalog.DTOs.Commands.Category.GetListCategoryCommand;
import com.Marketplace_Management.Catalog.DTOs.Response.CategoryResponse;
import com.Marketplace_Management.Catalog.Entities.Category;
import com.Marketplace_Management.Shared.QueryBuilderDatabase;

import tools.jackson.databind.ObjectMapper;

class CategoryRepositoryQueryBuilderTest {
    private final CategoryJpaRepository jpa = mock(CategoryJpaRepository.class);

    @Test
    void filtersAndPaginatesWithQueryBuilderWhileKeepingSortOrderAndTheFullTree() {
        Category root = Category.builder().id(UUID.randomUUID()).name("Z Electronics").build();
        Category other = Category.builder().id(UUID.randomUUID()).name("A Electronics").build();
        Category child = Category.builder().id(UUID.randomUUID()).name("Phones").parent(root).build();
        Category grandchild = Category.builder().id(UUID.randomUUID()).name("iPhone").parent(child).build();
        root.getChildren().add(child);
        child.getChildren().add(grandchild);
        QueryBuilderDatabase database = new QueryBuilderDatabase("categories", 7, List.of(
                Map.of("categories_id", root.getId()), Map.of("categories_id", other.getId())));
        CategoryRepository repository = repository(database);
        when(jpa.findAllById(List.of(root.getId(), other.getId()))).thenReturn(List.of(other, root));

        var page = repository.findAll(GetListCategoryCommand.builder()
                .page(1).size(2).search("ElEcTrOnIcS").sortBy("name").sortOrder("desc").build());

        assertEquals(7, page.getTotalElements());
        assertEquals(4, page.getTotalPages());
        assertEquals(1, page.getCurrentPage());
        assertSame(root, page.getData().getFirst());
        assertSame(other, page.getData().get(1));
        assertEquals(grandchild.getId(), new CategoryResponse(page.getData().getFirst())
                .getChildren().getFirst().getChildren().getFirst().getId());
        assertEquals(3, database.calls().size());
        for (var call : database.calls()) {
            assertTrue(call.sql().contains("cast(categories.name as varchar) ilike ?"), call.sql());
            assertTrue(call.sql().contains("or cast(categories.description as varchar) ilike ?"), call.sql());
            assertTrue(call.sql().contains("categories.deleted_at is null"), call.sql());
            assertEquals(List.of("%ElEcTrOnIcS%", "%ElEcTrOnIcS%"), call.bindings().subList(0, 2));
        }
        String pageSql = database.calls().get(1).sql();
        assertTrue(pageSql.contains("order by categories.name desc"), pageSql);
        assertEquals(List.of(2, 2), database.calls().get(1).bindings().subList(2, 4).stream()
                .map(value -> ((Number) value).intValue()).toList());
        verify(jpa).findAllById(List.of(root.getId(), other.getId()));
    }

    @Test
    void emptyPageKeepsTheCountAndDoesNotLoadJpaEntitiesOrSearchForWhitespace() {
        QueryBuilderDatabase database = new QueryBuilderDatabase("categories", 7, List.of());

        var page = repository(database).findAll(GetListCategoryCommand.builder()
                .page(99).size(2).search("  ").build());

        assertTrue(page.getData().isEmpty());
        assertEquals(7, page.getTotalElements());
        assertEquals(2, database.calls().size());
        assertTrue(database.calls().stream().noneMatch(call -> call.sql().contains("ilike")));
        verifyNoInteractions(jpa);
    }

    @Test
    void invalidSortFieldIsRejectedBeforeExecutingSql() {
        QueryBuilderDatabase database = new QueryBuilderDatabase("categories", 0, List.of());

        assertThrows(IllegalArgumentException.class, () -> repository(database).findAll(
                GetListCategoryCommand.builder().page(0).size(10).sortBy("name; drop table categories").build()));

        assertTrue(database.calls().isEmpty());
        verifyNoInteractions(jpa);
    }

    private CategoryRepository repository(QueryBuilderDatabase database) {
        return new CategoryRepository(jpa, database.dsl(), QueryBuilderDatabase.metadata(), new ObjectMapper());
    }
}
