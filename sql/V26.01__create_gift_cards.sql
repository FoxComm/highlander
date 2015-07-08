create table gift_cards (
    id serial primary key,
    customer_id integer null,
    origin_id integer not null,
    origin_type character varying(255) not null,
    code character varying(255) not null,
    status character varying(255) not null,
    currency currency,
    original_balance integer not null,
    current_balance integer not null,
    reloadable boolean not null default false,
    canceled_reason character varying(255) null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (origin_id) references gift_card_origins(id) on update restrict on delete restrict,
    constraint valid_status check (status in ('new', 'auth', 'hold', 'canceled', 'partiallyApplied', 'applied')),
    constraint positive_balance check (original_balance >= 0 and current_balance >= 0)
);

create index gift_cards_idx on gift_cards (code, status);

