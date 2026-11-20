--liquibase formatted sql

--changeset marketplace:catalog-002-create-categories dbms:postgresql
--comment: Initial Catalog schema for categories; UUIDs and audit timestamps are supplied by Hibernate.
CREATE TABLE categories (
    id uuid not null,
    name varchar(255) not null,
    description varchar(500),
    image varchar(255),
    parent_id uuid,
    created_at timestamp(6),
    updated_at timestamp(6),
    deleted_at timestamp(6) with time zone,
    primary key (id)
);

comment on column categories.deleted_at is 'Soft-delete indicator';

ALTER TABLE categories add constraint fk_categories_parent_id foreign key (parent_id) references categories;

--rollback DROP TABLE categories;
