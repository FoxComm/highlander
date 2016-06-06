create table return_payments (
    id serial primary key,
    return_id integer not null references returns(id) on update restrict on delete restrict,
    payment_method_id integer not null references payment_methods(id) on update restrict on delete restrict,
    payment_method_type generic_string not null,
    amount integer not null,
    currency currency,
    created_at generic_timestamp,
    updated_at generic_timestamp,
    constraint valid_payment_type check (payment_method_type in ('creditCard', 'giftCard', 'storeCredit'))
);

create index return_payments_return_id_idx on return_payments (return_id);