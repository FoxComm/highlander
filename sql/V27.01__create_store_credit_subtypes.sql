create table store_credit_subtypes (
    id serial primary key,
    title character varying(255) not null,
    origin_type sc_origin_type
);