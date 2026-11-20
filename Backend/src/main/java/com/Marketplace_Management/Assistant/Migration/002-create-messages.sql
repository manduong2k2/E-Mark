--liquibase formatted sql

--changeset marketplace:assistant-002-create-messages dbms:postgresql
--comment: Initial Assistant schema for messages; UUIDs and audit timestamps are supplied by Hibernate.
CREATE TABLE messages (
    id uuid not null,
    content varchar(255) not null,
    metadata varchar(255) not null,
    role varchar(255) not null,
    created_at timestamp(6),
    updated_at timestamp(6),
    deleted_at timestamp(6) with time zone,
    primary key (id)
);

comment on column messages.deleted_at is 'Soft-delete indicator';

--rollback DROP TABLE messages;
