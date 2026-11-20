package com.Marketplace_Management.Shared;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.junit.jupiter.api.Test;

import com.Marketplace_Management.Catalog.Entities.ProductAttribute;
import com.Marketplace_Management.Catalog.Entities.ProductAttributeValue;
import com.Marketplace_Management.Shared.Entities.UuidEntity;

class JpaEntityEqualityTest {
    @Test
    void newAttributesAndValuesRemainDistinctInSets() {
        var color = ProductAttribute.builder().code("color").build();
        var size = ProductAttribute.builder().code("size").build();
        var red = ProductAttributeValue.builder().value("Red").build();
        var blue = ProductAttributeValue.builder().value("Blue").build();

        assertEquals(color, color);
        assertNotEquals(color, size);
        assertEquals(2, new HashSet<>(List.of(color, size)).size());
        assertEquals(2, new HashSet<>(List.of(red, blue)).size());
        assertNotEquals(color, null);
        assertNotEquals(color, "color");
    }

    @Test
    void equalityUsesIdAndEntityTypeInsteadOfMutableFields() {
        var first = ProductAttribute.builder().id(1L).code("color").build();
        var second = ProductAttribute.builder().id(1L).code("renamed").build();
        second.setUpdatedAt(LocalDateTime.now());

        assertEquals(first, second);
        assertEquals(second, first);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, ProductAttribute.builder().id(2L).build());
        assertNotEquals(first, ProductAttributeValue.builder().id(1L).build());
    }

    @Test
    void assigningGeneratedIdsPreservesSetMembership() {
        var attribute = new ProductAttribute();
        var attributes = new HashSet<>(List.of(attribute));
        attribute.setId(1L);
        assertTrue(attributes.contains(attribute));
        assertTrue(attributes.remove(attribute));

        var entity = new UuidTestEntity();
        var another = new UuidTestEntity();
        assertNotEquals(entity, another);
        var entities = new HashSet<>(List.of(entity));
        entity.setId(UUID.randomUUID());
        another.setId(entity.getId());
        assertEquals(entity, another);
        assertEquals(entity.hashCode(), another.hashCode());
        assertTrue(entities.contains(another));
    }

    @Test
    void proxyAndEntityCompareWithoutInitializingTheProxy() {
        var initializer = mock(LazyInitializer.class);
        doReturn(ProductAttribute.class).when(initializer).getPersistentClass();
        when(initializer.getInternalIdentifier()).thenReturn(1L);
        var proxy = new AttributeProxy(initializer);
        var entity = ProductAttribute.builder().id(1L).build();

        assertEquals(entity, proxy);
        assertEquals(proxy, entity);
        assertEquals(entity.hashCode(), proxy.hashCode());
        verify(initializer, never()).initialize();
        verify(initializer, never()).getImplementation();
    }

    private static class UuidTestEntity extends UuidEntity {
    }

    private static class AttributeProxy extends ProductAttribute implements HibernateProxy {
        private final LazyInitializer initializer;

        AttributeProxy(LazyInitializer initializer) {
            this.initializer = initializer;
        }

        @Override
        public LazyInitializer getHibernateLazyInitializer() {
            return initializer;
        }

        @Override
        public Object writeReplace() {
            return this;
        }
    }
}
