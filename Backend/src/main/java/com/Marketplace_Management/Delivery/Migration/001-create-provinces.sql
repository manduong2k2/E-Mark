--liquibase formatted sql

--changeset marketplace:delivery-001-create-provinces dbms:postgresql
--comment: Initial Delivery schema for provinces; UUIDs and audit timestamps are supplied by Hibernate.
CREATE TABLE provinces (
    id varchar(20) not null,
    name varchar(255) not null,
    code_name varchar(255),
    full_name varchar(255) not null,
    full_name_en varchar(255),
    name_en varchar(255),
    primary key (id)
);

--rollback DROP TABLE provinces;
