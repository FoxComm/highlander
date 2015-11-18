create table rma_payments (
    id serial primary key,
    rma_id integer not null references rmas(id) on update restrict on delete restrict,
    payment_method_id integer not null references payment_methods(id) on update restrict on delete restrict,
    payment_method_type generic_string not null,
    amount integer not null,
    currency currency,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    constraint valid_payment_type check (payment_method_type in ('creditCard', 'giftCard', 'storeCredit'))
);

create index rma_payments_rma_id_idx on rma_payments (rma_id);