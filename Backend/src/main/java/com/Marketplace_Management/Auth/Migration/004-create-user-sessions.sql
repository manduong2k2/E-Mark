--liquibase formatted sql

--changeset marketplace:auth-004-create-user_sessions dbms:postgresql
--comment: Initial Auth schema for user_sessions; UUIDs and audit timestamps are supplied by Hibernate.
CREATE TABLE user_sessions (
    id uuid not null,
    access_jti varchar(36) not null,
    ip_address varchar(45),
    refresh_jti varchar(36) not null,
    user_id uuid not null,
    access_expires_at timestamp(6) with time zone not null,
    login_at timestamp(6) with time zone not null,
    refresh_expires_at timestamp(6) with time zone not null,
    primary key (id),
    constraint uk_user_sessions_refresh_jti unique (refresh_jti)
);

create index idx_user_sessions_user_access on user_sessions (user_id, access_jti);

create index idx_user_sessions_access_expires_at on user_sessions (access_expires_at);

ALTER TABLE user_sessions add constraint fk_user_sessions_user foreign key (user_id) references users on delete cascade;

--rollback DROP TABLE user_sessions;
