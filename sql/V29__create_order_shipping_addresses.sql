create table order_shipping_addresses (
    id serial primary key,
    order_id integer not null,
    state_id integer not null, -- TODO: nullable for foreign addresses?
    name character varying(255) not null, -- TODO: probably need > 255 chars?
    street1 character varying(255) not null, -- TODO: can we have no street at all?
    street2 character varying(255) null,
    city character varying(255) not null, -- TODO: nullable for foreign addresses?
    zip character (5) not null, -- TODO: nullable for foreign addresses?
    phone_number character varying(12),
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null,
    constraint valid_zip check (zip ~ '[0-9]{5}'),
    foreign key (state_id) references states(id) on update restrict on delete restrict,
    foreign key (order_id) references orders(id) on update restrict on delete restrict
);

create unique index order_shipping_addresses_order_id_idx on order_shipping_addresses (order_id);
