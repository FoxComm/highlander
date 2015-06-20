create table shipping_pricing_rules(
    id serial primary key,
    name character varying(255),
    rule_type character varying(255),
    flat_price integer,
    flat_markup integer
);