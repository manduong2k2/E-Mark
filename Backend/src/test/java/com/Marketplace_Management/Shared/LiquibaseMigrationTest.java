package com.Marketplace_Management.Shared;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.schema.internal.ExceptionHandlerHaltImpl;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Table;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.change.core.RawSQLChange;
import liquibase.database.DatabaseFactory;
import liquibase.database.OfflineConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

class LiquibaseMigrationTest {
    private static final String CHANGELOG = "db/changelog/db.changelog-master.yaml";

    @Test
    void packagedChangelogCoversEveryEntityAndJoinTableWithRollback() throws Exception {
        try (var resources = new ClassLoaderResourceAccessor()) {
            var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(
                    new OfflineConnection("offline:postgresql?version=18&changeLogFile=target/liquibase-offline-history.csv", resources));
            try (var liquibase = new Liquibase(CHANGELOG, resources, database)) {
                liquibase.validate();
                var expectedTables = new HashSet<String>();
                for (var entity : entities()) {
                    expectedTables.add(entity.getAnnotation(Table.class).name());
                    for (var field : entity.getDeclaredFields()) {
                        var join = field.getAnnotation(JoinTable.class);
                        if (join != null) expectedTables.add(join.name());
                    }
                }
                var actualTables = new HashSet<String>();
                var createTable = Pattern.compile("(?i)CREATE TABLE (\\w+)");
                for (var changeSet : liquibase.getDatabaseChangeLog().getChangeSets()) {
                    assertFalse(changeSet.getRollback().getChanges().isEmpty(), changeSet.getId());
                    for (var change : changeSet.getChanges()) {
                        if (change instanceof RawSQLChange sql) {
                            var matcher = createTable.matcher(sql.getSql());
                            while (matcher.find()) {
                                assertTrue(actualTables.add(matcher.group(1)), "Duplicate table: " + matcher.group(1));
                            }
                        }
                    }
                }
                assertEquals(expectedTables, actualTables);
            }
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "migration.test.url", matches = "jdbc:postgresql:.*")
    void postgresMigrationsValidateAgainstHibernateAndCanRollbackAndReapply() throws Exception {
        String url = System.getProperty("migration.test.url");
        String user = System.getProperty("migration.test.user", "migration_test");
        String password = System.getProperty("migration.test.password", "");
        // All writes are confined to a uniquely named schema, never the application's schema.
        String schema = "migration_test_" + UUID.randomUUID().toString().replace("-", "");
        try {
            try (var connection = DriverManager.getConnection(url, user, password);
                 var resources = new ClassLoaderResourceAccessor()) {
                execute(connection, "CREATE SCHEMA " + schema);
                connection.setSchema(schema);
                var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
                database.setDefaultSchemaName(schema);
                try (var liquibase = new Liquibase(CHANGELOG, resources, database)) {
                    liquibase.validate();
                    int changeCount = liquibase.getDatabaseChangeLog().getChangeSets().size();
                    liquibase.update(new Contexts(), new LabelExpression());
                    validateHibernate(url, user, password, schema);
                    assertGeographicData(connection);
                    assertConstraints(connection);
                    assertSoftDeleteUniqueness(connection);

                    liquibase.update(new Contexts(), new LabelExpression());
                    assertGeographicData(connection);
                    try (var statement = connection.createStatement();
                         var rows = statement.executeQuery("SELECT count(*) FROM databasechangelog")) {
                        assertTrue(rows.next());
                        assertEquals(changeCount, rows.getInt(1));
                    }

                    liquibase.rollback(changeCount, new Contexts(), new LabelExpression());
                    try (var statement = connection.prepareStatement(
                            "SELECT count(*) FROM information_schema.tables WHERE table_schema = ? AND table_name NOT IN ('databasechangelog', 'databasechangeloglock')")) {
                        statement.setString(1, schema);
                        try (var rows = statement.executeQuery()) {
                            assertTrue(rows.next());
                            assertEquals(0, rows.getInt(1));
                        }
                    }
                    liquibase.update(new Contexts(), new LabelExpression());
                    validateHibernate(url, user, password, schema);
                    assertGeographicData(connection);
                }
            }
        } finally {
            try (var cleanup = DriverManager.getConnection(url, user, password)) {
                execute(cleanup, "DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            }
        }
    }

    private static List<Class<?>> entities() throws ClassNotFoundException {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        var classes = new java.util.ArrayList<Class<?>>();
        for (var definition : scanner.findCandidateComponents("com.Marketplace_Management")) {
            classes.add(Class.forName(definition.getBeanClassName()));
        }
        return classes;
    }

    private static void validateHibernate(String url, String user, String password, String schema) throws Exception {
        Map<String, Object> settings = new HashMap<>();
        settings.put("hibernate.connection.url", url);
        settings.put("hibernate.connection.username", user);
        settings.put("hibernate.connection.password", password);
        settings.put("hibernate.default_schema", schema);
        settings.put("hibernate.physical_naming_strategy", PhysicalNamingStrategySnakeCaseImpl.class.getName());
        var registry = new StandardServiceRegistryBuilder().applySettings(settings).build();
        try {
            var sources = new MetadataSources(registry);
            for (var entity : entities()) sources.addAnnotatedClass(entity);
            var metadata = sources.buildMetadata();
            var options = SchemaManagementToolCoordinator.buildExecutionOptions(settings, ExceptionHandlerHaltImpl.INSTANCE);
            registry.getService(SchemaManagementTool.class).getSchemaValidator(settings)
                    .doValidation(metadata, options, ContributableMatcher.ALL);
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    private static void assertGeographicData(Connection connection) throws SQLException {
        try (var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT (SELECT count(*) FROM provinces), (SELECT count(*) FROM wards), (SELECT name FROM provinces WHERE id = '01')")) {
            assertTrue(rows.next());
            assertEquals(34, rows.getInt(1));
            assertEquals(3321, rows.getInt(2));
            assertEquals("Hà Nội", rows.getString(3));
        }
    }

    private static void assertConstraints(Connection connection) throws SQLException {
        connection.setAutoCommit(true);
        String brand = UUID.randomUUID().toString();
        String product = UUID.randomUUID().toString();
        execute(connection, "INSERT INTO brands(id, name) VALUES ('" + brand + "', 'Brand')");
        execute(connection, "INSERT INTO products(id, name, status, brand_id) VALUES ('" + product + "', 'Product', 'PUBLISHED', '" + brand + "')");
        execute(connection, "INSERT INTO product_attributes(code, name, product_id) VALUES ('color', 'Color', '" + product + "')");
        var duplicate = assertThrows(SQLException.class, () -> execute(connection,
                "INSERT INTO product_attributes(code, name, product_id) VALUES ('color', 'Color', '" + product + "')"));
        assertEquals("23505", duplicate.getSQLState());
        var foreignKey = assertThrows(SQLException.class, () -> execute(connection,
                "INSERT INTO product_attribute_values(value, product_attribute_id) VALUES ('Red', -1)"));
        assertEquals("23503", foreignKey.getSQLState());
        // File.entity is polymorphic and must not be constrained to product_variants.
        execute(connection, "INSERT INTO files(id, url, entity_type, entity_id) VALUES ('" + UUID.randomUUID()
                + "', '/image.png', 'example.OtherEntity', '" + UUID.randomUUID() + "')");
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static void assertSoftDeleteUniqueness(Connection connection) throws SQLException {
        assertReusableAfterSoftDelete(connection, "roles", "code = 'soft-delete-role'", "uk_roles_code",
                () -> "INSERT INTO roles(id, name, code) VALUES ('" + UUID.randomUUID() + "', 'Role', 'soft-delete-role')");
        assertReusableAfterSoftDelete(connection, "users", "email = 'soft-delete@example.com'", "uk_users_email",
                () -> "INSERT INTO users(id, email, password) VALUES ('" + UUID.randomUUID() + "', 'soft-delete@example.com', 'password')");
        assertReusableAfterSoftDelete(connection, "users", "phone = '0901234567'", "uk_users_phone",
                () -> "INSERT INTO users(id, email, password, phone) VALUES ('" + UUID.randomUUID() + "', '"
                        + UUID.randomUUID() + "@example.com', 'password', '0901234567')");

        String userId = UUID.randomUUID().toString();
        assertReusableAfterSoftDelete(connection, "carts", "user_id = '" + userId + "'", "uk_carts_user_id",
                () -> "INSERT INTO carts(id, status, user_id) VALUES ('" + UUID.randomUUID() + "', 'NEW', '" + userId + "')");

        Map<String, String> vendorKeys = Map.of("email", "soft-delete-vendor@example.com", "phone", "0901234567",
                "tax_code", "SOFTDELETE", "address_id", UUID.randomUUID().toString());
        for (var entry : vendorKeys.entrySet()) {
            String column = entry.getKey();
            String value = entry.getValue();
            assertReusableAfterSoftDelete(connection, "vendors", column + " = '" + value + "'", "uk_vendors_" + column,
                    () -> "INSERT INTO vendors(id, name, status, user_id, " + column + ") VALUES ('"
                            + UUID.randomUUID() + "', 'Vendor', 'PENDING', '" + userId + "', '" + value + "')");
        }

        String brandId = UUID.randomUUID().toString();
        String productId = UUID.randomUUID().toString();
        execute(connection, "INSERT INTO brands(id, name) VALUES ('" + brandId + "', 'Soft-delete brand')");
        execute(connection, "INSERT INTO products(id, name, status, brand_id) VALUES ('" + productId
                + "', 'Soft-delete product', 'PUBLISHED', '" + brandId + "')");
        assertReusableAfterSoftDelete(connection, "product_attributes", "product_id = '" + productId + "' AND code = 'color'",
                "uk_product_attributes_product_code",
                () -> "INSERT INTO product_attributes(name, code, product_id) VALUES ('Color', 'color', '" + productId + "')");
        long attributeId;
        try (var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT id FROM product_attributes WHERE product_id = '" + productId + "' AND deleted_at IS NULL")) {
            assertTrue(rows.next());
            attributeId = rows.getLong(1);
        }
        assertReusableAfterSoftDelete(connection, "product_attribute_values", "product_attribute_id = " + attributeId + " AND value = 'Red'",
                "uk_product_attribute_values_attribute_value",
                () -> "INSERT INTO product_attribute_values(product_attribute_id, value) VALUES (" + attributeId + ", 'Red')");
    }

    private static void assertReusableAfterSoftDelete(Connection connection, String table, String key,
            String index, Supplier<String> insert) throws SQLException {
        execute(connection, insert.get());
        var duplicate = assertThrows(SQLException.class, () -> execute(connection, insert.get()));
        assertEquals("23505", duplicate.getSQLState());
        assertTrue(duplicate.getMessage().contains(index), duplicate.getMessage());

        execute(connection, "UPDATE " + table + " SET deleted_at = CURRENT_TIMESTAMP WHERE " + key + " AND deleted_at IS NULL");
        execute(connection, insert.get());

        var restore = assertThrows(SQLException.class, () -> execute(connection,
                "UPDATE " + table + " SET deleted_at = NULL WHERE " + key + " AND deleted_at IS NOT NULL"));
        assertEquals("23505", restore.getSQLState());
        assertTrue(restore.getMessage().contains(index), restore.getMessage());
    }
}
