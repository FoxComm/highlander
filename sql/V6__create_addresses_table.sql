create table addresses (
    id serial primary key,
    customer_id integer not null,
    region_id integer not null references regions(id) on update restrict on delete restrict,
    name character varying(255) not null,
    street1 character varying(255) not null,
    street2 character varying(255) null,
    city character varying(255) not null,
    zip character varying(12) not null,
    is_default_shipping boolean default false not null,
    phone_number character varying(15) null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null,
    constraint valid_zip check (zip ~ '(?i)^[a-z0-9][a-z0-9\- ]{0,10}[a-z0-9]$'),
    foreign key (customer_id) references customers(id) on update restrict on delete restrict
);

create index addresses_customer_id_idx on addresses (customer_id);

create unique index address_shipping_default_idx on addresses (customer_id, is_default_shipping)
    where is_default_shipping = true;

