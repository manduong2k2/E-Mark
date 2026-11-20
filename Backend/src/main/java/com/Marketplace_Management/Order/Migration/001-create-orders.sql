--liquibase formatted sql

--changeset marketplace:order-001-create-orders dbms:postgresql
--comment: Initial Order schema for orders; UUIDs and audit timestamps are supplied by Hibernate.
CREATE TABLE orders (
    id uuid not null,
    name varchar(255) not null,
    address varchar(255) not null,
    note varchar(255),
    phone varchar(255) not null,
    status varchar(20) not null,
    total float(53) not null,
    user_id uuid not null,
    created_at timestamp(6),
    updated_at timestamp(6),
    deleted_at timestamp(6) with time zone,
    primary key (id)
);

comment on column orders.deleted_at is 'Soft-delete indicator';

--rollback DROP TABLE orders;
