--liquibase formatted sql

--changeset marketplace:auth-001-create-roles dbms:postgresql
--comment: Initial Auth schema for roles; UUIDs and audit timestamps are supplied by Hibernate.
CREATE TABLE roles (
    id uuid not null,
    name varchar(255) not null,
    code varchar(255) not null,
    created_at timestamp(6),
    updated_at timestamp(6),
    deleted_at timestamp(6) with time zone,
    primary key (id)
);

comment on column roles.deleted_at is 'Soft-delete indicator';

CREATE UNIQUE INDEX uk_roles_code ON roles (code) WHERE deleted_at IS NULL;

--rollback DROP TABLE roles;
