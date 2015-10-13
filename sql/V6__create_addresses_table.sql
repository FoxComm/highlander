create table addresses (
    id serial primary key,
    customer_id integer not null,
    region_id integer not null references regions(id) on update restrict on delete restrict,
    name generic_string not null,
    address1 generic_string not null,
    address2 generic_string null,
    city generic_string not null,
    zip zip_code not null,
    is_default_shipping boolean default false not null,
    phone_number phone_number null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null,
    foreign key (customer_id) references customers(id) on update restrict on delete restrict
);

create index addresses_customer_id_idx on addresses (customer_id);

create unique index address_shipping_default_idx on addresses (customer_id, is_default_shipping)
    where is_default_shipping = true;

