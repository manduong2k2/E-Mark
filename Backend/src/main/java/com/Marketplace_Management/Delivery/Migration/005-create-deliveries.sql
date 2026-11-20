--liquibase formatted sql

--changeset marketplace:delivery-005-create-deliveries dbms:postgresql
--comment: Initial Delivery schema for deliveries; UUIDs and audit timestamps are supplied by Hibernate.
CREATE TABLE deliveries (
    id uuid not null,
    fee DOUBLE PRECISION DEFAULT 0 not null,
    notes varchar(255),
    recipient_name varchar(255),
    recipient_phone varchar(255),
    status varchar(255) not null,
    order_id uuid not null,
    user_id uuid not null,
    delivery_method_id bigint not null,
    shipping_address_id bigint not null,
    actual_delivery_date timestamp(6),
    estimated_delivery_date timestamp(6),
    created_at timestamp(6),
    updated_at timestamp(6),
    deleted_at timestamp(6) with time zone,
    primary key (id)
);

comment on column deliveries.deleted_at is 'Soft-delete indicator';

create index idx_delivery_order on deliveries (order_id);

create index idx_delivery_user on deliveries (user_id);

create index idx_delivery_shipping_address on deliveries (shipping_address_id);

ALTER TABLE deliveries add constraint fk_deliveries_delivery_method_id foreign key (delivery_method_id) references delivery_methods;

ALTER TABLE deliveries add constraint fk_deliveries_shipping_address_id foreign key (shipping_address_id) references addresses;

--rollback DROP TABLE deliveries;
