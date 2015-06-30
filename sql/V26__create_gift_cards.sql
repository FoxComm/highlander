create table gift_cards (
    id serial primary key,
    status character varying(255) not null,
    currency currency,
    constraint valid_status check (status in ('hold','active','canceled'))
);

