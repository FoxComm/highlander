create table shipping_price_rules(
    id serial primary key,
    name character varying(255),
    rule_type character varying(255) not null,
    flat_price integer not null default 0,
    flat_markup integer not null default 0
);