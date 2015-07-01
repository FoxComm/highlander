create table store_credits (
    id serial primary key,
    customer_id integer not null,
    status character varying(255) not null,
    currency currency,
    canceled_reason character varying(255) null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    constraint valid_status check (status in ('new', 'auth', 'hold','active','canceled', 'partiallyApplied', 'applied'))
);

