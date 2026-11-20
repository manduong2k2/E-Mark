--liquibase formatted sql

--changeset marketplace:vendor-002-create-collections dbms:postgresql
--comment: Initial Vendor schema for collections; UUIDs and audit timestamps are supplied by Hibernate.
CREATE TABLE collections (
    id uuid not null,
    name varchar(255) not null,
    display_order integer,
    vendor_id uuid not null,
    created_at timestamp(6),
    updated_at timestamp(6),
    deleted_at timestamp(6) with time zone,
    primary key (id)
);

comment on column collections.deleted_at is 'Soft-delete indicator';

ALTER TABLE collections add constraint fk_collections_vendor_id foreign key (vendor_id) references vendors;

--rollback DROP TABLE collections;
