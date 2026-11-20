--liquibase formatted sql

--changeset marketplace:catalog-006-create-product_variants dbms:postgresql
--comment: Initial Catalog schema for product_variants; UUIDs and audit timestamps are supplied by Hibernate.
CREATE TABLE product_variants (
    id uuid not null,
    name varchar(255) not null,
    attribute_values_key varchar(1024),
    price float(53) not null,
    sku varchar(255) not null,
    stock int default 0 not null,
    product_id uuid not null,
    created_at timestamp(6),
    updated_at timestamp(6),
    deleted_at timestamp(6) with time zone,
    primary key (id)
);

comment on column product_variants.deleted_at is 'Soft-delete indicator';

CREATE UNIQUE INDEX uk_product_variants_sku ON product_variants (sku) WHERE deleted_at IS NULL;

ALTER TABLE product_variants add constraint fk_product_variants_product_id foreign key (product_id) references products on delete cascade;

--rollback DROP TABLE product_variants;
