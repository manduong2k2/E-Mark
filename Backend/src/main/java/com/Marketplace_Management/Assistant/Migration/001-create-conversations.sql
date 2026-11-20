--liquibase formatted sql

--changeset marketplace:assistant-001-create-conversations dbms:postgresql
--comment: Initial Assistant schema for conversations; UUIDs and audit timestamps are supplied by Hibernate.
CREATE TABLE conversations (
    id uuid not null,
    status varchar(255) not null,
    title varchar(255) not null,
    user_id uuid not null,
    created_at timestamp(6),
    updated_at timestamp(6),
    deleted_at timestamp(6) with time zone,
    primary key (id)
);

comment on column conversations.deleted_at is 'Soft-delete indicator';

--rollback DROP TABLE conversations;
