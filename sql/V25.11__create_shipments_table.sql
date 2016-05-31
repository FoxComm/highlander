create table shipments (
    id bigint primary key,
    order_id bigint not null,
    order_shipping_method_id integer,
    shipping_address_id integer,
    state generic_string not null,
    shipping_price integer,
    created_at generic_timestamp,
    updated_at generic_timestamp,
    deleted_at timestamp without time zone null,
    constraint valid_state check (state in ('cart','ordered','fraudHold','remorseHold','manualHold','canceled',
                                              'fulfillmentStarted','partiallyShipped','shipped')),
    foreign key (id) references inventory_events(id) on update restrict on delete restrict,
    foreign key (order_id) references orders(id) on update restrict on delete restrict,
    foreign key (order_shipping_method_id) references order_shipping_methods(id) on update restrict on delete restrict,
    foreign key (shipping_address_id) references order_shipping_addresses(id) on update restrict on delete restrict
);

create trigger set_inventory_id_trigger
    before insert
    on shipments
    for each row
    execute procedure set_inventory_event_id();

