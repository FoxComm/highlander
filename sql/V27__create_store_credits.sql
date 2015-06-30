create table store_credits (
    id serial primary key,
    customer_id integer not null,
    status character varying(255) not null,
    currency currency,
    constraint valid_status check (status in ('new', 'auth', 'hold','active','canceled', 'partiallyApplied', 'applied'))
);

