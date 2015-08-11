create table order_payments (
    id serial primary key,
    order_id integer not null,
    payment_method_id integer not null,
    payment_method_type character varying(255) not null,
    amount integer not null default 0,
    status character varying(255) not null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (order_id) references orders(id) on update restrict on delete restrict,
    foreign key (payment_method_id) references payment_methods(id) on update restrict on delete restrict,
    constraint positive_amount check (amount >= 0),
    constraint valid_payment_type check (payment_method_type in ('creditCard', 'giftCard', 'storeCredit'))
);

create index order_payments_order_id_idx on order_payments (order_id);

-- we allow only one CC payment per order
create unique index order_has_only_one_credit_card_idx on order_payments (order_id, payment_method_type)
    where payment_method_type = 'creditCard';

