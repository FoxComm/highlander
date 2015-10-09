create table gift_card_subtypes (
    id serial primary key,
    title character varying(255) not null,
    origin_type gc_origin_type
);