--liquibase formatted sql

--changeset marketplace:cart-001-create-carts dbms:postgresql
--comment: Initial Cart schema for carts; UUIDs and audit timestamps are supplied by Hibernate.
CREATE TABLE carts (
    id uuid not null,
    status varchar(255) not null,
    user_id uuid not null,
    created_at timestamp(6),
    updated_at timestamp(6),
    deleted_at timestamp(6) with time zone,
    primary key (id)
);

comment on column carts.deleted_at is 'Soft-delete indicator';

CREATE UNIQUE INDEX uk_carts_user_id ON carts (user_id) WHERE deleted_at IS NULL;

--rollback DROP TABLE carts;
