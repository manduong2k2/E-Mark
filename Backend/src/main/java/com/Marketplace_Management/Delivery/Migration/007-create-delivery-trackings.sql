--liquibase formatted sql

--changeset marketplace:delivery-007-create-delivery_trackings dbms:postgresql
--comment: Initial Delivery schema for delivery_trackings; UUIDs and audit timestamps are supplied by Hibernate.
CREATE TABLE delivery_trackings (
    id uuid not null,
    notes varchar(255),
    status varchar(255),
    delivery_id uuid not null,
    timestamp timestamp(6),
    created_at timestamp(6),
    updated_at timestamp(6),
    deleted_at timestamp(6) with time zone,
    primary key (id)
);

comment on column delivery_trackings.deleted_at is 'Soft-delete indicator';

create index idx_delivery_tracking_delivery on delivery_trackings (delivery_id);

ALTER TABLE delivery_trackings add constraint fk_delivery_trackings_delivery_id foreign key (delivery_id) references deliveries;

--rollback DROP TABLE delivery_trackings;
