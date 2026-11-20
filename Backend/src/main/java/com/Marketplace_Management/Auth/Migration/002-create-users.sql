--liquibase formatted sql

--changeset marketplace:auth-002-create-users dbms:postgresql
--comment: Initial Auth schema for users; UUIDs and audit timestamps are supplied by Hibernate.
CREATE TABLE users (
    id uuid not null,
    name varchar(255),
    avatar varchar(50),
    email varchar(255) not null,
    password varchar(200) not null,
    phone varchar(255),
    status varchar(20) default 'INACTIVE',
    created_at timestamp(6),
    updated_at timestamp(6),
    deleted_at timestamp(6) with time zone,
    primary key (id)
);

comment on column users.deleted_at is 'Soft-delete indicator';

CREATE UNIQUE INDEX uk_users_email ON users (email) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uk_users_phone ON users (phone) WHERE deleted_at IS NULL;

--rollback DROP TABLE users;
