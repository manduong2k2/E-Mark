--liquibase formatted sql

--changeset marketplace:order-002-create-order_items dbms:postgresql
--comment: Initial Order schema for order_items; UUIDs and audit timestamps are supplied by Hibernate.
CREATE TABLE order_items (
    id uuid not null,
    product_description varchar(255),
    product_images varchar(255) array,
    product_name varchar(255),
    product_price float(53) not null,
    product_sku varchar(255),
    quantity integer not null,
    total float(53) not null,
    product_id uuid not null,
    order_id uuid not null,
    created_at timestamp(6),
    updated_at timestamp(6),
    deleted_at timestamp(6) with time zone,
    primary key (id)
);

comment on column order_items.deleted_at is 'Soft-delete indicator';

ALTER TABLE order_items add constraint fk_order_items_order_id foreign key (order_id) references orders on delete cascade;

--rollback DROP TABLE order_items;
