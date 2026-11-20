--liquibase formatted sql

--changeset marketplace:auth-003-create-user_roles dbms:postgresql
--comment: Initial Auth schema for user_roles; UUIDs and audit timestamps are supplied by Hibernate.
CREATE TABLE user_roles (
    user_id uuid not null,
    role_id uuid not null,
    primary key (user_id, role_id)
);

ALTER TABLE user_roles add constraint fk_user_roles_role_id foreign key (role_id) references roles;

ALTER TABLE user_roles add constraint fk_user_roles_user_id foreign key (user_id) references users on delete cascade;

--rollback DROP TABLE user_roles;
