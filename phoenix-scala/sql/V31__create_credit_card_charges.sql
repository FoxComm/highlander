create table credit_card_charges (
    id serial primary key,
    credit_card_id integer not null,
    order_payment_id integer not null,
    charge_id generic_string not null,
    state generic_string not null,
    amount integer not null,
    currency currency,
    created_at generic_timestamp,
    foreign key (credit_card_id) references credit_cards(id) on update restrict on delete restrict,
    foreign key (order_payment_id) references order_payments(id) on update restrict on delete restrict,
    constraint valid_state check (state in ('cart','auth','failedAuth','expiredAuth','canceledAuth','failedCapture',
        'fullCapture'))
);

create index credit_card_charges_order_idx on credit_card_charges (credit_card_id, order_payment_id);
