package com.Marketplace_Management.Catalog.Models;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.Marketplace_Management.Shared.Models.Entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class ProductVariant extends Entity<UUID> {
    private UUID productId;
    private String name;
    private String sku;
    private Money price;
    private int stock;
    private List<File> images;
    private Product product;

    /** An unset price reads as 0. */
    public Money getPrice() {
        return price != null ? price : new Money(0);
    }

    /** The builder takes the price as a plain amount. */
    public abstract static class ProductVariantBuilder<C extends ProductVariant, B extends ProductVariantBuilder<C, B>>
            extends Entity.EntityBuilder<UUID, C, B> {
        public B price(double price) {
            this.price = new Money(price);
            return self();
        }
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
