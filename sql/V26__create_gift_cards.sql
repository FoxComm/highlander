create table gift_cards (
    id serial primary key,
    status character varying(255) not null,
    currency currency,
    original_balance integer not null,
    current_balance integer not null,
    constraint valid_status check (status in ('hold','active','canceled'))
);

