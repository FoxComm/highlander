create table order_shipping_addresses (
    id serial primary key,
    order_id integer not null,
    region_id integer not null references regions(id) on update restrict on delete restrict,
    name generic_string not null,
    address1 generic_string not null,
    address2 generic_string null,
    city generic_string not null,
    zip zip_code not null,
    phone_number phone_number null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (order_id) references orders(id) on update restrict on delete restrict
);

create unique index order_shipping_addresses_order_id_idx on order_shipping_addresses (order_id);
