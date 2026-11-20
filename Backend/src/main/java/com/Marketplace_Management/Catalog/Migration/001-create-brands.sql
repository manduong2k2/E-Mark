--liquibase formatted sql

--changeset marketplace:catalog-001-create-brands dbms:postgresql
--comment: Initial Catalog schema for brands; UUIDs and audit timestamps are supplied by Hibernate.
CREATE TABLE brands (
    id uuid not null,
    name varchar(255) not null,
    description varchar(500),
    image varchar(255),
    created_at timestamp(6),
    updated_at timestamp(6),
    deleted_at timestamp(6) with time zone,
    primary key (id)
);

comment on column brands.deleted_at is 'Soft-delete indicator';

--rollback DROP TABLE brands;
