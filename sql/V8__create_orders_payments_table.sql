create table order_payments (
    id serial primary key,
    order_id integer not null,
    payment_method_id integer not null,
    payment_method_type character varying(255) not null,
    amount integer null,
    currency currency,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (order_id) references orders(id) on update restrict on delete restrict,
    foreign key (payment_method_id) references payment_methods(id) on update restrict on delete restrict,
    constraint valid_payment_type check (payment_method_type in ('creditCard', 'giftCard', 'storeCredit'))
);

create index order_payments_order_id_idx on order_payments (order_id);

