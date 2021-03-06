create table order_payments (
    id serial primary key,
    cord_ref text not null,
    payment_method_id integer not null,
    payment_method_type generic_string not null,
    amount integer null,
    currency currency,
    created_at generic_timestamp,
    updated_at generic_timestamp,
    foreign key (cord_ref) references cords(reference_number) on update restrict on delete restrict,
    foreign key (payment_method_id) references payment_methods(id) on update restrict on delete restrict,
    constraint valid_payment_type check (payment_method_type in ('creditCard', 'giftCard', 'storeCredit'))
);

create index order_payments_order_id_idx on order_payments (cord_ref);

