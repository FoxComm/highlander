create table order_billing_addresses (
    id serial primary key,
    order_payment_id integer not null,
    region_id integer not null references regions(id) on update restrict on delete restrict,
    name character varying(255) not null,
    street1 character varying(255) not null,
    street2 character varying(255) null,
    city character varying(255) not null,
    zip character varying(12) not null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null,
    constraint valid_zip check (zip ~ '(?i)^[a-z0-9][a-z0-9\- ]{0,10}[a-z0-9]$'),
    foreign key (order_payment_id) references order_payments(id) on update restrict on delete restrict
);

create unique index order_billing_addresses_order_payment_id_idx on order_billing_addresses (order_payment_id);
