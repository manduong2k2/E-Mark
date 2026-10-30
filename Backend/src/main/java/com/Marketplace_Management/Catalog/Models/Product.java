package com.Marketplace_Management.Catalog.Models;

import java.util.Set;
import java.util.UUID;

import com.Marketplace_Management.Shared.Models.AggregateRoot;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
public class Product extends AggregateRoot<UUID> {
    private String name;
    private String description;
    private UUID brandId;
    private Brand brand;
    private List<UUID> categoryIds;
    private Set<Category> categories;
    private String status;
    private Set<ProductVariant> variants;
    private Set<ProductOption> options;
    private UUID vendorId;

    public boolean isArchived() {
        return status.equals("ARCHIVED");
    }

    public void update(Map<String, Object> updates) {
        updates.forEach((key, value) -> {
            String setterName = "set" +
                    Character.toUpperCase(key.charAt(0)) +
                    key.substring(1);

            try {
                Method setter = Arrays.stream(getClass().getMethods())
                        .filter(method -> method.getName().equals(setterName))
                        .filter(method -> method.getParameterCount() == 1)
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Unsupported product field: " + key));

                setter.invoke(this, value);

            } catch (InvocationTargetException e) {
                throw new RuntimeException(e.getCause());
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
