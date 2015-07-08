create table store_credits (
    id serial primary key,
    customer_id integer not null,
    origin_id integer not null,
    origin_type character varying(255) not null,
    status character varying(255) not null,
    currency currency,
    original_balance integer not null,
    current_balance integer not null,
    canceled_reason character varying(255) null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (origin_id) references store_credit_origins(id) on update restrict on delete restrict,
    constraint valid_status check (status in ('new', 'auth', 'hold', 'canceled', 'partiallyApplied', 'applied'))
);

create index store_credits_idx on store_credits (customer_id, status);

