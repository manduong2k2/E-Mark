
package com.Marketplace_Management.Shared.Validation.Rules;

import java.lang.reflect.Field;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.springframework.stereotype.Component;

import com.Marketplace_Management.Shared.Annotation.Rules.Unique;

@Component
public class UniqueValidator implements ConstraintValidator<Unique, Object> {

    @PersistenceContext
    private EntityManager em;

    private boolean when;
    private String table;
    private String column;
    private Class<?> type;
    private String deletedAtColumn;
    private String whereClause;
    private String ignore;
    private boolean classLevel;

    @Override
    public void initialize(Unique unique) {
        this.table = unique.table();
        this.column = unique.column();
        this.type = unique.type();
        this.deletedAtColumn = unique.deletedAtColumn();
        this.whereClause = unique.whereClause();
        this.ignore = unique.ignore();
        this.when = unique.when();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (!when || value == null) {
            return true;
        }

        Object bean = value;
        Object ignoredId = null;

        // Nhận diện class-level bằng cách kiểm tra xem DTO có field
        // trùng tên với column hay không.
        Field targetField = findField(bean.getClass(), column);

        if (targetField != null) {
            classLevel = true;

            try {
                targetField.setAccessible(true);
                value = targetField.get(bean);

                if (value == null) {
                    return true;
                }

                if (ignore != null && !ignore.isBlank()) {
                    Field ignoredField = findField(bean.getClass(), ignore);

                    System.out.println("Ignored field: " + ignoredField);

                    if (ignoredField != null) {
                        ignoredField.setAccessible(true);
                        ignoredId = ignoredField.get(bean);
                    }
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(
                        "Cannot access field for unique validation", e);
            }
        }

        if (type == String.class) {
            value = value.toString();
        } else if (type == UUID.class && !(value instanceof UUID)) {
            value = UUID.fromString(value.toString());
        }

        String sql = "SELECT COUNT(*) FROM " + table
                + " WHERE " + column + " = :value";

        if (deletedAtColumn != null && !deletedAtColumn.isBlank()) {
            sql += " AND " + deletedAtColumn + " IS NULL";
        }

        if (whereClause != null && !whereClause.isBlank()) {
            sql += " AND (" + whereClause + ")";
        }

        // Loại trừ bản ghi hiện tại khi cập nhật.
        if (ignoredId != null) {
            sql += " AND id <> :ignoredId";
        }

        var query = em.createNativeQuery(sql)
                .setParameter("value", value);

        if (ignoredId != null) {
            query.setParameter("ignoredId", ignoredId);
        }

        Number count = (Number) query.getSingleResult();

        boolean valid = count.intValue() == 0;

        if (!valid && classLevel) {
            context.disableDefaultConstraintViolation();

            context.buildConstraintViolationWithTemplate(
                    context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode(column)
                    .addConstraintViolation();
        }

        return valid;
    }

    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;

        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }

        return null;
    }
}
