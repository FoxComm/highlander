create table gift_cards (
    id serial primary key,
    status character varying(255) not null,
    currency currency,
    original_balance integer not null,
    current_balance integer not null,
    canceled_reason character varying(255) null,
    constraint valid_status check (status in ('new', 'auth', 'hold','active','canceled', 'partiallyApplied', 'applied')),
    constraint positive_balance check (original_balance >= 0 and original_balance >= 0)
);

