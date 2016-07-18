create table gift_card_subtypes (
    id serial primary key,
    title generic_string not null,
    origin_type gift_card_origin_type
);