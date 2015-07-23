create table shipping_addresses (
    id integer primary key,
    customer_id integer not null,
    is_default boolean default false not null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null,
    foreign key (id) references addresses(id) on update restrict on delete restrict,
    foreign key (customer_id) references customers(id) on update restrict on delete restrict
);

create unique index shipping_addresses_default_idx on shipping_addresses (customer_id, is_default)
    where is_default = true;

