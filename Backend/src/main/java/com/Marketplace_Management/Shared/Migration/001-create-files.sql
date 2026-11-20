--liquibase formatted sql

--changeset marketplace:shared-001-create-files dbms:postgresql
--comment: Initial Shared schema for files; UUIDs and audit timestamps are supplied by Hibernate.
CREATE TABLE files (
    id uuid not null,
    entity_type VARCHAR(255),
    url varchar(255) not null,
    entity_id uuid,
    created_at timestamp(6),
    updated_at timestamp(6),
    deleted_at timestamp(6) with time zone,
    primary key (id)
);

comment on column files.deleted_at is 'Soft-delete indicator';

--rollback DROP TABLE files;
