--liquibase formatted sql

--changeset marketplace:catalog-008-create-product_variant_attribute_values dbms:postgresql
--comment: Initial Catalog schema for product_variant_attribute_values; UUIDs and audit timestamps are supplied by Hibernate.
CREATE TABLE product_variant_attribute_values (
    variant_id uuid not null,
    attribute_value_id bigint not null,
    constraint uk_product_variant_attribute_values unique (variant_id, attribute_value_id)
);

ALTER TABLE product_variant_attribute_values add constraint fk_product_variant_attribute_values_attribute_value_id foreign key (attribute_value_id) references product_attribute_values;

ALTER TABLE product_variant_attribute_values add constraint fk_product_variant_attribute_values_variant_id foreign key (variant_id) references product_variants;

--rollback DROP TABLE product_variant_attribute_values;
