--liquibase formatted sql

--changeset marketplace:auth-005-create-oauth_infos dbms:postgresql
--comment: Initial Auth schema for oauth_infos; UUIDs and audit timestamps are supplied by Hibernate.
CREATE TABLE oauth_infos (
    id uuid not null,
    oauth_provider varchar(20) not null check ((oauth_provider in ('GOOGLE','FACEBOOK'))),
    oauth_provider_subject varchar(255) not null,
    user_id uuid not null,
    created_at timestamp(6) with time zone not null,
    primary key (id),
    constraint uk_oauth_infos_provider_subject unique (oauth_provider, oauth_provider_subject)
);

create index idx_oauth_infos_user_id on oauth_infos (user_id);

ALTER TABLE oauth_infos add constraint fk_oauth_infos_user foreign key (user_id) references users on delete cascade;

--rollback DROP TABLE oauth_infos;
