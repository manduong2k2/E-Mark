--liquibase formatted sql

--changeset marketplace:delivery-006-create-delivery_items dbms:postgresql
--comment: Initial Delivery schema for delivery_items; UUIDs and audit timestamps are supplied by Hibernate.
CREATE TABLE delivery_items (
    id uuid not null,
    delivered integer not null,
    product_name varchar(255),
    product_sku varchar(255),
    quantity integer not null,
    order_item_id uuid,
    product_variant_id uuid,
    delivery_id uuid not null,
    created_at timestamp(6),
    updated_at timestamp(6),
    deleted_at timestamp(6) with time zone,
    primary key (id)
);

comment on column delivery_items.deleted_at is 'Soft-delete indicator';

create index idx_delivery_item_delivery on delivery_items (delivery_id);

create index idx_delivery_item_order_item on delivery_items (order_item_id);

ALTER TABLE delivery_items add constraint fk_delivery_items_delivery_id foreign key (delivery_id) references deliveries on delete cascade;

--rollback DROP TABLE delivery_items;
