create table shipments (
    id bigint primary key,
    order_id bigint not null,
    shippingMethodId integer,
    shippingAddressId integer,
    status character varying(255) not null,
    shipping_price integer,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null,
    foreign key (id) references inventory_events(id) on update restrict on delete restrict,
    foreign key (order_id) references orders(id) on update restrict on delete restrict,
    foreign key (shippingMethodId) references shipping_methods(id) on update restrict on delete restrict,
    foreign key (shippingAddressId) references addresses(id) on update restrict on delete restrict
);

create trigger set_inventory_id_trigger
    before insert
    on shipments
    for each row
    execute procedure set_inventory_event_id();

