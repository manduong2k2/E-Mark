package com.Marketplace_Management.Shared.Entities;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.proxy.HibernateProxy;

import com.Marketplace_Management.Shared.Events.EntityEventListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SoftDelete(columnName = "deleted_at", strategy = SoftDeleteType.TIMESTAMP)
@Data
@MappedSuperclass
@SuperBuilder
@NoArgsConstructor
@EntityListeners(EntityEventListener.class) // publishes EntityEvent<T> on create/update/delete for every entity
public abstract class JpaEntity {
    public abstract Object getId();

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof JpaEntity entity) || entityClass(this) != entityClass(entity)) {
            return false;
        }
        Object id = identifier(this);
        return id != null && id.equals(identifier(entity));
    }

    @Override
    public int hashCode() {
        // Generated IDs must not change the hash while the entity is in a Set.
        return entityClass(this).hashCode();
    }

    private static Class<?> entityClass(JpaEntity entity) {
        return entity instanceof HibernateProxy proxy
                ? proxy.getHibernateLazyInitializer().getPersistentClass()
                : entity.getClass();
    }

    private static Object identifier(JpaEntity entity) {
        return entity instanceof HibernateProxy proxy
                ? proxy.getHibernateLazyInitializer().getInternalIdentifier()
                : entity.getId();
    }

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    protected LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    protected LocalDateTime updatedAt;
}
