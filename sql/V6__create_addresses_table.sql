create table addresses (
    id serial primary key,
    customer_id integer not null, -- TODO: how do we handle guest addresses?
    state_id integer not null, -- TODO: nullable for foreign addresses?
    name character varying(255) not null, -- TODO: probably need > 255 chars?
    street1 character varying(255) not null, -- TODO: can we have no street at all?
    street2 character varying(255) null,
    city character varying(255) not null, -- TODO: nullable for foreign addresses?
    zip character (5) not null, -- TODO: nullable for foreign addresses?
    is_default_shipping boolean default false not null,
    phone_number character varying(12),
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null,
    constraint valid_zip check (zip ~ '[0-9]{5}'),
    foreign key (state_id) references states(id) on update restrict on delete restrict,
    foreign key (customer_id) references customers(id) on update restrict on delete restrict
);

create index addresses_customer_id_idx on addresses (customer_id);

create unique index address_shipping_default_idx on addresses (customer_id, is_default_shipping)
    where is_default_shipping = true;

