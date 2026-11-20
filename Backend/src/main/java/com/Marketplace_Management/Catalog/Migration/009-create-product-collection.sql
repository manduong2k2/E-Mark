--liquibase formatted sql

--changeset marketplace:catalog-009-create-product_collection dbms:postgresql
--comment: Initial Catalog schema for product_collection; UUIDs and audit timestamps are supplied by Hibernate.
CREATE TABLE product_collection (
    display_order integer not null,
    collection_id uuid not null,
    product_id uuid not null,
    primary key (collection_id, product_id)
);

ALTER TABLE product_collection add constraint fk_product_collection_product_id foreign key (product_id) references products;

--rollback DROP TABLE product_collection;
