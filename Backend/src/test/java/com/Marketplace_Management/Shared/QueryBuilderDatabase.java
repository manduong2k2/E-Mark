package com.Marketplace_Management.Shared;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockResult;

import com.Marketplace_Management.Shared.Utils.QueryBuilder.EntityMetadataRegistry;
import com.Marketplace_Management.Shared.Utils.QueryBuilder.EntityMetadataScanner;

/** Runs the real QueryBuilder against JDBC fixtures and captures its SQL and bind values. */
public class QueryBuilderDatabase {
    public record Call(String sql, List<Object> bindings) {}

    private final List<Call> calls = new ArrayList<>();
    private final DSLContext records = DSL.using(SQLDialect.POSTGRES);
    private final DSLContext dsl;

    public QueryBuilderDatabase(String table, long total, List<Map<String, Object>> rows) {
        dsl = DSL.using(new MockConnection(context -> {
            String sql = context.sql().replace("\"", "").replaceAll("\\s+", " ").trim();
            calls.add(new Call(sql, Arrays.asList(context.bindings().clone())));
            Result<Record> result;
            if (sql.startsWith("select count(distinct")) {
                result = result(List.of(Map.of("total", total)), List.of("total"));
            } else if (sql.startsWith("select distinct")) {
                List<String> columns = Arrays.stream(sql.substring("select distinct ".length(), sql.indexOf(" from "))
                        .split(", ")).map(field -> field.substring(field.lastIndexOf('.') + 1)).toList();
                List<Map<String, Object>> pageRows = rows.stream().map(row -> {
                    Map<String, Object> selected = new LinkedHashMap<>();
                    columns.forEach(column -> selected.put(column, row.get(table + "_" + column)));
                    return selected;
                }).toList();
                result = result(pageRows, columns);
            } else {
                result = result(rows, new ArrayList<>(rows.getFirst().keySet()));
            }
            return new MockResult[] { new MockResult(result.size(), result) };
        }), SQLDialect.POSTGRES);
    }

    public DSLContext dsl() {
        return dsl;
    }

    public List<Call> calls() {
        return calls;
    }

    public static EntityMetadataRegistry metadata() {
        EntityMetadataRegistry registry = new EntityMetadataRegistry();
        new EntityMetadataScanner(registry).scan();
        return registry;
    }

    private Result<Record> result(List<Map<String, Object>> rows, List<String> columns) {
        Field<?>[] fields = columns.stream().map(column -> DSL.field(DSL.name(column), Object.class)).toArray(Field<?>[]::new);
        Result<Record> result = records.newResult(fields);
        for (Map<String, Object> row : rows) {
            Record record = records.newRecord(fields);
            record.fromArray(columns.stream().map(row::get).toArray());
            result.add(record);
        }
        return result;
    }
}
