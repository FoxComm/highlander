create table shipping_addresses (
    id serial primary key,
    address_id integer not null,
    is_default boolean default false not null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null,
    foreign key (address_id) references addresses(id) on update restrict on delete restrict
);

