--liquibase formatted sql

--changeset marketplace:delivery-002-create-wards dbms:postgresql
--comment: Initial Delivery schema for wards; UUIDs and audit timestamps are supplied by Hibernate.
CREATE TABLE wards (
    id varchar(20) not null,
    name varchar(255) not null,
    code_name varchar(255),
    full_name varchar(255),
    full_name_en varchar(255),
    name_en varchar(255),
    province_id varchar(20),
    primary key (id)
);

create index idx_wards_province on wards (province_id);

ALTER TABLE wards add constraint fk_wards_province_id foreign key (province_id) references provinces;

--rollback DROP TABLE wards;
