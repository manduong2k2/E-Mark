--liquibase formatted sql

--changeset marketplace:auth-006-create-email_verify_tokens dbms:postgresql
--comment: Initial Auth schema for email_verify_tokens; UUIDs and audit timestamps are supplied by Hibernate.
CREATE TABLE email_verify_tokens (
    email varchar(255) not null,
    token varchar(255) not null,
    expires_at timestamp(6) not null,
    created_at timestamp(6) not null,
    primary key (email)
);

--rollback DROP TABLE email_verify_tokens;
