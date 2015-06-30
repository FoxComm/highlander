create table store_credits (
    id serial primary key,
    customer_id integer not null,
    status character varying(255) not null,
    currency currency,
    canceled_reason character varying(255) null,
    constraint valid_status check (status in ('new', 'auth', 'hold','active','canceled', 'partiallyApplied', 'applied'))
);

