create table billing_addresses (
    address_id integer not null,
    payment_id integer not null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null,
    primary key (address_id, payment_id)
);

alter table only billing_addresses
  add constraint billing_addresses_address_id_fk foreign key (address_id) references addresses(id) on update restrict on delete cascade,
  add constraint billing_addresses_applied_payments_id_fk foreign key (payment_id) references applied_payments(id) on update restrict on delete cascade;

