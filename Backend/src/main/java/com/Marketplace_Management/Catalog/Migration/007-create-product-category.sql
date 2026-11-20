--liquibase formatted sql

--changeset marketplace:catalog-007-create-product_category dbms:postgresql
--comment: Initial Catalog schema for product_category; UUIDs and audit timestamps are supplied by Hibernate.
CREATE TABLE product_category (
    product_id uuid not null,
    category_id uuid not null,
    unique (product_id, category_id)
);

ALTER TABLE product_category add constraint fk_product_category_category_id foreign key (category_id) references categories;

ALTER TABLE product_category add constraint fk_product_category_product_id foreign key (product_id) references products on delete cascade;

--rollback DROP TABLE product_category;
