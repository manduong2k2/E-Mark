--liquibase formatted sql

--changeset marketplace:assistant-003-create-summaries dbms:postgresql
--comment: Initial Assistant schema for summaries; UUIDs and audit timestamps are supplied by Hibernate.
CREATE TABLE summaries (
    id uuid not null,
    content varchar(255) not null,
    title varchar(255) not null,
    created_at timestamp(6),
    updated_at timestamp(6),
    deleted_at timestamp(6) with time zone,
    primary key (id)
);

comment on column summaries.deleted_at is 'Soft-delete indicator';

--rollback DROP TABLE summaries;
