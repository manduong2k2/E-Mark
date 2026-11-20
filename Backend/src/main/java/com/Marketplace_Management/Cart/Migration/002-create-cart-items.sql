--liquibase formatted sql

--changeset marketplace:cart-002-create-cart_items dbms:postgresql
--comment: Initial Cart schema for cart_items; UUIDs and audit timestamps are supplied by Hibernate.
CREATE TABLE cart_items (
    id uuid not null,
    quantity integer not null,
    product_variant_id uuid not null,
    cart_id uuid not null,
    created_at timestamp(6),
    updated_at timestamp(6),
    deleted_at timestamp(6) with time zone,
    primary key (id)
);

comment on column cart_items.deleted_at is 'Soft-delete indicator';

ALTER TABLE cart_items add constraint fk_cart_items_cart_id foreign key (cart_id) references carts on delete cascade;

--rollback DROP TABLE cart_items;
