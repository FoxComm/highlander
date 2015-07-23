create table billing_addresses (
    address_id integer not null,
    payment_id integer not null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null,
    foreign key (address_id) references addresses(id) on update restrict on delete restrict,
    foreign key (payment_id) references order_payments(id) on update restrict on delete restrict,
    primary key (address_id, payment_id)
);

