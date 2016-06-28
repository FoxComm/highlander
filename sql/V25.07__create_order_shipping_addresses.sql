create table order_shipping_addresses (
    id serial primary key,
    order_ref text not null references orders(reference_number) on update restrict on delete restrict,
    region_id integer not null references regions(id) on update restrict on delete restrict,
    name generic_string not null,
    address1 generic_string not null,
    address2 generic_string null,
    city generic_string not null,
    zip zip_code not null,
    phone_number phone_number null,
    created_at generic_timestamp,
    updated_at generic_timestamp
);

create unique index order_shipping_addresses_order_ref_idx on order_shipping_addresses (order_ref);
