create table order_shipping_addresses (
    id serial primary key,
    order_id integer not null,
    region_id integer not null references regions(id) on update restrict on delete restrict,
    name character varying(255) not null,
    street1 character varying(255) not null,
    street2 character varying(255) null,
    city character varying(255) not null,
    zip character varying(12) not null,
    phone_number character varying(12) null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null,
    constraint valid_zip check (zip ~ '(?i)^[a-z0-9][a-z0-9\- ]{0,10}[a-z0-9]$'),
    foreign key (order_id) references orders(id) on update restrict on delete restrict
);

create unique index order_shipping_addresses_order_id_idx on order_shipping_addresses (order_id);
