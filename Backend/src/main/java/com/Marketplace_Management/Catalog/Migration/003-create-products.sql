--liquibase formatted sql

--changeset marketplace:catalog-003-create-products dbms:postgresql
--comment: Initial Catalog schema for products; UUIDs and audit timestamps are supplied by Hibernate.
CREATE TABLE products (
    id uuid not null,
    name varchar(255) not null,
    description varchar(500),
    status varchar(255) not null,
    vendor_id uuid,
    brand_id uuid not null,
    created_at timestamp(6),
    updated_at timestamp(6),
    deleted_at timestamp(6) with time zone,
    primary key (id)
);

comment on column products.deleted_at is 'Soft-delete indicator';

create index idx_product_brand on products (brand_id);

create index idx_product_status on products (status);

ALTER TABLE products add constraint fk_products_brand_id foreign key (brand_id) references brands;

--rollback DROP TABLE products;
