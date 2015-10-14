create table shipping_price_rules(
    id serial primary key,
    name generic_string,
    rule_type generic_string not null,
    flat_price integer not null default 0,
    flat_markup integer not null default 0
);