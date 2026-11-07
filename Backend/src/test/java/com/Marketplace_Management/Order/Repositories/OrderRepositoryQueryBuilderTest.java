package com.Marketplace_Management.Order.Repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.Marketplace_Management.Order.DTOs.Commands.ListOrderCommand;
import com.Marketplace_Management.Shared.QueryBuilderDatabase;

import tools.jackson.databind.ObjectMapper;

class OrderRepositoryQueryBuilderTest {
    @Test
    void combinesSearchWithOwnershipRangesAndStatusInCountAndPageQueries() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = from.plusMonths(1);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("orders_id", orderId);
        row.put("orders_user_id", userId);
        row.put("orders_status", "PENDING");
        row.put("orders_total", 150.0);
        row.put("orders_name", "Alice");
        row.put("orders_phone", "0901234567");
        row.put("orders_address", "12 Main Street");
        row.put("orders_note", null);
        row.put("orders_created_at", Timestamp.valueOf(from.plusDays(1)));
        row.put("orders_updated_at", Timestamp.valueOf(from.plusDays(2)));
        QueryBuilderDatabase database = new QueryBuilderDatabase("orders", 5, List.of(row));

        var page = repository(database).findByUserIdWithFilters(userId, ListOrderCommand.builder()
                .page(1).size(2).search("ALIce").status("PENDING")
                .totalMin(100.0).totalMax(200.0).dateFrom(from).dateTo(to)
                .sortBy("total").sortOrder("asc").build());

        assertEquals(5, page.getTotalElements());
        assertEquals(3, page.getTotalPages());
        var order = page.getData().getFirst();
        assertEquals(orderId, order.getId());
        assertEquals(userId, order.getUserId());
        assertEquals(150.0, order.getTotal());
        assertEquals(from.plusDays(1), order.getCreatedAt());
        assertEquals(from.plusDays(2), order.getUpdatedAt());
        assertNull(order.getNote());
        assertEquals(3, database.calls().size());
        List<Object> filters = List.of("%ALIce%", "%ALIce%", "%ALIce%", userId, "PENDING", 100.0, 200.0,
                Timestamp.valueOf(from), Timestamp.valueOf(to));
        for (var call : database.calls()) {
            assertTrue(call.sql().contains("orders.deleted_at is null"), call.sql());
            assertTrue(call.sql().contains("orders.user_id = cast(? as uuid)"), call.sql());
            assertTrue(call.sql().contains("orders.total >= cast(? as double precision)"), call.sql());
            assertTrue(call.sql().contains("orders.total <= cast(? as double precision)"), call.sql());
            assertTrue(call.sql().contains("orders.created_at >= cast(? as timestamp)"), call.sql());
            assertTrue(call.sql().contains("orders.created_at <= cast(? as timestamp)"), call.sql());
            // The OR group must close before the user predicate; search must never broaden ownership.
            assertTrue(call.sql().matches(".*orders.note as varchar\\) ilike \\?\\)+ and \\(?orders.user_id = .*"), call.sql());
            assertEquals(filters.stream().map(String::valueOf).toList(),
                    call.bindings().subList(0, filters.size()).stream().map(String::valueOf).toList());
        }
        assertTrue(database.calls().get(1).sql().contains("order by orders.total asc"));
        assertEquals(List.of(2, 2), database.calls().get(1).bindings().subList(9, 11).stream()
                .map(value -> ((Number) value).intValue()).toList());
    }

    @Test
    void adminQueryHasNoUserFilterAndDefaultsToNewestFirst() {
        QueryBuilderDatabase database = new QueryBuilderDatabase("orders", 0, List.of());

        var page = repository(database).findAll(ListOrderCommand.builder().page(0).size(10).build());

        assertTrue(page.getData().isEmpty());
        assertEquals(0, page.getTotalElements());
        assertTrue(database.calls().stream().noneMatch(call -> call.sql().contains("orders.user_id =")));
        assertTrue(database.calls().get(1).sql().contains("order by orders.created_at desc"));
    }

    @Test
    void unsupportedSortInputFallsBackToWhitelistedColumnAndDirection() {
        QueryBuilderDatabase database = new QueryBuilderDatabase("orders", 0, List.of());

        repository(database).findAll(ListOrderCommand.builder().page(0).size(10)
                .sortBy("total; drop table orders").sortOrder("invalid").build());

        assertTrue(database.calls().get(1).sql().contains("order by orders.created_at desc"));
        assertTrue(database.calls().stream().noneMatch(call -> call.sql().contains("drop table")));
    }

    private OrderRepository repository(QueryBuilderDatabase database) {
        return new OrderRepository(mock(OrderJpaRepository.class), database.dsl(), QueryBuilderDatabase.metadata(), new ObjectMapper());
    }
}
