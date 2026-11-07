package com.Marketplace_Management.Shared.Events;

import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;

/**
 * Generic entity lifecycle event.
 * Implements ResolvableTypeProvider so listeners can filter by the generic type,
 * e.g. onUserCreated(EntityEvent<User> event). Without it, type erasure makes
 * Spring unable to match EntityEvent<User> and the listener is never called.
 */
public class EntityEvent<T> implements ResolvableTypeProvider {
    public static enum Type {
        CREATED,
        UPDATED,
        DELETED
    }

    private T entity;
    private Type type;

    public EntityEvent(T entity, Type type) {
        this.entity = entity;
        this.type = type;
    }

    public T getEntity() {
        return entity;
    }

    public Type getType() {
        return type;
    }

    @Override
    public ResolvableType getResolvableType() {
        return ResolvableType.forClassWithGenerics(getClass(), ResolvableType.forInstance(entity));
    }
}
