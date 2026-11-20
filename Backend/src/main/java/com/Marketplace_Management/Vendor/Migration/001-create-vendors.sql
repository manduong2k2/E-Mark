--liquibase formatted sql

--changeset marketplace:vendor-001-create-vendors dbms:postgresql
--comment: Initial Vendor schema for vendors; UUIDs and audit timestamps are supplied by Hibernate.
CREATE TABLE vendors (
    id uuid not null,
    name varchar(255) not null,
    description varchar(500),
    banner varchar(500),
    email varchar(100),
    logo varchar(500),
    phone varchar(15),
    status varchar(255) not null check ((status in ('PENDING','ACTIVE','BANNED'))),
    tax_code varchar(100),
    address_id uuid,
    user_id uuid not null,
    created_at timestamp(6),
    updated_at timestamp(6),
    deleted_at timestamp(6) with time zone,
    primary key (id)
);

comment on column vendors.deleted_at is 'Soft-delete indicator';

CREATE UNIQUE INDEX uk_vendors_email ON vendors (email) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uk_vendors_phone ON vendors (phone) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uk_vendors_tax_code ON vendors (tax_code) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uk_vendors_address_id ON vendors (address_id) WHERE deleted_at IS NULL;

--rollback DROP TABLE vendors;
