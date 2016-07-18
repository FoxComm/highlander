create table store_credit_subtypes (
    id serial primary key,
    title generic_string not null,
    origin_type store_credit_origin_type
);